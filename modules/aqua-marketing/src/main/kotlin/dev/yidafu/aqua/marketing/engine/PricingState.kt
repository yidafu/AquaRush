package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.dto.CouponOption
import dev.yidafu.aqua.api.dto.GiftItem
import dev.yidafu.aqua.api.dto.OrderPricingItemResult
import dev.yidafu.aqua.common.domain.model.enums.PriceSource

/**
 * 促销引擎计价中间状态(规则按 R-MKT-07 顺序累积,多商品订单 F-ORD-004):
 * items 为逐商品计价结果(单价/小计/水票覆盖),聚合字段为订单层口径。
 */
data class PricingState(
  // F-ORD-004 逐商品计价结果(UnitPriceRule 初始化,后继规则就地更新)
  var items: MutableList<OrderPricingItemResult> = mutableListOf(),
  // R-MKT-02 单价解析(首件商品,聚合展示用)
  var unitPriceCents: Long = 0,
  var priceSource: PriceSource = PriceSource.CATALOG_PRICE,
  // 商品小计(分)= Σ(成交单价 × 数量)
  var subtotalCents: Long = 0,
  // R-MKT-08 水票核销
  var ticketRedeemCount: Int = 0,
  var ticketCoveredQuantity: Int = 0,
  var cashSubtotalCents: Long = 0,
  var availableTicketCount: Int = 0,
  // R-MKT-07 叠加① 满减/折扣(与首单立减互斥取最大)
  var promotionDiscountCents: Long = 0,
  var promotionActivityId: Long? = null,
  var promotionActivityName: String? = null,
  // R-MKT-07 叠加① 首单立减(仅首单,与满减互斥取最大)
  var firstOrderDiscountCents: Long = 0,
  var firstOrderActivityId: Long? = null,
  var firstOrderActivityName: String? = null,
  // R-MKT-07 叠加③ 优惠券
  var couponDiscountCents: Long = 0,
  var selectedCouponId: Long? = null,
  var selectedCouponName: String? = null,
  var couponOptions: List<CouponOption> = emptyList(),
  // R-MKT-09 买赠/满赠赠品(附加 0 价行,不参与现金计价与叠加)
  var giftItems: List<GiftItem> = emptyList(),
) {
  val totalDiscountCents: Long
    get() = promotionDiscountCents + firstOrderDiscountCents + couponDiscountCents

  val payableProductCents: Long
    get() = maxOf(0, cashSubtotalCents - totalDiscountCents)

  /** 任一商品为限时特价价(叠加① 跳过判定,R-MKT-07 边界) */
  val hasFlashSaleItem: Boolean
    get() = items.any { it.priceSource == PriceSource.FLASH_SALE_PRICE }
}
