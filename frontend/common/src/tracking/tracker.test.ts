/**
 * UserActionTracker 单测(埋点 SDK 核心,平台无关)
 * 验证:入队/队列上限、PAGE_VIEW 去重与页面配置合并、token 门控、
 * flush 成功/失败重入队、认证上下文注入、dispose 语义。
 */
import { afterEach, describe, expect, it, jest } from '@jest/globals'
import {
  UserActionTracker,
  initTracking,
  getTracker,
  resetTrackingForTests,
  type TrackingAdapter,
} from './tracker'

/** 轮询等待异步断言通过(jest 无内置 waitFor) */
const waitFor = async (assert: () => void, timeout = 2000): Promise<void> => {
  const start = Date.now()
  for (;;) {
    try {
      assert()
      return
    } catch (error) {
      if (Date.now() - start > timeout) throw error
      await new Promise((resolve) => setTimeout(resolve, 5))
    }
  }
}

/** 等待内部自动 flush 完成(跨宏任务) */
const flushSettled = async (): Promise<void> => {
  await new Promise((resolve) => setTimeout(resolve, 0))
  await new Promise((resolve) => setTimeout(resolve, 0))
}

const makeAdapter = (overrides: Partial<TrackingAdapter> = {}): TrackingAdapter => ({
  send: jest.fn(async () => {}),
  getCurrentRoute: () => '/pages/index',
  getAuthContext: async () => ({ userId: 'u1', username: 'alice' }),
  hasToken: async () => true,
  ...overrides,
})

const sentPayloads = (adapter: TrackingAdapter): any[] =>
  (adapter.send as jest.Mock).mock.calls[0][0] as any[]

describe('track / 队列', () => {
  it('有 token 时入队并自动上报', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.track('CLICK', 'add-to-cart', { elementId: 'btn-1' })
    await waitFor(() => expect(adapter.send).toHaveBeenCalledTimes(1))
    tracker.dispose()
  })

  it('无 token 时仅入队不上报', async () => {
    const adapter = makeAdapter({ hasToken: async () => false })
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.track('CLICK', 'x')
    await flushSettled()

    expect(adapter.send).not.toHaveBeenCalled()
    expect(tracker.getQueueSize()).toBe(1)
    tracker.dispose()
  })

  it('超过 maxQueue 丢弃最旧事件', async () => {
    const adapter = makeAdapter({ hasToken: async () => false })
    const tracker = new UserActionTracker(adapter, { maxQueue: 3, flushIntervalMs: 60000 })

    tracker.track('CLICK', 'a')
    tracker.track('CLICK', 'b')
    tracker.track('CLICK', 'c')
    tracker.track('CLICK', 'd')
    await flushSettled()

    expect(tracker.getQueueSize()).toBe(3)
    tracker.dispose()
  })

  it('缺少 target 时跳过并告警', async () => {
    const adapter = makeAdapter()
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {})
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    // @ts-expect-error 故意传空 target 验证守卫
    tracker.track('CLICK', '', {})
    expect(warn).toHaveBeenCalled()
    expect(tracker.getQueueSize()).toBe(0)
    tracker.dispose()
    warn.mockRestore()
  })
})

