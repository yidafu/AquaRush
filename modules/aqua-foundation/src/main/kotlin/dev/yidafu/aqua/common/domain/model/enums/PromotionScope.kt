package dev.yidafu.aqua.common.domain.model.enums

/**
 * 营销活动适用范围(PRD 10-marketing.md):
 * - ALL: 全场
 * - PRODUCT: 指定商品(配合 productId)
 * - GROUP: 指定客户分组(配合 customerGroupId)
 */
enum class PromotionScope {
  ALL,
  PRODUCT,
  GROUP,
  ;

  companion object {
    fun fromValue(value: String): PromotionScope =
      when (value) {
        "ALL" -> ALL
        "PRODUCT" -> PRODUCT
        "GROUP" -> GROUP
        else -> throw IllegalArgumentException("Unknown PromotionScope value: '" + value + "'")
      }
  }
}
