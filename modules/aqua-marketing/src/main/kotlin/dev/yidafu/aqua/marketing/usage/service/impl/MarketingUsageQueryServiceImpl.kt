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

package dev.yidafu.aqua.marketing.usage.service.impl

import dev.yidafu.aqua.api.dto.MarketingActivityName
import dev.yidafu.aqua.api.dto.MarketingActivityNameRef
import dev.yidafu.aqua.api.dto.MarketingUsageItemRow
import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.api.dto.MarketingUsagePage
import dev.yidafu.aqua.api.dto.MarketingUsageSummary
import dev.yidafu.aqua.api.service.marketing.MarketingUsageQueryService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import org.springframework.stereotype.Service

/**
 * 营销使用情况查询服务实现(PRD 10-marketing.md 营销方式详情):
 * 返回「营销使用汇总 + 相关订单分页」;营销方式不存在时返回空汇总(0)与空分页。
 * 口径见 MarketingUsageKind / MarketingUsageSummary;限时特价/新人专享价按「商品+活动价+时间窗」
 * 匹配 order_items(订单未落活动 ID 快照,近似口径),赠品让利按当前目录价估算。
 */
@Service
class MarketingUsageQueryServiceImpl(
  private val orderQueryApiService: OrderQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
  private val userCouponRepository: UserCouponRepository,
  private val flashSaleActivityRepository: FlashSaleActivityRepository,
  private val giftActivityRepository: GiftActivityRepository,
  private val firstOrderActivityRepository: FirstOrderActivityRepository,
  private val couponTemplateRepository: CouponTemplateRepository,
  private val promotionActivityRepository: PromotionActivityRepository,
  private val ticketTypeRepository: TicketTypeRepository,
) : MarketingUsageQueryService {
  override fun resolveMarketingActivityNames(refs: List<MarketingActivityNameRef>): Map<MarketingActivityNameRef, MarketingActivityName> {
    if (refs.isEmpty()) return emptyMap()
    val byKind = refs.groupBy { it.kind }
    val names = mutableMapOf<MarketingActivityNameRef, MarketingActivityName>()

    byKind[KIND_COUPON_TEMPLATE]?.let { list ->
      val idToName = couponTemplateRepository.findAllById(list.map { it.refId }).associate { it.id!! to it.name }
      list.forEach { ref -> names[ref] = MarketingActivityName(idToName[ref.refId] ?: "") }
    }
    byKind[KIND_PROMOTION_ACTIVITY]?.let { list ->
      val idToName = promotionActivityRepository.findAllById(list.map { it.refId }).associate { it.id!! to it.name }
      list.forEach { ref -> names[ref] = MarketingActivityName(idToName[ref.refId] ?: "") }
    }
    byKind[KIND_FIRST_ORDER_ACTIVITY]?.let { list ->
      val idToName = firstOrderActivityRepository.findAllById(list.map { it.refId }).associate { it.id!! to it.name }
      list.forEach { ref -> names[ref] = MarketingActivityName(idToName[ref.refId] ?: "") }
    }
    byKind[KIND_GIFT_ACTIVITY]?.let { list ->
      val byId = giftActivityRepository.findAllById(list.map { it.refId }).associateBy { it.id!! }
      list.forEach { ref ->
        val activity = byId[ref.refId]
        names[ref] = MarketingActivityName(activity?.name ?: "", activity?.giftProductId)
      }
    }
    byKind[KIND_TICKET_TYPE]?.let { list ->
      val idToName = ticketTypeRepository.findAllById(list.map { it.refId }).associate { it.id!! to it.name }
      list.forEach { ref -> names[ref] = MarketingActivityName(idToName[ref.refId] ?: "") }
    }
    return names
  }

  override fun couponTemplateUsage(
    templateId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage {
    val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.COUPON_TEMPLATE, templateId)
    val orderCount = orderQueryApiService.countOrdersByMarketingUsage(criteria)
    return MarketingUsagePage(
      summary =
        MarketingUsageSummary(
          orderCount = orderCount,
          benefitCents = orderQueryApiService.sumMarketingDiscountCents(criteria),
          usageCount = userCouponRepository.countByCouponTemplateIdAndStatus(templateId, UserCouponStatus.USED),
        ),
      orders = orderQueryApiService.findOrdersByMarketingUsage(criteria, page, size),
    )
  }

  override fun promotionActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage {
    val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, activityId)
    val orderCount = orderQueryApiService.countOrdersByMarketingUsage(criteria)
    return MarketingUsagePage(
      summary =
        MarketingUsageSummary(
          orderCount = orderCount,
          benefitCents = orderQueryApiService.sumMarketingDiscountCents(criteria),
          usageCount = orderCount,
        ),
      orders = orderQueryApiService.findOrdersByMarketingUsage(criteria, page, size),
    )
  }

  override fun flashSaleActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage {
    val activity = flashSaleActivityRepository.findById(activityId).orElse(null) ?: return MarketingUsagePage()
    val criteria =
      MarketingUsageOrderCriteria(
        kind = MarketingUsageKind.FLASH_SALE_ITEM,
        refId = activityId,
        productId = activity.productId,
        unitPriceCents = activity.salePriceCents,
        validFrom = activity.validFrom,
        validTo = activity.validTo,
      )
    return buildItemUsagePage(criteria, page, size)
  }

  override fun giftActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage {
    val activity = giftActivityRepository.findById(activityId).orElse(null) ?: return MarketingUsagePage()
    val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.GIFT_ACTIVITY, activityId)
    val rows = orderQueryApiService.aggregateMarketingUsageItems(criteria)
    return MarketingUsagePage(
      summary =
        MarketingUsageSummary(
          orderCount = orderQueryApiService.countOrdersByMarketingUsage(criteria),
          benefitCents = benefitFromItems(rows),
          usageCount = rows.sumOf { it.quantitySum },
        ),
      orders = orderQueryApiService.findOrdersByMarketingUsage(criteria, page, size),
    )
  }

  override fun firstOrderActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage {
    val activity = firstOrderActivityRepository.findById(activityId).orElse(null) ?: return MarketingUsagePage()
    return when (activity.activityType) {
      FirstOrderActivityType.FIRST_ORDER_REDUCE -> {
        val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.FIRST_ORDER_REDUCE, activityId)
        val orderCount = orderQueryApiService.countOrdersByMarketingUsage(criteria)
        MarketingUsagePage(
          summary =
            MarketingUsageSummary(
              orderCount = orderCount,
              benefitCents = orderQueryApiService.sumMarketingDiscountCents(criteria),
              usageCount = orderCount,
            ),
          orders = orderQueryApiService.findOrdersByMarketingUsage(criteria, page, size),
        )
      }

      FirstOrderActivityType.NEWCOMER_PRICE -> {
        val criteria =
          MarketingUsageOrderCriteria(
            kind = MarketingUsageKind.NEWCOMER_PRICE_ITEM,
            refId = activityId,
            productId = activity.productId,
            unitPriceCents = activity.newcomerPriceCents,
            validFrom = activity.validFrom,
            validTo = activity.validTo,
          )
        buildItemUsagePage(criteria, page, size)
      }
    }
  }

  /**
   * 特价/新人价/赠品使用情况:按商品聚合行计算让利金额与使用数量。
   */
  private fun buildItemUsagePage(
    criteria: MarketingUsageOrderCriteria,
    page: Int,
    size: Int,
  ): MarketingUsagePage {
    val rows = orderQueryApiService.aggregateMarketingUsageItems(criteria)
    return MarketingUsagePage(
      summary =
        MarketingUsageSummary(
          orderCount = orderQueryApiService.countOrdersByMarketingUsage(criteria),
          benefitCents = benefitFromItems(rows),
          usageCount = rows.sumOf { it.quantitySum },
        ),
      orders = orderQueryApiService.findOrdersByMarketingUsage(criteria, page, size),
    )
  }

  /**
   * 让利金额估算(口径见 MarketingUsageSummary):
   * - 特价/新人价:Σ(当前目录价 − 成交单价) × 数量,成交单价取订单快照(精确),目录价取商品当前价(估算);
   * - 赠品:unitPriceCents 为 null(0 价行),让利 = Σ 赠品数量 × 当前目录价;
   * 目录价低于成交价(改价)时该项按 0 计,避免负让利。
   */
  private fun benefitFromItems(rows: List<MarketingUsageItemRow>): Long {
    val catalogById =
      rows
        .map { it.productId }
        .distinct()
        .associateWith { productQueryApiService.findById(it)?.price ?: 0L }
    return rows.sumOf { row ->
      val unitPrice = row.unitPriceCents ?: 0L
      val catalogPrice = catalogById[row.productId] ?: 0L
      (row.quantitySum * (catalogPrice - unitPrice)).coerceAtLeast(0L)
    }
  }

  companion object {
    // 营销统计明细类型(MarketingBreakdownKind 的 name,以字符串承载跨模块解耦)
    private const val KIND_COUPON_TEMPLATE = "COUPON_TEMPLATE"
    private const val KIND_PROMOTION_ACTIVITY = "PROMOTION_ACTIVITY"
    private const val KIND_FIRST_ORDER_ACTIVITY = "FIRST_ORDER_ACTIVITY"
    private const val KIND_GIFT_ACTIVITY = "GIFT_ACTIVITY"
    private const val KIND_TICKET_TYPE = "TICKET_TYPE"
  }
}
