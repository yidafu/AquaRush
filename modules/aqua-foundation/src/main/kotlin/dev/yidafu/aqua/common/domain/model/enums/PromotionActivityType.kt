package dev.yidafu.aqua.common.domain.model.enums

/**
 * 满减/折扣活动类型(PRD 10-marketing.md §R-MKT-04):
 * - FULL_REDUCE: 满减(满 X 减 Y)
 * - FULL_DISCOUNT: 满折(满 X 打 N 折)
 */
enum class PromotionActivityType {
  FULL_REDUCE,
  FULL_DISCOUNT,
  ;

  companion object {
    fun fromValue(value: String): PromotionActivityType =
      when (value) {
        "FULL_REDUCE" -> FULL_REDUCE
        "FULL_DISCOUNT" -> FULL_DISCOUNT
        else -> throw IllegalArgumentException("Unknown PromotionActivityType value: '" + value + "'")
      }
  }
}
