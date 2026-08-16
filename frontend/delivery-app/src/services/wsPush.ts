import { Alert } from 'react-native';

import { WS_PUSH_URL } from '@/constants/api';
import { authService } from '@/services/auth';

/**
 * WebSocket 实时推送客户端(送水工端,PRD 13-notification.md F-NTF-003,WebSocket 直连)
 *
 * 登录后连接 aqua-admin `/ws/notifications`,首条消息发送 `{token, receiverType:'WORKER'}` 完成注册;
 * 服务端校验 JWT 并从 token 解析送水工账号 id。断线指数退避重连;
 * 离线消息以消息中心(GraphQL)为权威补齐,按 idempotencyKey 去重(R-NTF-02/03)。
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

/** 推送横幅提示(任务页顶部) */
export function showPushBanner(message: WsPushMessage): void {
  const title = message.title ?? '新消息';
  if (message.scene === 'NEW_ORDER') {
    Alert.alert('新订单待接单', message.summary ?? '有新的待接单订单,请及时处理');
    return;
  }
  if (message.type === 'ORDER') {
    Alert.alert(title, message.summary ?? undefined);
  }
}

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
