package dev.yidafu.aqua.common.domain.model.enums

/**
 * 优惠券类型(PRD 10-marketing.md §R-MKT-03):
 * - CASH: 现金券(满 X 减 Y)
 * - DISCOUNT: 折扣券(满 X 打 N 折)
 * - NO_THRESHOLD_CASH: 无门槛现金券
 */
enum class CouponType {
  CASH,
  DISCOUNT,
  NO_THRESHOLD_CASH,
  ;

  companion object {
    fun fromValue(value: String): CouponType =
      when (value) {
        "CASH" -> CASH
        "DISCOUNT" -> DISCOUNT
        "NO_THRESHOLD_CASH" -> NO_THRESHOLD_CASH
        else -> throw IllegalArgumentException("Unknown CouponType value: '" + value + "'")
      }
  }
}
