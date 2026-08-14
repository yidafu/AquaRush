package dev.yidafu.aqua.common.domain.model.enums

/**
 * 首单立减/新人专享价活动类型(PRD 10-marketing.md §R-MKT-10):
 * - FIRST_ORDER_REDUCE: 首单立减(订单级,满 X 减 Y 或打 N 折,与满减互斥取最大)
 * - NEWCOMER_PRICE: 新人专享价(商品级,指定商品对新用户按专享单价计价)
 */
enum class FirstOrderActivityType {
  FIRST_ORDER_REDUCE,
  NEWCOMER_PRICE,
}
