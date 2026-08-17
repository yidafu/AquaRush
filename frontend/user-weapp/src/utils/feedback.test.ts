/**
 * 意见反馈展示工具单测(PRD 07-user.md §F-USR-005 / 12-operations-config.md §F-OPS-003)
 */
import { describe, expect, it } from '@jest/globals'
import { FeedbackStatus, FeedbackType } from '@aquarush/common'
import {
  getFeedbackStatusColor,
  getFeedbackStatusLabel,
  getFeedbackTypeLabel,
} from './feedback'

describe('getFeedbackTypeLabel', () => {
  it('各类型中文标签', () => {
    expect(getFeedbackTypeLabel(FeedbackType.PRODUCT_QUALITY)).toBe('产品质量问题')
    expect(getFeedbackTypeLabel(FeedbackType.DELIVERY_SERVICE)).toBe('配送服务问题')
    expect(getFeedbackTypeLabel(FeedbackType.APP_USAGE)).toBe('小程序使用问题')
    expect(getFeedbackTypeLabel(FeedbackType.SUGGESTION)).toBe('功能建议')
    expect(getFeedbackTypeLabel(FeedbackType.COMPLAINT)).toBe('投诉建议')
    expect(getFeedbackTypeLabel(FeedbackType.OTHER)).toBe('其他问题')
  })

  it('未知类型回退 其他问题', () => {
    // @ts-expect-error 非法枚举值
    expect(getFeedbackTypeLabel('UNKNOWN')).toBe('其他问题')
  })
})

describe('getFeedbackStatusLabel', () => {
  it('状态中文标签', () => {
    expect(getFeedbackStatusLabel(FeedbackStatus.PENDING)).toBe('待处理')
    expect(getFeedbackStatusLabel(FeedbackStatus.PROCESSING)).toBe('处理中')
    expect(getFeedbackStatusLabel(FeedbackStatus.RESOLVED)).toBe('已处理')
  })

  it('未知状态回退空串', () => {
    // @ts-expect-error 非法枚举值
    expect(getFeedbackStatusLabel('NOPE')).toBe('')
  })
})

describe('getFeedbackStatusColor', () => {
  it('状态色与后台 Tag 一致:待处理橙/处理中蓝/已处理绿(设计 token)', () => {
    expect(getFeedbackStatusColor(FeedbackStatus.PENDING)).toBe('var(--status-pending-pay-text)')
    expect(getFeedbackStatusColor(FeedbackStatus.PROCESSING)).toBe('var(--status-dispatch-text)')
    expect(getFeedbackStatusColor(FeedbackStatus.RESOLVED)).toBe('var(--status-delivering-text)')
  })

  it('未知状态回退弱色', () => {
    // @ts-expect-error 非法枚举值
    expect(getFeedbackStatusColor('NOPE')).toBe('var(--text-weak)')
  })
})
