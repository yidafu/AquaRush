/**
 * toast-core 队列单测(node 环境,纯逻辑、无 DOM)。
 * 覆盖 [S2.6] 验收:入队/出队顺序、到时自动消退、destroy 单条/全部、
 * 超上限淘汰最旧、open()(= push)自定义 type/duration/key。
 */
import { describe, expect, it } from '@jest/globals'
import {
  createToastStore,
  TOAST_DEFAULT_DURATION,
  type ToastClock,
} from './toast-core'

/** 假时钟:手动 advance 推进,毫秒级精确断言自动消退,不依赖真实定时器。 */
class FakeClock implements ToastClock {
  private now = 0
  private nextId = 0
  private readonly tasks = new Map<number, { id: number; at: number; fn: () => void }>()

  setTimeout(handler: () => void, timeoutMs: number): number {
    this.nextId += 1
    this.tasks.set(this.nextId, { id: this.nextId, at: this.now + timeoutMs, fn: handler })
    return this.nextId
  }

  clearTimeout(handle: unknown): void {
    if (typeof handle === 'number') this.tasks.delete(handle)
  }

  /** 未清理的定时器数 —— 用于断言出队/淘汰确实取消了计时。 */
  get pending(): number {
    return this.tasks.size
  }

  advance(ms: number): void {
    const target = this.now + ms
    for (;;) {
      const due = [...this.tasks.values()]
        .filter((task) => task.at <= target)
        .sort((a, b) => a.at - b.at)[0]
      if (!due) break
      this.tasks.delete(due.id)
      this.now = due.at
      due.fn()
    }
    this.now = target
  }
}

function setup(maxVisible?: number): { clock: FakeClock; store: ReturnType<typeof createToastStore> } {
  const clock = new FakeClock()
  const store = createToastStore({ clock, maxVisible })
  return { clock, store }
}

describe('toast-core 队列', () => {
  it('enqueue 按调用顺序入队,自动消退按先到期先出队且余下顺序不变', () => {
    const { clock, store } = setup()
    const key1 = store.push({ type: 'info', content: '一', duration: 1000 })
    const key2 = store.push({ type: 'info', content: '二', duration: 2000 })
    const key3 = store.push({ type: 'info', content: '三', duration: 3000 })

    expect([key1, key2, key3]).toEqual(['toast-1', 'toast-2', 'toast-3'])
    expect(store.getSnapshot().map((item) => item.content)).toEqual(['一', '二', '三'])

    clock.advance(1000) // '一' 到期出队
    expect(store.getSnapshot().map((item) => item.content)).toEqual(['二', '三'])
    clock.advance(1000) // '二' 到期出队
    expect(store.getSnapshot().map((item) => item.content)).toEqual(['三'])
  })

  it('到时自动消退,默认时长对齐 antd 6:全类型 3s', () => {
    const { clock, store } = setup()
    expect(TOAST_DEFAULT_DURATION.success).toBe(3000)
    expect(TOAST_DEFAULT_DURATION.error).toBe(3000)
    expect(TOAST_DEFAULT_DURATION.warning).toBe(3000)

    store.push({ type: 'success', content: 'ok' })
    clock.advance(1000)
    store.push({ type: 'error', content: 'bad' })
    expect(store.getSnapshot()).toHaveLength(2)

    clock.advance(1999) // success 推入后 2999ms,未满 3s
    expect(store.getSnapshot().map((item) => item.type)).toEqual(['success', 'error'])
    clock.advance(1) // success 满 3000ms
    expect(store.getSnapshot().map((item) => item.type)).toEqual(['error'])
    clock.advance(1000) // error 满 3000ms
    expect(store.getSnapshot()).toHaveLength(0)
    expect(clock.pending).toBe(0)
  })

  it('destroy(key) 只出队对应条目并取消其计时,其余不受影响', () => {
    const { clock, store } = setup()
    const key1 = store.push({ type: 'info', content: '一' })
    const key2 = store.push({ type: 'warning', content: '二' })
    const key3 = store.push({ type: 'info', content: '三' })
    expect(clock.pending).toBe(3)

    store.destroy(key2)
    expect(store.getSnapshot().map((item) => item.key)).toEqual([key1, key3])
    expect(clock.pending).toBe(2)

    // 销毁不存在的 key 静默无副作用
    store.destroy('nope')
    expect(store.getSnapshot()).toHaveLength(2)
  })

  it('destroy() 清空全部并取消所有计时', () => {
    const { clock, store } = setup()
    store.push({ type: 'success', content: 'a' })
    store.push({ type: 'error', content: 'b' })
    store.push({ type: 'info', content: 'c' })
    expect(store.getSnapshot()).toHaveLength(3)

    store.destroy()
    expect(store.getSnapshot()).toHaveLength(0)
    expect(clock.pending).toBe(0)
    clock.advance(60_000) // 计时已清,不会「复活」或重复触发
    expect(store.getSnapshot()).toHaveLength(0)
  })

  it('超过 maxVisible(默认 3)时淘汰最旧一条并清理其计时', () => {
    const { clock, store } = setup()
    const key1 = store.push({ type: 'info', content: '1' })
    store.push({ type: 'info', content: '2' })
    store.push({ type: 'info', content: '3' })
    expect(clock.pending).toBe(3)

    store.push({ type: 'info', content: '4' })
    expect(store.getSnapshot().map((item) => item.content)).toEqual(['2', '3', '4'])
    expect(store.getSnapshot().some((item) => item.key === key1)).toBe(false)
    expect(clock.pending).toBe(3) // 最旧条目的计时已随淘汰清理

    // 连续压入也不越界
    store.push({ type: 'warning', content: '5' })
    store.push({ type: 'error', content: '6' })
    expect(store.getSnapshot().map((item) => item.content)).toEqual(['4', '5', '6'])
  })

  it('open()(= push):自定义 type / duration / key', () => {
    const { clock, store } = setup()

    // 自定义 type + duration
    const key = store.push({ type: 'warning', content: '注意', duration: 500 })
    expect(key).toBe('toast-1')
    expect(store.getSnapshot()[0]).toMatchObject({ type: 'warning', duration: 500 })
    clock.advance(499)
    expect(store.getSnapshot()).toHaveLength(1)
    clock.advance(1)
    expect(store.getSnapshot()).toHaveLength(0)

    // 自定义 key + duration 0(不自动关闭)
    const customKey = store.push({ key: 'my-key', type: 'error', content: '常驻', duration: 0 })
    expect(customKey).toBe('my-key')
    clock.advance(60_000)
    expect(store.getSnapshot().map((item) => item.key)).toEqual(['my-key'])

    // 同 key 再次 push:原位覆盖内容、重置计时、不追加新条目
    const again = store.push({ key: 'my-key', type: 'success', content: '已更新' })
    expect(again).toBe('my-key')
    expect(store.getSnapshot()).toHaveLength(1)
    expect(store.getSnapshot()[0]).toMatchObject({ type: 'success', content: '已更新' })
    expect(store.getSnapshot()[0].duration).toBe(TOAST_DEFAULT_DURATION.success)
  })

  it('subscribe 在每次队列变化时收到通知,退订后不再通知', () => {
    const { store } = setup()
    let calls = 0
    const unsubscribe = store.subscribe(() => {
      calls += 1
    })
    const key = store.push({ type: 'info', content: 'x' })
    store.destroy(key)
    expect(calls).toBe(2)
    unsubscribe()
    store.push({ type: 'info', content: 'y' })
    expect(calls).toBe(2)
  })
})
