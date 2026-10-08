package dev.yidafu.aqua.common.domain.model.enums

/**
 * 用户水票实例状态机(PRD 10-marketing.md §R-MKT-08):
 * UNUSED → USED(核销)→ RETURNED(订单取消/退款退回,可复用)→ USED ...
 * 过期: EXPIRED(不可核销,仅展示历史)
 */
enum class UserTicketStatus {
  UNUSED,
  USED,
  RETURNED,
  EXPIRED,
}
