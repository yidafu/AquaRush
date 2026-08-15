import type { AuthContext, TrackPayload, TrackingAdapter, TrackingOptions } from './types'

const DEFAULT_MAX_QUEUE = 20
const DEFAULT_FLUSH_INTERVAL = 5000
const DEFAULT_PAGE_VIEW_DEDUPE_MS = 500

/**
 * 用户操作追踪器(SDK 核心,平台无关)
 *
 * 特性:
 * - 队列缓冲(上限 maxQueue,超出丢最旧)+ 定时 flush + 手动 flush;
 * - 自动附带 userId/username(经 adapter 在 flush 时解析,支持异步存储)与 pageUrl(入队时捕获当前路由);
 * - 无 token 时仅入队不发送(登录成功/切后台后再 flush),网络失败静默重入队,绝不抛出、绝不阻塞 UI。
 *
 * 用法:各端用 `initTracking(adapter, options)` 创建全局单例,或直接 new(测试用)。
 */
export class UserActionTracker {
  private queue: TrackPayload[] = []
  private timer: ReturnType<typeof setInterval> | null = null
  private lastPageViewRoute = ''
  private lastPageViewAt = 0
  private flushing = false
  private disposed = false

  private readonly maxQueue: number
  private readonly pageViewDedupeMs: number
  private readonly pageTrackConfig: TrackingOptions['pageTrackConfig']

  constructor(
    private readonly adapter: TrackingAdapter,
    options: TrackingOptions = {},
  ) {
    this.maxQueue = options.maxQueue ?? DEFAULT_MAX_QUEUE
    this.pageViewDedupeMs = options.pageViewDedupeMs ?? DEFAULT_PAGE_VIEW_DEDUPE_MS
    this.pageTrackConfig = options.pageTrackConfig

    // 环境可用时启动定时 flush
    if (typeof setInterval === 'function') {
      this.timer = setInterval(() => {
        void this.flush()
      }, options.flushIntervalMs ?? DEFAULT_FLUSH_INTERVAL)
    }
  }

  /** 当前页面路由:入队时捕获,保证与事件发生时页面一致 */
  private getCurrentRoute(): string {
    try {
      return this.adapter.getCurrentRoute() || ''
    } catch {
      return ''
    }
  }

  /** 页面访问埋点:页面展示时调用(各端 hook/布局接入) */
  trackPageView(): void {
    const route = this.getCurrentRoute()
    if (!route) return

    // 同路由快速重复展示去重
    const now = Date.now()
    if (route === this.lastPageViewRoute && now - this.lastPageViewAt < this.pageViewDedupeMs) {
      return
    }
    this.lastPageViewRoute = route
    this.lastPageViewAt = now

    const config = this.pageTrackConfig?.[route]
    this.track('PAGE_VIEW', route, {
      pageUrl: route,
      properties: {
        ...(config?.props || {}),
        ...(config?.title ? { pageTitle: config.title } : {}),
      },
    })
  }

  /**
   * 通用埋点:入队并尝试 flush
   * @param actionType 动作类型(CLICK/FORM_SUBMIT/SEARCH/SHARE/...)
   * @param target 操作对象标识(如 add-to-cart、order-submit)
   * @param payload 附加数据(userId/username 覆盖、elementId/elementType/elementText、properties 等)
   */
  track(
    actionType: string,
    target: string,
    payload: Partial<TrackPayload> & { properties?: Record<string, unknown> } = {},
  ): void {
    if (this.disposed) return
    if (!target) {
      console.warn('[tracking] track target is required, skipped:', actionType)
      return
    }
    const route = this.getCurrentRoute()
    const item: TrackPayload = {
      // userId/username 在 flush 时经 adapter 解析填充;这里仅保留显式覆盖值
      userId: payload.userId,
      username: payload.username,
      actionType,
      target,
      pageUrl: payload.pageUrl ?? (route || undefined),
      elementId: payload.elementId,
      elementType: payload.elementType,
      elementText: payload.elementText,
      coordinates: payload.coordinates,
      properties: payload.properties ?? {},
      timestamp: Date.now(),
    }
    this.queue.push(item)
    if (this.queue.length > this.maxQueue) {
      this.queue.shift()
    }
    void this.flush()
  }

  /** 队列长度(调试用) */
  getQueueSize(): number {
    return this.queue.length
  }

  /** 立即上报队列(无 token 时保留队列等待下次;失败静默重入队) */
  async flush(): Promise<void> {
    if (this.flushing || this.disposed || this.queue.length === 0) {
      return
    }
    // 先占锁再 await,避免并发 flush 同时通过 hasToken 检查
    this.flushing = true
    try {
      let hasToken: boolean
      try {
        hasToken = Boolean(await this.adapter.hasToken())
      } catch {
        return
      }
      if (!hasToken) return

      const actions = this.queue.splice(0, this.maxQueue)
      try {
        const ctx = await this.resolveAuthContext()
        const enriched = actions.map((action) => ({
          ...action,
          userId: action.userId ?? ctx.userId,
          username: action.username ?? ctx.username,
        }))
        await this.adapter.send(enriched)
      } catch (error) {
        // 上报失败静默:网络异常/服务端不可用时不影响业务;重新入队等待下次 flush
        this.queue.unshift(...actions)
        if (this.queue.length > this.maxQueue) {
          this.queue.splice(this.maxQueue)
        }
        console.warn('[tracking] failed to flush user actions:', error)
      }
    } finally {
      this.flushing = false
    }
  }

  private async resolveAuthContext(): Promise<AuthContext> {
    try {
      return await this.adapter.getAuthContext()
    } catch {
      return {}
    }
  }

  /** 释放定时器等资源(测试用;之后 flush 不再执行) */
  dispose(): void {
    this.disposed = true
    if (this.timer !== null) {
      clearInterval(this.timer)
      this.timer = null
    }
  }
}

let currentTracker: UserActionTracker | null = null

/**
 * 创建全局追踪器单例(各端启动时调用一次;重复调用返回已有实例并告警)
 */
export function initTracking(adapter: TrackingAdapter, options: TrackingOptions = {}): UserActionTracker {
  if (currentTracker) {
    console.warn('[tracking] initTracking called twice; returning existing tracker')
    return currentTracker
  }
  currentTracker = new UserActionTracker(adapter, options)
  return currentTracker
}

/** 获取全局追踪器(未初始化时抛出) */
export function getTracker(): UserActionTracker {
  if (!currentTracker) {
    throw new Error('[tracking] initTracking() must be called before using the tracker')
  }
  return currentTracker
}

/** 重置全局单例(测试用) */
export function resetTrackingForTests(): void {
  currentTracker?.dispose()
  currentTracker = null
}
