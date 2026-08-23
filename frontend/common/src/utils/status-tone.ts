/**
 * 状态标签(label)tone 体系 —— 三端状态「状态 → 颜色」的唯一来源
 *
 * 权威定义:docs/ui-design-system.md §2.2(10% tint 底 + 状态色文字)。
 * 本模块只承载语义 tone 与 tint 色对,不含任何平台实现:
 * - admin-client 经 `toneToAntDColor`(AntD preset 色名)渲染;
 * - user-weapp 经 `StatusTag` 组件 + `--status-*` CSS 变量渲染(值由同步守卫测试锁定本模块);
 * - delivery-app 经 `StatusTints`(light 值派生自 STATUS_TONE_COLORS,dark 本地适配)渲染。
 */
import {
  FeedbackStatus,
  OrderStatus,
  UserCouponStatus,
  UserTicketStatus,
} from '../types/graphql'

/** 语义 tone(与平台无关;accent 为消费者端强调橙,管理端禁用 —— §2.1 红线) */
export type StatusTone = 'info' | 'success' | 'warning' | 'error' | 'neutral' | 'accent'

/** 各 tone 的「状态色文字 + 10% tint 底」色对(§2.2) */
export const STATUS_TONE_COLORS: Record<StatusTone, { text: string; bg: string }> = {
  info: { text: '#3A9BD9', bg: 'rgba(58, 155, 217, 0.10)' },
  success: { text: '#389E0D', bg: 'rgba(82, 196, 26, 0.10)' },
  warning: { text: '#FA8C16', bg: 'rgba(250, 173, 20, 0.10)' },
  neutral: { text: '#8C8C8C', bg: 'rgba(140, 140, 140, 0.10)' },
  error: { text: '#CF1322', bg: 'rgba(255, 77, 79, 0.10)' },
  accent: { text: '#FF5436', bg: 'rgba(255, 84, 54, 0.10)' },
}

/** 订单状态 → tone(§2.2:待付款 warning / 待派单·待配送 info / 配送中 success / 已完成 neutral / 已取消·已退款 error) */
export const ORDER_STATUS_TONE: Record<OrderStatus, StatusTone> = {
  [OrderStatus.PENDING_PAYMENT]: 'warning',
  [OrderStatus.PENDING_DISPATCH]: 'info',
  [OrderStatus.PENDING_DELIVERY]: 'info',
  [OrderStatus.DELIVERING]: 'success',
  [OrderStatus.COMPLETED]: 'neutral',
  [OrderStatus.CANCELLED]: 'error',
  [OrderStatus.REFUNDED]: 'error',
}

/** 反馈状态 → tone(待处理 warning / 处理中 info / 已处理 success) */
export const FEEDBACK_STATUS_TONE: Record<FeedbackStatus, StatusTone> = {
  [FeedbackStatus.PENDING]: 'warning',
  [FeedbackStatus.PROCESSING]: 'info',
  [FeedbackStatus.RESOLVED]: 'success',
}

/** 用户券状态 → tone(未使用 info / 已使用 success / 已退回 warning / 已过期 neutral) */
export const COUPON_STATUS_TONE: Record<UserCouponStatus, StatusTone> = {
  [UserCouponStatus.RECEIVED]: 'info',
  [UserCouponStatus.USED]: 'success',
  [UserCouponStatus.RETURNED]: 'warning',
  [UserCouponStatus.EXPIRED]: 'neutral',
}

/** 用户水票状态 → tone(与券一致) */
export const TICKET_STATUS_TONE: Record<UserTicketStatus, StatusTone> = {
  [UserTicketStatus.UNUSED]: 'info',
  [UserTicketStatus.USED]: 'success',
  [UserTicketStatus.RETURNED]: 'warning',
  [UserTicketStatus.EXPIRED]: 'neutral',
}

/** 每日收款状态 → tone(delivery-app 现有 DAILY_COLLECTION_STATUS_TONE 收敛;DRAFT 以纯文本渲染,给 neutral) */
export const DAILY_COLLECTION_STATUS_TONE: Record<string, StatusTone> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  CONFIRMED: 'success',
  DISPUTED: 'error',
}

/** 对账记录状态 → tone(delivery-app 现有 RECONCILIATION_STATUS_TONE 收敛) */
export const RECONCILIATION_STATUS_TONE: Record<string, StatusTone> = {
  PENDING: 'warning',
  MATCHED: 'success',
  DISCREPANCY: 'error',
  REVIEWED: 'info',
}

/**
 * 从域映射取 tone(未命中返回 undefined,调用方决定兜底展示)。
 * 支持 enum 成员与原始字符串键(后端 wire 值即枚举名)。
 */
export function getStatusTone(
  map: Record<string, StatusTone>,
  status: string | null | undefined
): StatusTone | undefined {
  if (status == null) return undefined
  return map[status]
}
