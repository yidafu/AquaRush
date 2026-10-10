package dev.yidafu.aqua.common.domain.model.enums

/**
 * 对账任务类型
 */
enum class ReconciliationTaskType {
  PAYMENT, // 支付对账
  REFUND, // 退款对账
  SETTLEMENT, // 结算对账
  ;

  companion object {
    fun fromValue(value: String): ReconciliationTaskType =
      when (value) {
        "PAYMENT" -> PAYMENT
        "REFUND" -> REFUND
        "SETTLEMENT" -> SETTLEMENT
        else -> throw IllegalArgumentException("Unknown ReconciliationTaskType value: '" + value + "'")
      }
  }
}
