package dev.yidafu.aqua.common.domain.model.enums

/**
 * 购桶单支付状态(G06:一次性塑料水桶费,独立支付,每用户总限购一次):
 * PENDING → PAID(支付回调入账)/ CANCELLED(未支付作废) → REFUNDED(管理员手动退还)
 */
enum class BarrelPaymentStatus {
  PENDING,
  PAID,
  CANCELLED,
  REFUNDED,
}
