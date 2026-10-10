package dev.yidafu.aqua.common.domain.model.enums

enum class OrderModelStatus {
  PENDING_PAYMENT, // 待支付
  PENDING_DISPATCH, // 待分配
  PENDING_DELIVERY, // 待配送
  DELIVERING, // 配送中
  AWAITING_CONFIRMATION, // 待确认收货(已送达,等待用户确认签收;送达 N 天未确认自动签收 —— R-ORD-11)
  COMPLETED, // 已完成(用户确认收货或系统自动确认后,计入营收)
  CANCELLED, // 已取消
  REFUNDED, // 已退款
  ;

  companion object {
    fun fromValue(value: String): OrderModelStatus =
      when (value) {
        "PENDING_PAYMENT" -> PENDING_PAYMENT
        "PENDING_DISPATCH" -> PENDING_DISPATCH
        "PENDING_DELIVERY" -> PENDING_DELIVERY
        "DELIVERING" -> DELIVERING
        "AWAITING_CONFIRMATION" -> AWAITING_CONFIRMATION
        "COMPLETED" -> COMPLETED
        "CANCELLED" -> CANCELLED
        "REFUNDED" -> REFUNDED
        else -> throw IllegalArgumentException("Unknown OrderModelStatus value: '" + value + "'")
      }
  }
}
