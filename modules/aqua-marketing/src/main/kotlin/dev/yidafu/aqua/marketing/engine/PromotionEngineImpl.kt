package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.marketing.PromotionPricingService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 促销引擎实现(PRD 10-marketing.md:营销规则与配置的唯一归属,订单域应用):
 * 规则按 R-MKT-02/07 顺序执行(单价解析 → 水票核销 → 满减/折扣 → 优惠券),封顶不超应付;
 * 多商品订单(F-ORD-004):单价逐商品解析,订单层优惠按聚合口径;
 * preview 纯计算;applyForOrder 在调用方事务内同计算并原子消费(失败整体回滚)。
 */
@Service
class PromotionEngineImpl(
  private val productQueryApiService: ProductQueryApiService,
  rules: List<PricingRule>,
) : PromotionPricingService {
  private val orderedRules = rules.sortedBy { it.order }

  override fun preview(request: OrderPricingRequest): OrderPricingResult = compute(buildContext(request, orderId = null))

  override fun applyForOrder(
    orderId: Long,
    request: OrderPricingRequest,
  ): OrderPricingResult = compute(buildContext(request, orderId = orderId))

  private fun buildContext(
    request: OrderPricingRequest,
    orderId: Long?,
  ): PricingContext {
    // 同一商品不可重复出现(购物车按 productId 合并,后端兜底防重复计价/扣减)
    val duplicate =
      request.items
        .groupBy { it.productId }
        .entries
        .firstOrNull { it.value.size > 1 }
    if (duplicate != null) {
      throw BadRequestException("商品重复: ${duplicate.key}")
    }
    val items =
      request.items.map { item ->
        val product =
          productQueryApiService.findById(item.productId)
            ?: throw NotFoundException("产品不存在: ${item.productId}")
        PricingItemContext(
          productId = item.productId,
          quantity = item.quantity,
          product = product,
        )
      }
    return PricingContext(
      userId = request.userId,
      items = items,
      isSelfCollect = request.isSelfCollect,
      preferredCouponId = request.preferredCouponId,
      ticketRedeemCount = request.ticketRedeemCount,
      now = LocalDateTime.now(),
      orderId = orderId,
    )
  }

  private fun compute(ctx: PricingContext): OrderPricingResult {
    val state = PricingState()
    orderedRules.forEach { it.evaluate(ctx, state) }
    return OrderPricingResult(
      items = state.items,
      unitPriceCents = state.unitPriceCents,
      priceSource = state.priceSource,
      subtotalCents = state.subtotalCents,
      ticketRedeemCount = state.ticketRedeemCount,
      ticketCoveredQuantity = state.ticketCoveredQuantity,
      cashSubtotalCents = state.cashSubtotalCents,
      promotionDiscountCents = state.promotionDiscountCents,
      promotionActivityId = state.promotionActivityId,
      promotionActivityName = state.promotionActivityName,
      firstOrderDiscountCents = state.firstOrderDiscountCents,
      firstOrderActivityId = state.firstOrderActivityId,
      firstOrderActivityName = state.firstOrderActivityName,
      couponDiscountCents = state.couponDiscountCents,
      selectedCouponId = state.selectedCouponId,
      selectedCouponName = state.selectedCouponName,
      totalDiscountCents = state.totalDiscountCents,
      payableProductCents = state.payableProductCents,
      availableTicketCount = state.availableTicketCount,
      couponOptions = state.couponOptions,
      giftItems = state.giftItems,
    )
  }
}
