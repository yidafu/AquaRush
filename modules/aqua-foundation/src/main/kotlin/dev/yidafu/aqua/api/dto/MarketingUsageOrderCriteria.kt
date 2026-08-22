package dev.yidafu.aqua.api.dto

import java.time.LocalDateTime

/**
 * 营销使用订单匹配条件(PRD 10-marketing.md 营销方式详情「相关订单记录/使用汇总」):
 * - PROMOTION_ACTIVITY / FIRST_ORDER_REDUCE / COUPON_TEMPLATE / GIFT_ACTIVITY:kind + refId 即可;
 * - FLASH_SALE_ITEM / NEWCOMER_PRICE_ITEM:按「商品 + 活动价 + 活动时间窗」匹配 order_items
 *   (订单未落活动 ID 快照,近似口径),此时需提供 productId/unitPriceCents/validFrom/validTo,refId 不参与匹配。
 */
data class MarketingUsageOrderCriteria(
  val kind: MarketingUsageKind,
  val refId: Long,
  val productId: Long? = null,
  val unitPriceCents: Long? = null,
  val validFrom: LocalDateTime? = null,
  val validTo: LocalDateTime? = null,
)
