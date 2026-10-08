package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.dto.OrderPricingItemResult
import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import org.springframework.stereotype.Component

/**
 * 成交单价解析规则(R-MKT-02,多商品订单 F-ORD-004):
 * ① 手动实收价(预留,未接入)→ ② 用户所属分组商品专属价(熟客价)→ ③ 商品目录价;
 * 后续限时特价(链路③)/新人专享价(链路④)按同链路插入优先级;
 * 逐商品解析并写入 state.items,聚合小计=Σ(成交单价×数量);首件单价/来源供聚合展示。
 */
@Component
class UnitPriceRule(
  private val marketingPricingQueryApiService: MarketingPricingQueryApiService,
) : PricingRule {
  override val order: Int = 100

  override fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  ) {
    val results =
      ctx.items.map { item ->
        val resolution = marketingPricingQueryApiService.resolveUnitPrice(ctx.userId, item.productId)
        OrderPricingItemResult(
          productId = item.productId,
          quantity = item.quantity,
          unitPriceCents = resolution.unitPriceCents,
          priceSource = resolution.priceSource,
          catalogPriceCents = item.product.price,
          subtotalCents = resolution.unitPriceCents * item.quantity,
          ticketCoveredQuantity = 0,
          cashSubtotalCents = resolution.unitPriceCents * item.quantity,
        )
      }
    state.items = results.toMutableList()
    val first = results.first()
    state.unitPriceCents = first.unitPriceCents
    state.priceSource = first.priceSource
    state.subtotalCents = results.sumOf { it.subtotalCents }
  }
}
