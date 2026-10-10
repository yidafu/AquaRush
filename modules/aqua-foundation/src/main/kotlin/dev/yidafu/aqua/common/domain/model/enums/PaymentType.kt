package dev.yidafu.aqua.common.domain.model.enums

/**
 * 收款方式枚举
 * 用于记录配送员完成配送时的收款方式
 */
enum class PaymentType {
  WATER_TICKET, // 水票
  CASH, // 现金
  QR_CODE, // 扫码
  ;

  companion object {
    fun fromValue(value: String): PaymentType =
      when (value) {
        "WATER_TICKET" -> WATER_TICKET
        "CASH" -> CASH
        "QR_CODE" -> QR_CODE
        else -> throw IllegalArgumentException("Unknown PaymentType value: '" + value + "'")
      }
  }
}
