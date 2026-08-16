/**
 * 营销域展示工具单测(用户端,PRD 10-marketing.md)
 * 覆盖优惠券/水票的状态、门槛、优惠内容与有效期文案。
 */
import { describe, expect, it } from '@jest/globals'
import { CouponType, UserCouponStatus, UserTicketStatus, ValidityType } from '@aquarush/common'
import {
  getCouponDiscountText,
  getCouponStatusLabel,
  getCouponThresholdText,
  getCouponTypeLabel,
  getTicketStatusLabel,
  getValidityText,
} from './marketing'

describe('getCouponTypeLabel', () => {
  it('各类型中文标签', () => {
    expect(getCouponTypeLabel(CouponType.CASH)).toBe('现金券')
    expect(getCouponTypeLabel(CouponType.DISCOUNT)).toBe('折扣券')
    expect(getCouponTypeLabel(CouponType.NO_THRESHOLD_CASH)).toBe('无门槛现金券')
  })

  it('未知类型回退为 优惠券', () => {
    // @ts-expect-error 非法枚举值
    expect(getCouponTypeLabel('UNKNOWN')).toBe('优惠券')
  })
})

describe('getCouponThresholdText', () => {
  it('有门槛时显示 满¥x可用', () => {
    expect(getCouponThresholdText(5000)).toBe('满¥50.00可用')
    expect(getCouponThresholdText('3000')).toBe('满¥30.00可用')
  })

  it('无门槛(空值/0)显示 无门槛', () => {
    expect(getCouponThresholdText(null)).toBe('无门槛')
    expect(getCouponThresholdText(undefined)).toBe('无门槛')
    expect(getCouponThresholdText(0)).toBe('无门槛')
  })
})

describe('getCouponDiscountText', () => {
  it('现金券显示立减金额', () => {
    expect(
      getCouponDiscountText({ couponType: CouponType.CASH, discountAmountCents: 1000 })
    ).toBe('立减¥10.00')
    expect(
      getCouponDiscountText({ couponType: CouponType.NO_THRESHOLD_CASH, discountAmountCents: '200' })
    ).toBe('立减¥2.00')
  })

  it('折扣券按千分比换算折数(8500 → 8.5折)', () => {
    expect(
      getCouponDiscountText({ couponType: CouponType.DISCOUNT, discountRatePermille: 8500 })
    ).toBe('8.5折')
    expect(
      getCouponDiscountText({ couponType: CouponType.DISCOUNT, discountRatePermille: '10000' })
    ).toBe('10.0折')
  })

  it('折扣券缺折扣率回退 折扣券', () => {
    expect(getCouponDiscountText({ couponType: CouponType.DISCOUNT })).toBe('折扣券')
  })

  it('未知类型回退 优惠券', () => {
    // @ts-expect-error 非法枚举值
    expect(getCouponDiscountText({ couponType: 'X' })).toBe('优惠券')
  })
})

describe('getCouponStatusLabel / getTicketStatusLabel', () => {
  it('优惠券状态标签', () => {
    expect(getCouponStatusLabel(UserCouponStatus.RECEIVED)).toBe('未使用')
    expect(getCouponStatusLabel(UserCouponStatus.USED)).toBe('已使用')
    expect(getCouponStatusLabel(UserCouponStatus.RETURNED)).toBe('已退回')
    expect(getCouponStatusLabel(UserCouponStatus.EXPIRED)).toBe('已过期')
  })

  it('水票状态标签', () => {
    expect(getTicketStatusLabel(UserTicketStatus.UNUSED)).toBe('未用')
    expect(getTicketStatusLabel(UserTicketStatus.USED)).toBe('已核销')
    expect(getTicketStatusLabel(UserTicketStatus.RETURNED)).toBe('已退回')
    expect(getTicketStatusLabel(UserTicketStatus.EXPIRED)).toBe('已过期')
  })

  it('未知状态回退空串', () => {
    // @ts-expect-error 非法枚举值
    expect(getCouponStatusLabel('NOPE')).toBe('')
    // @ts-expect-error 非法枚举值
    expect(getTicketStatusLabel('NOPE')).toBe('')
  })
})

describe('getValidityText', () => {
  it('FIXED_RANGE 显示日期区间', () => {
    expect(
      getValidityText({
        validityType: ValidityType.FIXED_RANGE,
        validFrom: '2026-01-01T00:00:00',
        validTo: '2026-12-31T00:00:00',
      })
    ).toBe('2026-01-01 ~ 2026-12-31')
  })

  it('FIXED_RANGE 缺日期回退 长期有效', () => {
    expect(getValidityText({ validityType: ValidityType.FIXED_RANGE, validFrom: null, validTo: null })).toBe(
      '长期有效'
    )
  })

  it('按领取/购票后天数显示', () => {
    expect(
      getValidityText({ validityType: ValidityType.DAYS_AFTER_CLAIM, validDays: 30 })
    ).toBe('领取后30天有效')
    expect(
      getValidityText({ validityType: ValidityType.DAYS_AFTER_PURCHASE, validDays: 365 })
    ).toBe('购票后365天有效')
  })

  it('缺 validDays 时按 0 天处理', () => {
    expect(getValidityText({ validityType: ValidityType.DAYS_AFTER_CLAIM })).toBe('领取后0天有效')
  })

  it('未知类型回退 长期有效', () => {
    // @ts-expect-error 非法枚举值
    expect(getValidityText({ validityType: 'X' })).toBe('长期有效')
  })
})
