/**
 * 实时推送去重工具单测(R-NTF-02,PRD 13-notification.md)
 * 同一事件(幂等键)仅渲染一次;每用户保留最近 100 个键;登出清除。
 */
import { beforeEach, describe, expect, it, jest } from '@jest/globals'
import {
  clearNotificationDedup,
  isDuplicateNotification,
} from './notificationDedup'

jest.mock(
  '@tarojs/taro',
  () => {
    const store = new Map<string, any>()
    return {
      __esModule: true,
      default: {
        getStorageSync: jest.fn((key: string) => (store.has(key) ? store.get(key) : '')),
        setStorageSync: jest.fn((key: string, value: any) => void store.set(key, value)),
        removeStorageSync: jest.fn((key: string) => void store.delete(key)),
      },
    }
  }
)

describe('notificationDedup', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('首次到达不重复,再次到达判定重复', () => {
    const key = 'ORD123:ORDER_PAID:1715000000000'
    expect(isDuplicateNotification('user-1', key)).toBe(false)
    expect(isDuplicateNotification('user-1', key)).toBe(true)
  })

  it('不同用户互不影响', () => {
    const key = 'ORD456:ORDER_PAID:1715000000001'
    expect(isDuplicateNotification('user-1', key)).toBe(false)
    expect(isDuplicateNotification('user-2', key)).toBe(false)
  })

  it('同一用户不同事件不重复', () => {
    expect(isDuplicateNotification('user-1', 'A:ORDER_PAID:1')).toBe(false)
    expect(isDuplicateNotification('user-1', 'A:DELIVERY_STARTED:2')).toBe(false)
  })

  it('超过 100 条后淘汰最旧键(重复判定不受影响)', () => {
    for (let i = 0; i < 100; i++) {
      expect(isDuplicateNotification('user-1', `key-${i}`)).toBe(false)
    }
    // 第 101 条写入,最旧的 key-0 应被淘汰
    expect(isDuplicateNotification('user-1', 'key-101')).toBe(false)
    expect(isDuplicateNotification('user-1', 'key-0')).toBe(false)
  })

  it('登出清除该用户去重记录', () => {
    const key = 'ORD789:ORDER_PAID:1715000000002'
    expect(isDuplicateNotification('user-1', key)).toBe(false)
    clearNotificationDedup('user-1')
    expect(isDuplicateNotification('user-1', key)).toBe(false)
  })
})
