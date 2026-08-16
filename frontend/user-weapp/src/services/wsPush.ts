import Taro from '@tarojs/taro'

/**
 * WebSocket 实时推送客户端(PRD 13-notification.md F-NTF-003,WebSocket 直连)
 *
 * 登录后连接后端 `/ws/notifications`,首条消息发送 `{token, receiverType:'USER'}` 完成注册;
 * 服务端校验 JWT 并推送实时消息。断线指数退避重连;离线消息以消息中心(GraphQL)为权威补齐,
 * 推送与轮询按 idempotencyKey 去重(R-NTF-02/03)。生产环境使用 wss:// + 微信合法域名白名单。
 */

export interface WsPushMessage {
  id?: number
  idempotencyKey: string
  type: 'ORDER' | 'SYSTEM' | 'ANNOUNCEMENT' | 'FEEDBACK'
  scene: string
  title: string
  summary?: string | null
  relatedNo?: string | null
  createdAt?: number
  unreadCount?: number
}

export interface WsPushStatus {
  connected: boolean
  userId: string | null
}

type MessageListener = (message: WsPushMessage) => void
type StatusListener = (status: WsPushStatus) => void

const DEFAULT_WS_URL = 'ws://localhost:8080/ws/notifications'
const MAX_RECONNECT_DELAY_MS = 30_000

class WsPushService {
  private task: Taro.SocketTask | null = null
  private currentUserId: string | null = null
  private listeners = new Set<MessageListener>()
  private statusListeners = new Set<StatusListener>()
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private status: WsPushStatus = { connected: false, userId: null }

  /** 登录后建立连接(幂等:同用户已连接则复用) */
  async connect(userId: string): Promise<void> {
    if (this.currentUserId === userId && this.task) {
      return
    }
    await this.disconnect()
    this.currentUserId = userId
    const token = Taro.getStorageSync('auth_token') as string | undefined
    if (!token) {
      this.currentUserId = null
      return
    }
    this.openSocket(userId, token)
  }

  private openSocket(userId: string, token: string): void {
    const url = process.env.TARO_APP_WS_URL || DEFAULT_WS_URL
    const task = Taro.connectSocket({ url })
    this.task = task

    task.onOpen(() => {
      this.reconnectAttempts = 0
      this.setStatus({ connected: true, userId })
      // 首条消息注册接收人身份,服务端从 token 解析 userId(防伪造)
      task.send({ data: JSON.stringify({ token, receiverType: 'USER' }) })
    })

    task.onMessage(res => {
      try {
        const message = JSON.parse(String(res.data)) as WsPushMessage
        if (message?.idempotencyKey) {
          this.listeners.forEach(listener => listener(message))
        }
      } catch (e) {
        console.error('WS message parse failed:', e)
      }
    })

    task.onClose(() => {
      this.setStatus({ connected: false, userId })
      this.scheduleReconnect(userId, token)
    })

    task.onError(err => {
      console.error('WS error:', err?.errMsg)
    })
  }

  private scheduleReconnect(userId: string, token: string): void {
    if (this.reconnectTimer) {
      return
    }
    const delay = Math.min(MAX_RECONNECT_DELAY_MS, 3000 * 2 ** this.reconnectAttempts)
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.currentUserId === userId) {
        this.openSocket(userId, token)
      }
    }, delay)
  }

  onMessage(listener: MessageListener): () => void {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  onStatus(listener: StatusListener): () => void {
    this.statusListeners.add(listener)
    listener(this.status)
    return () => {
      this.statusListeners.delete(listener)
    }
  }

  getStatus(): WsPushStatus {
    return this.status
  }

  async disconnect(): Promise<void> {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    const task = this.task
    this.task = null
    this.currentUserId = null
    this.reconnectAttempts = 0
    if (task) {
      try {
        task.close({ code: 1000 })
      } catch (e) {
        console.error('WS disconnect failed:', e)
      }
    }
    this.setStatus({ connected: false, userId: null })
  }

  private setStatus(status: WsPushStatus): void {
    this.status = status
    this.statusListeners.forEach(listener => listener(status))
  }
}

export const wsPushService = new WsPushService()

/** 供页面/组件快速连接:根据当前登录用户建立 WebSocket 推送连接 */
export async function connectWsPushIfLoggedIn(): Promise<void> {
  try {
    const userInfo = Taro.getStorageSync('userInfo') as { id?: string } | null
    const token = Taro.getStorageSync('auth_token') as string | undefined
    if (token && userInfo?.id) {
      await wsPushService.connect(String(userInfo.id))
    }
  } catch (e) {
    console.error('WS auto-connect failed:', e)
  }
}
