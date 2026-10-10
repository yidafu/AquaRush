package dev.yidafu.aqua.common.domain.model.enums

enum class OrderModelStatus {
  PENDING_PAYMENT, // 待支付
  PENDING_DISPATCH, // 待分配
  PENDING_DELIVERY, // 待配送
  DELIVERING, // 配送中
  COMPLETED, // 已完成(送达即完成,completed_at=送达时间,即时计入营收 —— R-ORD-11 2026-10 简化)
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
        "COMPLETED" -> COMPLETED
        "CANCELLED" -> CANCELLED
        "REFUNDED" -> REFUNDED
        else -> throw IllegalArgumentException("Unknown OrderModelStatus value: '" + value + "'")
      }
  }
}
