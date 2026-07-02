package dev.yidafu.aqua.common.domain.model.enums

/**
 * 收款方式枚举
 * 用于记录配送员完成配送时的收款方式
 */
enum class PaymentType {
  WATER_TICKET, // 水票
  CASH, // 现金
  QR_CODE, // 扫码
}
