/**
 * 营销域展示标签 —— 优惠券/水票/反馈(weapp + admin-client 共用)
 *
 * 收敛来源:
 * - user-weapp src/utils/marketing.ts(优惠券/水票标签与有效期文案)
 * - user-weapp src/utils/feedback.ts(反馈类型/状态标签;颜色走 status-tone 的 FEEDBACK_STATUS_TONE)
 * - admin-client 各处内联 COUPON_TYPE_LABELS ×3 / FEEDBACK_TYPE_LABELS / FEEDBACK_STATUS_OPTIONS
 *
 * 金额均为分(R-MON-01),展示格式化统一走 common money。
 */
import {
  CouponType,
  FeedbackStatus,
  FeedbackType,
  UserCouponStatus,
  UserTicketStatus,
  ValidityType,
} from '../types/graphql'
import { formatDate } from './date'
import { displayCents, normalizeCents } from './money'

/** 优惠券类型 → 标签(admin 选项/列渲染与 weapp 券卡共用) */
export const COUPON_TYPE_LABELS: Record<CouponType, string> = {
  [CouponType.CASH]: '现金券',
  [CouponType.DISCOUNT]: '折扣券',
  [CouponType.NO_THRESHOLD_CASH]: '无门槛现金券',
}

export const getCouponTypeLabel = (couponType: CouponType): string =>
  COUPON_TYPE_LABELS[couponType] ?? '优惠券'

/** 优惠券使用门槛文案(满¥x可用 / 无门槛) */
export const getCouponThresholdText = (
  thresholdCents: number | string | null | undefined
): string => {
  const threshold = normalizeCents(thresholdCents)
  return threshold ? `满${displayCents(threshold)}可用` : '无门槛'
}

/** 优惠券优惠内容文案(立减¥x / x.x折;折扣率万分比 ÷1000 转折数,8500‰ = 8.5折) */
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
      return permille != null ? `${(permille / 1000).toFixed(1)}折` : '折扣券'
    }
    default:
      return '优惠券'
  }
}

/** 用户券状态 → 标签 */
export const COUPON_STATUS_LABELS: Record<UserCouponStatus, string> = {
  [UserCouponStatus.RECEIVED]: '未使用',
  [UserCouponStatus.USED]: '已使用',
  [UserCouponStatus.RETURNED]: '已退回',
  [UserCouponStatus.EXPIRED]: '已过期',
}

export const getCouponStatusLabel = (status: UserCouponStatus): string =>
  COUPON_STATUS_LABELS[status] ?? ''

/** 用户水票状态 → 标签 */
export const TICKET_STATUS_LABELS: Record<UserTicketStatus, string> = {
  [UserTicketStatus.UNUSED]: '未用',
  [UserTicketStatus.USED]: '已核销',
  [UserTicketStatus.RETURNED]: '已退回',
  [UserTicketStatus.EXPIRED]: '已过期',
}

export const getTicketStatusLabel = (status: UserTicketStatus): string =>
  TICKET_STATUS_LABELS[status] ?? ''

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

/** 反馈类型 → 标签 */
export const FEEDBACK_TYPE_LABELS: Record<FeedbackType, string> = {
  [FeedbackType.PRODUCT_QUALITY]: '产品质量问题',
  [FeedbackType.DELIVERY_SERVICE]: '配送服务问题',
  [FeedbackType.APP_USAGE]: '小程序使用问题',
  [FeedbackType.SUGGESTION]: '功能建议',
  [FeedbackType.COMPLAINT]: '投诉建议',
  [FeedbackType.OTHER]: '其他问题',
}

export const getFeedbackTypeLabel = (type: FeedbackType): string =>
  FEEDBACK_TYPE_LABELS[type] ?? '其他问题'

/** 反馈状态 → 标签 */
export const FEEDBACK_STATUS_LABELS: Record<FeedbackStatus, string> = {
  [FeedbackStatus.PENDING]: '待处理',
  [FeedbackStatus.PROCESSING]: '处理中',
  [FeedbackStatus.RESOLVED]: '已处理',
}

export const getFeedbackStatusLabel = (status: FeedbackStatus): string =>
  FEEDBACK_STATUS_LABELS[status] ?? ''

/** 反馈状态下拉选项(admin 表单/筛选用) */
export const FEEDBACK_STATUS_OPTIONS: { value: FeedbackStatus; label: string }[] = (
  Object.keys(FEEDBACK_STATUS_LABELS) as FeedbackStatus[]
).map((value) => ({ value, label: FEEDBACK_STATUS_LABELS[value] }))
