package dev.yidafu.aqua.api.dto

/**
 * 营销使用商品聚合行(限时特价/新人专享价/买赠满赠让利金额计算用):
 * - productId:商品 ID;
 * - unitPriceCents:成交单价(分,赠品无成交单价为 null);
 * - quantitySum:该商品(单价)下的数量合计。
 */
data class MarketingUsageItemRow(
  val productId: Long,
  val unitPriceCents: Long?,
  val quantitySum: Long,
)
