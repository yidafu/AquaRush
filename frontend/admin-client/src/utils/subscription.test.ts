/**
 * 周期订购计划展示工具单测
 */
import { describe, expect, it } from '@jest/globals'
import {
  formatCycleRule,
  formatItemsSummary,
  getSubscriptionStatusColor,
  getSubscriptionStatusText,
} from './subscription'

describe('formatCycleRule', () => {
  it('WEEKLY:每 + 星期升序、斜杠分隔(设计稿口径「每周一 / 周四」)', () => {
    expect(formatCycleRule({ cycleType: 'WEEKLY', weekdays: [4, 1] })).toBe('每周一 / 周四')
  })

  it('BIWEEKLY:每两周 + 括号内星期', () => {
    expect(formatCycleRule({ cycleType: 'BIWEEKLY', weekdays: [1] })).toBe('每两周(周一)')
  })

  it('MONTHLY:每月 N 日', () => {
    expect(formatCycleRule({ cycleType: 'MONTHLY', monthDay: 15 })).toBe('每月 15 日')
  })

  it('MONTHLY 缺 monthDay:兜底「每月」', () => {
    expect(formatCycleRule({ cycleType: 'MONTHLY', monthDay: null })).toBe('每月')
  })

  it('weekdays 为空/含非法值时兜底「每周」', () => {
    expect(formatCycleRule({ cycleType: 'WEEKLY', weekdays: [] })).toBe('每周')
    expect(formatCycleRule({ cycleType: 'WEEKLY', weekdays: [0, 9] })).toBe('每周')
  })
})

describe('getSubscriptionStatusText/Color', () => {
  it('三态文案与 Tag 色对齐设计稿', () => {
    expect(getSubscriptionStatusText('ACTIVE')).toBe('生效中')
    expect(getSubscriptionStatusColor('ACTIVE')).toBe('success')
    expect(getSubscriptionStatusText('PAUSED')).toBe('已暂停')
    expect(getSubscriptionStatusColor('PAUSED')).toBe('warning')
    expect(getSubscriptionStatusText('TERMINATED')).toBe('已终止')
    expect(getSubscriptionStatusColor('TERMINATED')).toBe('default')
  })

  it('未知/null 状态回退', () => {
    expect(getSubscriptionStatusText('UNKNOWN')).toBe('UNKNOWN')
    expect(getSubscriptionStatusText(null)).toBe('-')
    expect(getSubscriptionStatusColor('UNKNOWN')).toBe('default')
    expect(getSubscriptionStatusColor(undefined)).toBe('default')
  })
})

describe('formatItemsSummary', () => {
  it('多项以顿号连接', () => {
    expect(
      formatItemsSummary([
        { productName: '怡宝 18.9L', quantity: 2 },
        { productName: '农夫山泉 19L', quantity: 3 },
      ]),
    ).toBe('怡宝 18.9L × 2、农夫山泉 19L × 3')
  })

  it('空列表兜底 -', () => {
    expect(formatItemsSummary([])).toBe('-')
    expect(formatItemsSummary(null)).toBe('-')
  })
})
