import * as Notifications from 'expo-notifications';
import { router } from 'expo-router';
import { Platform } from 'react-native';

import { WS_PUSH_URL } from '@/constants/api';
import { authService } from '@/services/auth';

/**
 * WebSocket 实时推送客户端(送水工端,PRD 13-notification.md F-NTF-003,WebSocket 直连)
 *
 * 登录后连接 aqua-admin `/ws/notifications`,首条消息发送 `{token, receiverType:'WORKER'}` 完成注册;
 * 服务端校验 JWT 并从 token 解析送水工账号 id。断线指数退避重连;
 * 离线消息以消息中心(GraphQL)为权威补齐,按 idempotencyKey 去重(R-NTF-02/03)。
 *
 * 推送呈现:登录后由 AuthProvider 注册全局监听(不依赖「订单」Tab 挂载),收到消息 →
 * expo-notifications 本地通知呈现于系统通知栏(F-NTF-003「系统通知栏提醒」,AC-NTF-003-06),
 * 不再使用应用内 toast;点击通知跳转关联订单详情。web 无系统通知栏,静默跳过
 * (消息仍驱动任务列表/消息中心刷新)。
 */

export interface WsPushMessage {
  id?: number;
  idempotencyKey: string;
  type?: 'ORDER' | 'SYSTEM' | 'ANNOUNCEMENT' | 'FEEDBACK';
  scene?: string;
  title?: string;
  summary?: string | null;
  relatedNo?: string | null;
  createdAt?: number;
  unreadCount?: number;
}

type MessageListener = (message: WsPushMessage) => void;

const MAX_RECONNECT_DELAY_MS = 30_000;

/** 通知能力是否已初始化(仅原生端需要;web 无通知栏) */
let pushInitStarted = false;

// 前台呈现策略:横幅 + 通知栏列表 + 声音(SDK 57 handler 字段,见
// https://docs.expo.dev/versions/v57.0.0/sdk/notifications/);仅原生端生效。
if (Platform.OS !== 'web') {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
    }),
  });
}

/**
 * 通知能力初始化:创建 Android 高优先级通道 + 请求通知权限(仅首次调用生效)。
 * 登录后由 AuthProvider 调用;权限被拒时静默降级(消息仍驱动列表刷新)。
 */
export async function ensurePushNotificationsReady(): Promise<void> {
  if (Platform.OS === 'web' || pushInitStarted) {
    return;
  }
  pushInitStarted = true;
  try {
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: '任务推送',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
      });
    }
    const current = await Notifications.getPermissionsAsync();
    if (current.status !== 'granted') {
      await Notifications.requestPermissionsAsync();
    }
  } catch (e) {
    console.error('通知能力初始化失败:', e);
  }
}

export interface PushNotificationContent {
  title: string;
  body?: string;
  data: Record<string, unknown>;
}

/**
 * WS 消息 → 系统通知内容(门控与旧 toast 横幅一致:仅「新订单」场景与 ORDER 类型弹通知)。
 * 返回 null 表示不呈现系统通知(消息仍会驱动任务列表/消息中心刷新)。
 */
export function buildPushNotificationContent(message: WsPushMessage): PushNotificationContent | null {
  const data: Record<string, unknown> = {
    orderNo: message.relatedNo ?? null,
    scene: message.scene ?? null,
    type: message.type ?? null,
    idempotencyKey: message.idempotencyKey,
  };
  if (message.scene === 'NEW_ORDER') {
    return {
      title: '新订单待接单',
      body: message.summary ?? '有新的待接单订单,请及时处理',
      data,
    };
  }
  if (message.type === 'ORDER') {
    return {
      title: message.title ?? '新消息',
      body: message.summary ?? undefined,
      data,
    };
  }
  return null;
}

/** 推送系统通知(通知栏替代旧 toast 横幅);web 无通知栏,静默跳过 */
export async function showPushNotification(message: WsPushMessage): Promise<void> {
  if (Platform.OS === 'web') {
    return;
  }
  const content = buildPushNotificationContent(message);
  if (!content) {
    return;
  }
  try {
    await Notifications.scheduleNotificationAsync({ content, trigger: null });
  } catch (e) {
    console.error('推送系统通知失败:', e);
  }
}

/**
 * 注册通知点击响应(AC-NTF-003-06:点击进入任务详情)。
 * 登录后由 AuthProvider 注册,登出时注销;web 无通知栏,返回空注销函数。
 */
export function registerNotificationResponseHandler(): () => void {
  if (Platform.OS === 'web') {
    return () => {};
  }
  const subscription = Notifications.addNotificationResponseReceivedListener((response) => {
    const orderNo = response.notification.request.content.data?.orderNo;
    if (typeof orderNo === 'string' && orderNo) {
      router.push({ pathname: '/order/[orderNo]', params: { orderNo } });
    }
  });
  return () => subscription.remove();
}

class WsPushService {
  private ws: WebSocket | null = null;
  private workerId: string | null = null;
  private listeners = new Set<MessageListener>();
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempts = 0;

  /** 登录后建立连接(幂等:同 worker 已连接则复用) */
  async connect(workerId: string, token: string): Promise<void> {
    if (this.workerId === workerId && this.ws?.readyState === WebSocket.OPEN) {
      return;
    }
    await this.disconnect();
    this.workerId = workerId;
    this.open(workerId, token);
  }

  private open(workerId: string, token: string): void {
    try {
      const ws = new WebSocket(WS_PUSH_URL);
      this.ws = ws;

      ws.onopen = () => {
        this.reconnectAttempts = 0;
        // 首条消息注册接收人身份,服务端从 token 解析送水工账号 id(防伪造)
        ws.send(JSON.stringify({ token, receiverType: 'WORKER' }));
      };

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(String(event.data)) as WsPushMessage;
          if (message?.idempotencyKey) {
            this.listeners.forEach((listener) => listener(message));
          }
        } catch (e) {
          console.error('WS message parse failed:', e);
        }
      };

      ws.onclose = () => {
        if (this.ws === ws) {
          this.scheduleReconnect(workerId, token);
        }
      };

      ws.onerror = (e) => {
        console.error('WS error:', e);
      };
    } catch (e) {
      console.error('WS connect failed:', e);
      this.scheduleReconnect(workerId, token);
    }
  }

  private scheduleReconnect(workerId: string, token: string): void {
    if (this.reconnectTimer) {
      return;
    }
    const delay = Math.min(MAX_RECONNECT_DELAY_MS, 3000 * 2 ** this.reconnectAttempts);
    this.reconnectAttempts++;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.workerId === workerId) {
        this.open(workerId, token);
      }
    }, delay);
  }

  onMessage(listener: MessageListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  async disconnect(): Promise<void> {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    const ws = this.ws;
    this.ws = null;
    this.workerId = null;
    this.reconnectAttempts = 0;
    if (ws && ws.readyState === WebSocket.OPEN) {
      try {
        ws.close(1000);
      } catch (e) {
        console.error('WS disconnect failed:', e);
      }
    }
  }
}

export const wsPushService = new WsPushService();

/** 供任务页兜底连接:根据当前登录用户建立连接 */
export async function connectWsPushIfLoggedIn(): Promise<void> {
  try {
    const session = await authService.getStoredSession();
    if (session.token && session.userInfo?.id != null) {
      await wsPushService.connect(String(session.userInfo.id), session.token);
    }
  } catch (e) {
    console.error('WS auto-connect failed:', e);
  }
}
