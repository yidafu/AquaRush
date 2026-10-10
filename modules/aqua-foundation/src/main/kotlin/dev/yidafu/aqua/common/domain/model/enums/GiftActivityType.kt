package dev.yidafu.aqua.common.domain.model.enums

/**
 * 买赠/满赠活动类型(PRD 10-marketing.md §R-MKT-09):
 * - FULL_GIFT: 满赠(订单现金部分商品小计满 X 元赠 Y 桶)
 * - BUY_GIFT: 买赠(指定商品满 X 桶送 Y 桶)
 */
enum class GiftActivityType {
  FULL_GIFT,
  BUY_GIFT,
  ;

  companion object {
    fun fromValue(value: String): GiftActivityType =
      when (value) {
        "FULL_GIFT" -> FULL_GIFT
        "BUY_GIFT" -> BUY_GIFT
        else -> throw IllegalArgumentException("Unknown GiftActivityType value: '" + value + "'")
      }
  }
}
