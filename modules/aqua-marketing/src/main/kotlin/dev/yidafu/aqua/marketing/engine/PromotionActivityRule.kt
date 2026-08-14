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

import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import org.springframework.stereotype.Component

/**
 * 满减/折扣 与 首单立减 规则(R-MKT-04/10,叠加①,互斥取最大):
 * 候选=启用且时间窗内(到期自动失效);范围过滤(全场/指定商品/指定分组);
 * 满减活动内取最高已达标档位(档位递增且互斥)、跨活动比较实际减免取最大者,不叠加;
 * 首单立减仅新用户(无已完成订单)适用,与满减互斥取最大;
 * 达标与减免均按现金部分商品小计(水票覆盖桶剔除后 R-MKT-08);
 * 限时特价商品不参与满减/折扣与首单立减(避免双重让利,R-MKT-07 边界)。
 */
@Component
class PromotionActivityRule(
  private val promotionActivityRepository: PromotionActivityRepository,
  private val promotionActivityTierRepository: PromotionActivityTierRepository,
  private val firstOrderActivityRepository: FirstOrderActivityRepository,
  private val orderQueryApiService: OrderQueryApiService,
  private val marketingPricingQueryApiService: MarketingPricingQueryApiService,
) : PricingRule {
  override val order: Int = 300

  override fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  ) {
    // 限时特价商品不参与叠加①(R-MKT-07 边界:避免双重让利)
    if (state.priceSource == PriceSource.FLASH_SALE_PRICE) {
      state.promotionDiscountCents = 0
      state.promotionActivityId = null
      state.promotionActivityName = null
      state.firstOrderDiscountCents = 0
      state.firstOrderActivityId = null
      state.firstOrderActivityName = null
      return
    }

    val userGroupId = marketingPricingQueryApiService.findGroupId(ctx.userId)
    val cashSubtotal = state.cashSubtotalCents

    // 满减/折扣候选(跨活动取优惠最大者,不叠加)
    var bestDiscount = 0L
    var bestActivityId: Long? = null
    var bestActivityName: String? = null
    promotionActivityRepository
      .findActiveActivities(PromotionActivityStatus.ACTIVE, ctx.now)
      .filter { activity ->
        when (activity.scope) {
          PromotionScope.ALL -> true
          PromotionScope.PRODUCT -> activity.productId == ctx.productId
          PromotionScope.GROUP -> activity.customerGroupId != null && activity.customerGroupId == userGroupId
        }
      }.forEach { activity ->
        val tiers = promotionActivityTierRepository.findByActivityIdOrderBySortAsc(activity.id!!)
        // 活动内取最高已达标档位(档位金额递增且互斥)
        val matchedTier = tiers.lastOrNull { it.thresholdCents <= cashSubtotal } ?: return@forEach
        val discount =
          when (activity.activityType) {
            PromotionActivityType.FULL_REDUCE ->
              matchedTier.discountAmountCents ?: 0
            PromotionActivityType.FULL_DISCOUNT -> {
              val permille = matchedTier.discountRatePermille ?: return@forEach
              cashSubtotal * (10000 - permille) / 10000 // 向下取整到分
            }
          }
        // 优惠封顶不超现金小计(R-MKT-07)
        val capped = minOf(discount, cashSubtotal)
        if (capped > bestDiscount) {
          bestDiscount = capped
          bestActivityId = activity.id
          bestActivityName = activity.name
        }
      }

    // 首单立减候选(仅新用户=无已完成订单;一单一享,首单完成即失效 R-MKT-10)
    var firstOrderBest = 0L
    var firstOrderActivityId: Long? = null
    var firstOrderActivityName: String? = null
    if (!orderQueryApiService.hasCompletedOrders(ctx.userId)) {
      firstOrderActivityRepository
        .findActiveActivities(PromotionActivityStatus.ACTIVE, ctx.now)
        .filter { it.activityType == FirstOrderActivityType.FIRST_ORDER_REDUCE }
        .filter { activity ->
          when (activity.scope) {
            PromotionScope.ALL -> true
            PromotionScope.PRODUCT -> activity.productId == ctx.productId
            PromotionScope.GROUP -> false
          }
        }.forEach { activity ->
          val threshold = activity.thresholdCents ?: return@forEach
          if (cashSubtotal < threshold) return@forEach
          val amount = activity.discountAmountCents
          val rate = activity.discountRatePermille
          val discount =
            when {
              amount != null -> amount
              rate != null ->
                cashSubtotal * (10000 - rate) / 10000 // 向下取整到分
              else -> return@forEach
            }
          val capped = minOf(discount, cashSubtotal)
          if (capped > firstOrderBest) {
            firstOrderBest = capped
            firstOrderActivityId = activity.id
            firstOrderActivityName = activity.name
          }
        }
    }

    // 叠加①:满减与首单立减互斥取最大(R-MKT-07/10)
    if (firstOrderBest > bestDiscount) {
      state.promotionDiscountCents = 0
      state.promotionActivityId = null
      state.promotionActivityName = null
      state.firstOrderDiscountCents = firstOrderBest
      state.firstOrderActivityId = firstOrderActivityId
      state.firstOrderActivityName = firstOrderActivityName
    } else {
      state.promotionDiscountCents = bestDiscount
      state.promotionActivityId = bestActivityId
      state.promotionActivityName = bestActivityName
      state.firstOrderDiscountCents = 0
      state.firstOrderActivityId = null
      state.firstOrderActivityName = null
    }
  }
}
