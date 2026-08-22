package dev.yidafu.aqua.api.dto

/**
 * 营销使用情况汇总(PRD 10-marketing.md 营销方式详情「营销使用情况」):
 * - orderCount:使用该营销方式的订单数;
 * - benefitCents:优惠/让利总金额(分)。口径:券/满减/首单立减 = 订单营销优惠快照列合计
 *   (coupon_discount_cents / promotion_discount_cents / first_order_discount_cents);
 *   限时特价/新人专享价 = Σ(当前目录价 − 成交单价) × 数量(估算);
 *   买赠/满赠 = Σ 赠品数量 × 赠品当前目录价(估算);
 * - usageCount:使用/核销数量。券=已核销张数;赠品=赠品总件数;特价/新人价=命中商品件数;满减/首单=使用订单数。
 */
data class MarketingUsageSummary(
  val orderCount: Long = 0,
  val benefitCents: Long = 0,
  val usageCount: Long = 0,
)
