/**
 * 营销域展示工具(用户端,PRD 10-marketing.md)
 * 优惠券/水票/有效期等文案格式化,供领券中心、券包、水票商城、水票列表页面复用。
 * 注意:后端 Long 字段传输为字符串,统一经 normalizeCents 归一化后再展示。
 */
import {
  CouponType,
  UserCouponStatus,
  UserTicketStatus,
  ValidityType,
  displayCents,
  formatDate,
  normalizeCents
} from '@aquarush/common'

/**
 * 优惠券类型标签(现金券/折扣券/无门槛现金券)
 */
export const getCouponTypeLabel = (couponType: CouponType): string => {
  switch (couponType) {
    case CouponType.CASH:
      return '现金券'
    case CouponType.DISCOUNT:
      return '折扣券'
    case CouponType.NO_THRESHOLD_CASH:
      return '无门槛现金券'
    default:
      return '优惠券'
  }
}

/**
 * 优惠券使用门槛文案(满¥x可用 / 无门槛)
 */
export const getCouponThresholdText = (
  thresholdCents: number | string | null | undefined
): string => {
  const threshold = normalizeCents(thresholdCents)
  return threshold ? `满${displayCents(threshold)}可用` : '无门槛'
}

/**
 * 优惠券优惠内容文案(立减¥x / x.x折)
 */
export const getCouponDiscountText = (coupon: {
  couponType: CouponType
  discountAmountCents?: number | string | null
  discountRatePermille?: number | string | null
}): string => {
  switch (coupon.couponType) {
    case CouponType.CASH:
    case CouponType.NO_THRESHOLD_CASH:
      return `立减${displayCents(normalizeCents(coupon.discountAmountCents))}`
    case CouponType.DISCOUNT: {
      const permille = normalizeCents(coupon.discountRatePermille)
      // 折扣率单位为万分比(如 8500 = 85折),除以 1000 转为折数
      return permille != null ? `${(permille / 1000).toFixed(1)}折` : '折扣券'
    }
    default:
      return '优惠券'
  }
}

/**
 * 用户券状态标签(未使用/已使用/已退回/已过期)
 */
export const getCouponStatusLabel = (status: UserCouponStatus): string => {
  switch (status) {
    case UserCouponStatus.RECEIVED:
      return '未使用'
    case UserCouponStatus.USED:
      return '已使用'
    case UserCouponStatus.RETURNED:
      return '已退回'
    case UserCouponStatus.EXPIRED:
      return '已过期'
    default:
      return ''
  }
}

/**
 * 水票状态标签(未用/已核销/已退回/已过期)
 */
export const getTicketStatusLabel = (status: UserTicketStatus): string => {
  switch (status) {
    case UserTicketStatus.UNUSED:
      return '未用'
    case UserTicketStatus.USED:
      return '已核销'
    case UserTicketStatus.RETURNED:
      return '已退回'
    case UserTicketStatus.EXPIRED:
      return '已过期'
    default:
      return ''
  }
}

/**
 * 有效期文案:
 * - FIXED_RANGE: 日期区间(2026-01-01 ~ 2026-12-31)
 * - DAYS_AFTER_CLAIM: 领取后N天有效
 * - DAYS_AFTER_PURCHASE: 购票后N天有效
 */
export const getValidityText = (item: {
  validityType: ValidityType
  validFrom?: string | null
  validTo?: string | null
  validDays?: number | null
}): string => {
  switch (item.validityType) {
    case ValidityType.FIXED_RANGE:
      return item.validFrom && item.validTo
        ? `${formatDate(item.validFrom)} ~ ${formatDate(item.validTo)}`
        : '长期有效'
    case ValidityType.DAYS_AFTER_CLAIM:
      return `领取后${item.validDays ?? 0}天有效`
    case ValidityType.DAYS_AFTER_PURCHASE:
      return `购票后${item.validDays ?? 0}天有效`
    default:
      return '长期有效'
  }
}
