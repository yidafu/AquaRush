package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.PriceSource

/**
 * 促销引擎计价结果(R-MKT-02/07/08/09/10 全链路,多商品订单 F-ORD-004):
 * 费用明细:逐商品 成交单价×数量=小计(items 明细);水票覆盖桶按单价 0 计入 → 现金小计(聚合);
 * 满减/折扣 或 首单立减(叠加①,互斥取最大)→ 优惠券(叠加③);赠品为附加 0 价行不参与现金;
 * 封顶:总优惠不超过现金小计,payableProductCents ≥ 0;券门槛按现金小计(未含优惠)判定。
 */
data class OrderPricingResult(
  // 商品明细计价结果(F-ORD-004:逐商品单价/小计/水票覆盖,权威明细数据源)
  val items: List<OrderPricingItemResult>,
  // 成交单价(分,首件商品,订单快照列 unit_price_cents 用 —— R-MKT-02)
  val unitPriceCents: Long,
  // 成交单价来源(首件商品,订单快照列 price_source 用 —— R-MKT-01)
  val priceSource: PriceSource,
  // 商品小计(分)= Σ(成交单价 × 数量)
  val subtotalCents: Long,
  // 水票核销张数(一票抵一桶 R-MKT-08)
  val ticketRedeemCount: Int,
  // 被水票覆盖的商品总数量(按单价 0 计入应付)
  val ticketCoveredQuantity: Int,
  // 现金部分商品小计(分)= 小计 - 覆盖桶按单价计的小计(覆盖桶单价0)
  val cashSubtotalCents: Long,
  // 满减/折扣优惠(叠加①,R-MKT-07)
  val promotionDiscountCents: Long,
  val promotionActivityId: Long?,
  val promotionActivityName: String?,
  // 首单立减优惠(叠加①,与满减互斥取最大,R-MKT-10)
  val firstOrderDiscountCents: Long,
  val firstOrderActivityId: Long?,
  val firstOrderActivityName: String?,
  // 优惠券优惠(叠加③,R-MKT-07)
  val couponDiscountCents: Long,
  // 实际采用的券(null=未用券)
  val selectedCouponId: Long?,
  val selectedCouponName: String?,
  // 总优惠(分)= 满减或首单 + 券
  val totalDiscountCents: Long,
  // 商品应付(分)= cashSubtotal - totalDiscount,封顶 ≥ 0
  val payableProductCents: Long,
  // 可用水票张数(预检口径)
  val availableTicketCount: Int,
  // 全部可用券及各自优惠金额(引擎计算,最优默认勾选,用户可改选/取消)
  val couponOptions: List<CouponOption>,
  // 买赠/满赠赠品(附加 0 价行,不参与现金计价与叠加,R-MKT-09)
  val giftItems: List<GiftItem>,
)

/**
 * 商品明细计价结果(F-ORD-004 多商品下单):
 * 成交单价与来源按 R-MKT-02 逐商品解析;subtotal=成交单价×数量;
 * ticketCoveredQuantity 为该商品被水票覆盖的桶数(按单价 0 计入应付);
 * cashSubtotal=(数量-覆盖数)×成交单价;catalogPriceCents 为目录价(划线展示用)。
 */
data class OrderPricingItemResult(
  val productId: Long,
  val quantity: Int,
  // 成交单价(分,R-MKT-02:分组价→特价→新人价→目录价)
  val unitPriceCents: Long,
  // 成交单价来源(R-MKT-01)
  val priceSource: PriceSource,
  // 目录价(分,非目录价成交时划线展示)
  val catalogPriceCents: Long,
  // 商品小计(分)= 成交单价 × 数量
  val subtotalCents: Long,
  // 被水票覆盖的商品数量(按单价 0 计入应付)
  val ticketCoveredQuantity: Int,
  // 现金部分小计(分)=(数量-覆盖数)×成交单价
  val cashSubtotalCents: Long,
)

/**
 * 赠品行(PRD 10-marketing.md §R-MKT-09):
 * 赠品为附加 0 价行随单配送,不参与现金计价与叠加。
 */
data class GiftItem(
  val productId: Long,
  val productName: String,
  val quantity: Int,
  val giftActivityId: Long? = null,
  val giftActivityName: String? = null,
)

/**
 * 单张可用券的计价选项(引擎为「最优优惠组合」计算):
 * discountCents 为该券在本订单上下文下的实际优惠金额;isBest=引擎选出的最优券(默认勾选)。
 */
data class CouponOption(
  val couponId: Long,
  val name: String,
  val discountCents: Long,
  val usable: Boolean,
  val isBest: Boolean,
)