describe('flush', () => {
  it('flush 时注入认证上下文(userId/username 缺省时补全)', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.track('SEARCH', 'product-search')
    await waitFor(() => expect(adapter.send).toHaveBeenCalledTimes(1))

    const [payload] = sentPayloads(adapter)
    expect(payload.userId).toBe('u1')
    expect(payload.username).toBe('alice')
    expect(payload.actionType).toBe('SEARCH')
    expect(payload.target).toBe('product-search')
    expect(payload.pageUrl).toBe('/pages/index')
    expect(typeof payload.timestamp).toBe('number')
    tracker.dispose()
  })

  it('显式覆盖的 userId/username 优先于认证上下文', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.track('CLICK', 'x', { userId: 'override', username: 'bob' })
    await waitFor(() => expect(adapter.send).toHaveBeenCalledTimes(1))

    const [payload] = sentPayloads(adapter)
    expect(payload.userId).toBe('override')
    expect(payload.username).toBe('bob')
    tracker.dispose()
  })

  it('上报失败时静默重入队,下次 flush 再发', async () => {
    const adapter = makeAdapter({
      send: jest.fn().mockRejectedValueOnce(new Error('network down')).mockResolvedValueOnce(undefined),
    })
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.track('CLICK', 'a')
    await waitFor(() => expect(adapter.send).toHaveBeenCalledTimes(1))
    expect(tracker.getQueueSize()).toBe(1)

    await tracker.flush()
    expect(adapter.send).toHaveBeenCalledTimes(2)
    expect(tracker.getQueueSize()).toBe(0)
    tracker.dispose()
  })

  it('队列为空时 flush 直接返回', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    await tracker.flush()
    expect(adapter.send).not.toHaveBeenCalled()
    tracker.dispose()
  })
})

describe('trackPageView', () => {
  it('同路由在去重窗口内只上报一次', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, {
      flushIntervalMs: 60000,
      pageViewDedupeMs: 5000,
    })

    tracker.trackPageView()
    await flushSettled()
    tracker.trackPageView()
    await flushSettled()

    expect(adapter.send).toHaveBeenCalledTimes(1)
    tracker.dispose()
  })

  it('不同路由分别上报', async () => {
    let route = '/pages/home'
    const adapter = makeAdapter({ getCurrentRoute: () => route })
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.trackPageView()
    await flushSettled()
    route = '/pages/orders'
    tracker.trackPageView()
    await waitFor(() => expect(adapter.send).toHaveBeenCalledTimes(2))
    tracker.dispose()
  })

  it('pageTrackConfig 合并页面标题与附加属性', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, {
      flushIntervalMs: 60000,
      pageTrackConfig: {
        '/pages/index': { title: '首页', props: { scene: 'tab' } },
      },
    })

    tracker.trackPageView()
    await waitFor(() => expect(adapter.send).toHaveBeenCalledTimes(1))

    const [payload] = sentPayloads(adapter)
    expect(payload.actionType).toBe('PAGE_VIEW')
    expect(payload.properties).toMatchObject({ pageTitle: '首页', scene: 'tab' })
    tracker.dispose()
  })

  it('无路由时跳过', async () => {
    const adapter = makeAdapter({ getCurrentRoute: () => '' })
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.trackPageView()
    await flushSettled()

    expect(adapter.send).not.toHaveBeenCalled()
    tracker.dispose()
  })
})

describe('生命周期', () => {
  it('dispose 后不再上报', async () => {
    const adapter = makeAdapter()
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.dispose()
    tracker.track('CLICK', 'x')
    await flushSettled()

    expect(tracker.getQueueSize()).toBe(0)
    expect(adapter.send).not.toHaveBeenCalled()
  })

  it('adapter 异常(hasToken 抛错)时 flush 静默返回', async () => {
    const adapter = makeAdapter({
      hasToken: () => {
        throw new Error('boom')
      },
    })
    const tracker = new UserActionTracker(adapter, { flushIntervalMs: 60000 })

    tracker.track('CLICK', 'x')
    await flushSettled()

    expect(adapter.send).not.toHaveBeenCalled()
    expect(tracker.getQueueSize()).toBe(1)
    tracker.dispose()
  })
})

describe('initTracking / getTracker', () => {
  afterEach(() => {
    resetTrackingForTests()
  })

  it('initTracking 创建单例,重复调用返回同一实例', () => {
    const a = initTracking(makeAdapter(), { flushIntervalMs: 60000 })
    const b = initTracking(makeAdapter(), { flushIntervalMs: 60000 })
    expect(a).toBe(b)
    expect(getTracker()).toBe(a)
  })

  it('未初始化时 getTracker 抛出', () => {
    expect(() => getTracker()).toThrow('initTracking() must be called')
  })
})
