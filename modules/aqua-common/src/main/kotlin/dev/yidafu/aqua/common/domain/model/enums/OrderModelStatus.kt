package dev.yidafu.aqua.common.domain.model.enums

enum class OrderModelStatus {
  PENDING_PAYMENT, // 待支付
  PENDING_DISPATCH, // 待分配
  PENDING_DELIVERY, // 待配送
  DELIVERING, // 配送中
  COMPLETED, // 已完成
  CANCELLED, // 已取消
  REFUNDED, // 已退款
}
