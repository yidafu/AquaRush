package dev.yidafu.aqua.common.domain.model.enums

/**
 * 用户优惠券实例状态机(PRD 10-marketing.md §R-MKT-03):
 * 未领取(模板)→ RECEIVED → USED → RETURNED(订单取消/退款退回,可复用)→ USED ...
 * 已过期: EXPIRED(不可用)
 */
enum class UserCouponStatus {
  RECEIVED,
  USED,
  RETURNED,
  EXPIRED,
}
