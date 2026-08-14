/**
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.marketing.PromotionPricingService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.exception.NotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 促销引擎实现(PRD 10-marketing.md:营销规则与配置的唯一归属,订单域应用):
 * 规则按 R-MKT-02/07 顺序执行(单价解析 → 水票核销 → 满减/折扣 → 优惠券),封顶不超应付;
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
    val product =
      productQueryApiService.findById(request.productId)
        ?: throw NotFoundException("产品不存在: ${request.productId}")
    return PricingContext(
      userId = request.userId,
      productId = request.productId,
      quantity = request.quantity,
      isSelfCollect = request.isSelfCollect,
      preferredCouponId = request.preferredCouponId,
      ticketRedeemCount = request.ticketRedeemCount,
      now = LocalDateTime.now(),
      product = product,
      orderId = orderId,
    )
  }

  private fun compute(ctx: PricingContext): OrderPricingResult {
    val state = PricingState()
    orderedRules.forEach { it.evaluate(ctx, state) }
    return OrderPricingResult(
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
