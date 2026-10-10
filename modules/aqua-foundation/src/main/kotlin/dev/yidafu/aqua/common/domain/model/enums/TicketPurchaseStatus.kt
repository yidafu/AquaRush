package dev.yidafu.aqua.common.domain.model.enums

/**
 * 购票单支付状态(PRD 10-marketing.md §R-MKT-08,购票即预收款项,不支持退款):
 * PENDING → PAID(支付回调入账)/ FAILED / CANCELLED
 */
enum class TicketPurchaseStatus {
  PENDING,
  PAID,
  FAILED,
  CANCELLED,
  ;

  companion object {
    fun fromValue(value: String): TicketPurchaseStatus =
      when (value) {
        "PENDING" -> PENDING
        "PAID" -> PAID
        "FAILED" -> FAILED
        "CANCELLED" -> CANCELLED
        else -> throw IllegalArgumentException("Unknown TicketPurchaseStatus value: '" + value + "'")
      }
  }
}
