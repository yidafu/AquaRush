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

import dev.yidafu.aqua.api.dto.GiftItem
import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import org.springframework.stereotype.Component

/**
 * 买赠/满赠规则(R-MKT-09):
 * 订单达标自动赠指定商品,赠品为附加 0 价行(不参与现金计价与叠加,不进券门槛);
 * 满赠按现金部分商品小计(水票覆盖桶剔除后)判定,买赠按现金购买桶数判定;
 * 候选=启用且时间窗内;v1 单档单赠品,命中即止(多档阶梯为开放问题)。
 */
@Component
class GiftRule(
  private val giftActivityRepository: GiftActivityRepository,
  private val marketingPricingQueryApiService: MarketingPricingQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
) : PricingRule {
  override val order: Int = 250

  override fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  ) {
    val userGroupId = marketingPricingQueryApiService.findGroupId(ctx.userId)
    val cashSubtotal = state.cashSubtotalCents
    val cashQuantity = ctx.totalQuantity - state.ticketCoveredQuantity

    val candidates =
      giftActivityRepository
        .findActiveActivities(PromotionActivityStatus.ACTIVE, ctx.now)
        .filter { activity ->
          when (activity.scope) {
            PromotionScope.ALL -> true
            PromotionScope.PRODUCT -> activity.productId != null && activity.productId in ctx.productIds
            PromotionScope.GROUP -> activity.customerGroupId != null && activity.customerGroupId == userGroupId
          }
        }

    candidates.forEach { activity ->
      val matched =
        when (activity.giftType) {
          GiftActivityType.FULL_GIFT ->
            cashSubtotal >= (activity.thresholdCents ?: 0L)
          GiftActivityType.BUY_GIFT ->
            cashQuantity >= (activity.thresholdQuantity ?: 0)
        }
      if (matched) {
        val giftName = productQueryApiService.findById(activity.giftProductId)?.name ?: "赠品"
        state.giftItems =
          listOf(
            GiftItem(
              productId = activity.giftProductId,
              productName = giftName,
              quantity = activity.giftQuantity,
              giftActivityId = activity.id,
              giftActivityName = activity.name,
            ),
          )
        return
      }
    }
  }
}
