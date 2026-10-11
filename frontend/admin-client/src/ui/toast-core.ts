/**
 * Toast 队列核心 —— 纯状态逻辑(无 JSX / DOM),定时器可注入以便 node 单测。
 * 契约: docs/compose/spec/admin-radix-ui.md [S2.6] message → 自研 toast。
 * DOM 层(toast.tsx)经 subscribe 消费本队列;两端均不依赖 React 组件树。
 */
import type { ReactNode } from 'react'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

/** 各类型默认驻留时长(毫秒):对齐现网 antd 6 —— 全类型 3s(DEFAULT_DURATION=3) */
export const TOAST_DEFAULT_DURATION: Readonly<Record<ToastType, number>> = {
  success: 3000,
  info: 3000,
  warning: 3000,
  error: 3000,
}

/** 同屏最多堆叠条数,超出丢最旧(antd message 常见形态)。 */
export const DEFAULT_MAX_VISIBLE = 3

export interface ToastItem {
  readonly key: string
  readonly type: ToastType
  readonly content: ReactNode
  /** 驻留毫秒;`<= 0` 表示不自动关闭(常驻到手动 destroy)。 */
  readonly duration: number
}

/** `message.open(config)` 的入参形状。 */
export interface ToastPushConfig {
  type: ToastType
  content: ReactNode
  duration?: number
  /** 指定 key 时覆盖同 key 既有条目(原位更新并重置计时),返回该 key。 */
  key?: string
}

/** 可注入定时器:单测传假时钟,浏览器运行时用全局 setTimeout。 */
export interface ToastClock {
  setTimeout(handler: () => void, timeoutMs: number): unknown
  clearTimeout(handle: unknown): void
}

const systemClock: ToastClock = {
  setTimeout: (handler, timeoutMs) => globalThis.setTimeout(handler, timeoutMs),
  clearTimeout: (handle) => globalThis.clearTimeout(handle as number),
}

export interface ToastStore {
  /** 入队(`open` 语义):返回 key(未提供时生成 `toast-<seq>`)。 */
  push(config: ToastPushConfig): string
  /** 销毁指定 key;无参销毁全部(并取消其计时)。 */
  destroy(key?: string): void
  /** 引用稳定:仅在队列变化时换新数组,供 useSyncExternalStore 消费。 */
  getSnapshot(): readonly ToastItem[]
  subscribe(listener: () => void): () => void
}

export interface ToastStoreOptions {
  clock?: ToastClock
  maxVisible?: number
}

export function createToastStore(options: ToastStoreOptions = {}): ToastStore {
  const clock = options.clock ?? systemClock
  const maxVisible = options.maxVisible ?? DEFAULT_MAX_VISIBLE
  const listeners = new Set<() => void>()
  const timers = new Map<string, unknown>()
  let items: ToastItem[] = []
  let seq = 0

  const notify = (): void => {
    for (const listener of [...listeners]) listener()
  }

  const clearTimer = (key: string): void => {
    if (timers.has(key)) {
      clock.clearTimeout(timers.get(key))
      timers.delete(key)
    }
  }

  /** 出队一条并清其计时;返回是否有变更。 */
  const removeKey = (key: string): boolean => {
    const index = items.findIndex((item) => item.key === key)
    if (index < 0) return false
    clearTimer(key)
    items = [...items.slice(0, index), ...items.slice(index + 1)]
    return true
  }

  const schedule = (item: ToastItem): void => {
    if (item.duration <= 0) return
    timers.set(
      item.key,
      clock.setTimeout(() => {
        if (removeKey(item.key)) notify()
      }, item.duration),
    )
  }

  const push = (config: ToastPushConfig): string => {
    const key = config.key ?? `toast-${++seq}`
    const item: ToastItem = {
      key,
      type: config.type,
      content: config.content,
      duration: config.duration ?? TOAST_DEFAULT_DURATION[config.type],
    }
    const index = items.findIndex((entry) => entry.key === key)
    if (index >= 0) {
      // 同 key 覆盖:原位替换,位置不变,计时重置
      clearTimer(key)
      items = items.map((entry) => (entry.key === key ? item : entry))
    } else {
      items = [...items, item]
      // 超出同屏上限:丢最旧(其计时一并清理)
      while (items.length > maxVisible) {
        removeKey(items[0].key)
      }
    }
    schedule(item)
    notify()
    return key
  }

  const destroy = (key?: string): void => {
    if (key !== undefined) {
      if (removeKey(key)) notify()
      return
    }
    for (const timerKey of [...timers.keys()]) clearTimer(timerKey)
    if (items.length > 0) {
      items = []
      notify()
    }
  }

  return {
    push,
    destroy,
    getSnapshot: () => items,
    subscribe: (listener) => {
      listeners.add(listener)
      return () => {
        listeners.delete(listener)
      }
    },
  }
}

/** 全局单例:toast.tsx 的 `message` 门面使用。 */
export const toastStore: ToastStore = createToastStore()
