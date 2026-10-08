/**
 * marketing 标签单测:优惠券/水票/反馈标签、门槛、折扣(含 8500‰ → 8.5折 修复断言)、有效期。
 */
import { describe, expect, it } from '@jest/globals'
import {
  CouponType,
  FeedbackStatus,
  FeedbackType,
  UserCouponStatus,
  UserTicketStatus,
  ValidityType,
} from '../types/graphql'
import {
  COUPON_STATUS_LABELS,
  COUPON_TYPE_LABELS,
  FEEDBACK_STATUS_LABELS,
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_TYPE_LABELS,
  TICKET_STATUS_LABELS,
  getCouponDiscountText,
  getCouponStatusLabel,
  getCouponThresholdText,
  getCouponTypeLabel,
  getFeedbackStatusLabel,
  getFeedbackTypeLabel,
  getTicketStatusLabel,
  getValidityText,
} from './marketing'

describe('优惠券标签', () => {
  it('类型标签', () => {
    expect(COUPON_TYPE_LABELS).toEqual({
      CASH: '现金券',
      DISCOUNT: '折扣券',
      NO_THRESHOLD_CASH: '无门槛现金券',
    })
    expect(getCouponTypeLabel(CouponType.CASH)).toBe('现金券')
    expect(getCouponTypeLabel(CouponType.DISCOUNT)).toBe('折扣券')
  })

  it('门槛文案:有值满x可用,0/空无门槛', () => {
    expect(getCouponThresholdText(5000)).toBe('满¥50.00可用')
    expect(getCouponThresholdText('5000')).toBe('满¥50.00可用')
    expect(getCouponThresholdText(0)).toBe('无门槛')
    expect(getCouponThresholdText(null)).toBe('无门槛')
  })

  it('优惠内容:现金券立减;折扣券万分比 ÷1000(8500‰ → 8.5折,修复 ÷100 的 bug)', () => {
    expect(getCouponDiscountText({ couponType: CouponType.CASH, discountAmountCents: 300 })).toBe('立减¥3.00')
    expect(
      getCouponDiscountText({ couponType: CouponType.NO_THRESHOLD_CASH, discountAmountCents: '300' })
    ).toBe('立减¥3.00')
    expect(getCouponDiscountText({ couponType: CouponType.DISCOUNT, discountRatePermille: 8500 })).toBe('8.5折')
    expect(getCouponDiscountText({ couponType: CouponType.DISCOUNT, discountRatePermille: '8500' })).toBe('8.5折')
    expect(getCouponDiscountText({ couponType: CouponType.DISCOUNT, discountRatePermille: null })).toBe('折扣券')
  })

  it('状态标签与映射', () => {
    expect(COUPON_STATUS_LABELS).toEqual({
      RECEIVED: '未使用',
      USED: '已使用',
      RETURNED: '已退回',
      EXPIRED: '已过期',
    })
    expect(getCouponStatusLabel(UserCouponStatus.RECEIVED)).toBe('未使用')
    expect(getCouponStatusLabel('UNKNOWN' as UserCouponStatus)).toBe('')
  })
})

describe('水票标签', () => {
  it('状态标签与映射', () => {
    expect(TICKET_STATUS_LABELS).toEqual({
      UNUSED: '未用',
      USED: '已核销',
      RETURNED: '已退回',
      EXPIRED: '已过期',
    })
    expect(getTicketStatusLabel(UserTicketStatus.UNUSED)).toBe('未用')
    expect(getTicketStatusLabel(UserTicketStatus.USED)).toBe('已核销')
  })

  it('有效期文案', () => {
    expect(
      getValidityText({ validityType: ValidityType.FIXED_RANGE, validFrom: '2026-01-01T00:00:00', validTo: '2026-12-31T00:00:00' })
    ).toMatch(/2026-01-01 ~ 2026-12-31/)
    expect(getValidityText({ validityType: ValidityType.FIXED_RANGE })).toBe('长期有效')
    expect(getValidityText({ validityType: ValidityType.DAYS_AFTER_CLAIM, validDays: 30 })).toBe('领取后30天有效')
    expect(getValidityText({ validityType: ValidityType.DAYS_AFTER_PURCHASE, validDays: 7 })).toBe('购票后7天有效')
    expect(getValidityText({ validityType: ValidityType.DAYS_AFTER_CLAIM })).toBe('领取后0天有效')
  })
})

describe('反馈标签', () => {
  it('类型标签', () => {
    expect(FEEDBACK_TYPE_LABELS.PRODUCT_QUALITY).toBe('产品质量问题')
    expect(getFeedbackTypeLabel(FeedbackType.PRODUCT_QUALITY)).toBe('产品质量问题')
    expect(getFeedbackTypeLabel('UNKNOWN' as FeedbackType)).toBe('其他问题')
  })

  it('状态标签与选项(admin 消费)', () => {
    expect(FEEDBACK_STATUS_LABELS).toEqual({
      PENDING: '待处理',
      PROCESSING: '处理中',
      RESOLVED: '已处理',
    })
    expect(getFeedbackStatusLabel(FeedbackStatus.PENDING)).toBe('待处理')
    expect(FEEDBACK_STATUS_OPTIONS).toEqual([
      { value: 'PENDING', label: '待处理' },
      { value: 'PROCESSING', label: '处理中' },
      { value: 'RESOLVED', label: '已处理' },
    ])
  })
})
