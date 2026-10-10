package dev.yidafu.aqua.common.domain.model.enums

/**
 * 桶水票票种类型(PRD 10-marketing.md §R-MKT-08):
 * - SPECIFIC_PRODUCT: 指定商品票(仅抵指定商品,配合 productId)
 * - UNIVERSAL: 通兑票(抵限定规格/容量下的全场商品,配合 specification,跨规格不通用防套利)
 */
enum class TicketTypeKind {
  SPECIFIC_PRODUCT,
  UNIVERSAL,
  ;

  companion object {
    fun fromValue(value: String): TicketTypeKind =
      when (value) {
        "SPECIFIC_PRODUCT" -> SPECIFIC_PRODUCT
        "UNIVERSAL" -> UNIVERSAL
        else -> throw IllegalArgumentException("Unknown TicketTypeKind value: '" + value + "'")
      }
  }
}
