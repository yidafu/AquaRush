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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingUsageFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsSnapshotAggregationRepository
import dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.api.dto.MarketingActivityName
import dev.yidafu.aqua.api.dto.MarketingActivityNameRef
import dev.yidafu.aqua.api.service.marketing.MarketingUsageQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.exception.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

/**
 * 营销统计服务单元测试(F-STA-007):
 * 两段合并(覆盖段读中间表/未覆盖段实时聚合)、日补零、汇总累加、明细跨日合并与名称解析、赠品让利估算、非法区间。
 */
class MarketingStatisticsServiceImplTest :
  FunSpec({
    var marketingFactRepository: DailyMarketingFactRepository = mockk()
    var marketingUsageFactRepository: DailyMarketingUsageFactRepository = mockk()
    var aggregationRepository: StatisticsSnapshotAggregationRepository = mockk()
    var snapshotCoverageService: StatisticsSnapshotCoverageService = mockk()
    var marketingUsageQueryService: MarketingUsageQueryService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var service: MarketingStatisticsServiceImpl = mockk()

    val d1 = LocalDate.of(2026, 1, 1)
    val d2 = LocalDate.of(2026, 1, 2)
    val d3 = LocalDate.of(2026, 1, 3)

    fun fact(
      date: LocalDate,
      marketingOrderCount: Long = 0,
      couponBenefitCents: Long = 0,
      promotionBenefitCents: Long = 0,
      firstOrderBenefitCents: Long = 0,
      couponRedeemedCount: Long = 0,
      ticketSalesCount: Long = 0,
      ticketSalesRevenueCents: Long = 0,
      ticketRedeemedCount: Long = 0,
      giftQuantity: Long = 0,
    ) = DailyMarketingFactModel(
      statDate = date,
      marketingOrderCount = marketingOrderCount,
      couponBenefitCents = couponBenefitCents,
      promotionBenefitCents = promotionBenefitCents,
      firstOrderBenefitCents = firstOrderBenefitCents,
      couponRedeemedCount = couponRedeemedCount,
      ticketSalesCount = ticketSalesCount,
      ticketSalesRevenueCents = ticketSalesRevenueCents,
      ticketRedeemedCount = ticketRedeemedCount,
      giftQuantity = giftQuantity,
    )

    fun usageRow(
      date: LocalDate,
      kind: MarketingBreakdownKind,
      refId: Long,
      orderCount: Long = 0,
      benefitCents: Long = 0,
      usageCount: Long = 0,
    ) = DailyMarketingUsageFactModel(
      statDate = date,
      kind = kind.name,
      refId = refId,
      orderCount = orderCount,
      benefitCents = benefitCents,
      usageCount = usageCount,
    )

    beforeTest {
      marketingFactRepository = mockk()
      marketingUsageFactRepository = mockk()
      aggregationRepository = mockk()
      snapshotCoverageService = mockk()
      marketingUsageQueryService = mockk()
      productQueryApiService = mockk()
      service =
        MarketingStatisticsServiceImpl(
          marketingFactRepository = marketingFactRepository,
          marketingUsageFactRepository = marketingUsageFactRepository,
          aggregationRepository = aggregationRepository,
          snapshotCoverageService = snapshotCoverageService,
          marketingUsageQueryService = marketingUsageQueryService,
          productQueryApiService = productQueryApiService,
        )
    }

    test("非法区间抛 BadRequestException") {
      shouldThrow<BadRequestException> { service.getMarketingStatistics(d2, d1) }
    }

    test("两段合并:覆盖段读中间表,未覆盖段走实时聚合,日序列补零") {
      // d1/d3 已快照覆盖,d2 未覆盖(实时兜底)
      every { snapshotCoverageService.coveredDates(any(), any()) } returns setOf(d1, d3)
      every { snapshotCoverageService.uncoveredRanges(any(), any(), any()) } returns listOf(d2 to d2)
      every { marketingFactRepository.findByStatDateBetween(d1, d3) } returns
        listOf(
          fact(
            d1,
            marketingOrderCount = 2,
            couponBenefitCents = 500,
            promotionBenefitCents = 300,
            ticketSalesCount = 1,
            ticketSalesRevenueCents = 1000,
            giftQuantity = 2,
          ),
          fact(d3, marketingOrderCount = 1, firstOrderBenefitCents = 200, ticketRedeemedCount = 1),
        )
      every { marketingUsageFactRepository.findByStatDateBetween(d1, d3) } returns
        listOf(
          usageRow(d1, MarketingBreakdownKind.COUPON_TEMPLATE, 1L, orderCount = 2, benefitCents = 500, usageCount = 2),
          usageRow(d1, MarketingBreakdownKind.GIFT_ACTIVITY, 9L, orderCount = 1, usageCount = 2),
        )
      every { aggregationRepository.aggregateMarketingFactsRange(d2, d2) } returns
        listOf(fact(d2, marketingOrderCount = 3, promotionBenefitCents = 900, couponRedeemedCount = 1))
      every { aggregationRepository.aggregateMarketingUsageFactsRange(d2, d2) } returns
        listOf(usageRow(d2, MarketingBreakdownKind.PROMOTION_ACTIVITY, 2L, orderCount = 3, benefitCents = 900, usageCount = 3))
      every { marketingUsageQueryService.resolveMarketingActivityNames(any()) } returns
        mapOf(
          MarketingActivityNameRef("COUPON_TEMPLATE", 1L) to MarketingActivityName("满50减10"),
          MarketingActivityNameRef("GIFT_ACTIVITY", 9L) to MarketingActivityName("买5送1", 8L),
          MarketingActivityNameRef("PROMOTION_ACTIVITY", 2L) to MarketingActivityName("满100减20"),
        )
      every { productQueryApiService.findById(8L) } returns ProductModel(price = 1000L)

      val result = service.getMarketingStatistics(d1, d3)

      // 日序列补零:3 天齐全
      result.daily.size shouldBe 3
      result.daily[0].marketingOrderCount shouldBe 2
      result.daily[1].marketingOrderCount shouldBe 3
      result.daily[2].marketingOrderCount shouldBe 1

      // 汇总 = 日序列累加
      val summary = result.summary
      summary.marketingOrderCount shouldBe 6L
      summary.totalBenefitCents shouldBe 500 + 300 + 900 + 200
      summary.couponBenefitCents shouldBe 500L
      summary.promotionBenefitCents shouldBe 300 + 900
      summary.firstOrderBenefitCents shouldBe 200L
      summary.couponRedeemedCount shouldBe 1L
      summary.ticketSalesCount shouldBe 1L
      summary.ticketSalesRevenueCents shouldBe 1000L
      summary.ticketRedeemedCount shouldBe 1L
      summary.giftQuantity shouldBe 2L

      // 明细:跨日合并 + 名称解析 + 赠品让利估算(件数 × 目录价)
      val coupon = result.breakdown.first { it.kind == MarketingBreakdownKind.COUPON_TEMPLATE }
      coupon.name shouldBe "满50减10"
      coupon.orderCount shouldBe 2L
      coupon.benefitCents shouldBe 500L
      coupon.usageCount shouldBe 2L
      val promotion = result.breakdown.first { it.kind == MarketingBreakdownKind.PROMOTION_ACTIVITY }
      promotion.name shouldBe "满100减20"
      promotion.orderCount shouldBe 3L
      promotion.benefitCents shouldBe 900L
      val gift = result.breakdown.first { it.kind == MarketingBreakdownKind.GIFT_ACTIVITY }
      gift.name shouldBe "买5送1"
      gift.benefitCents shouldBe 2L * 1000L
    }

    test("全部日期未覆盖时全部走实时聚合") {
      every { snapshotCoverageService.coveredDates(any(), any()) } returns emptySet()
      every { snapshotCoverageService.uncoveredRanges(any(), any(), any()) } returns listOf(d1 to d1)
      every { aggregationRepository.aggregateMarketingFactsRange(d1, d1) } returns
        listOf(fact(d1, marketingOrderCount = 1, couponBenefitCents = 100))
      every { aggregationRepository.aggregateMarketingUsageFactsRange(d1, d1) } returns emptyList()
      every { marketingUsageQueryService.resolveMarketingActivityNames(any()) } returns emptyMap()

      val result = service.getMarketingStatistics(d1, d1)

      result.summary.marketingOrderCount shouldBe 1L
      result.summary.couponBenefitCents shouldBe 100L
      result.daily.size shouldBe 1
      result.breakdown shouldBe emptyList()
    }

    test("无任何营销数据时汇总为 0,日序列补零") {
      every { snapshotCoverageService.coveredDates(any(), any()) } returns emptySet()
      every { snapshotCoverageService.uncoveredRanges(any(), any(), any()) } returns listOf(d1 to d3)
      every { aggregationRepository.aggregateMarketingFactsRange(any(), any()) } returns
        listOf(fact(d1), fact(d2), fact(d3))
      every { aggregationRepository.aggregateMarketingUsageFactsRange(any(), any()) } returns emptyList()
      every { marketingUsageQueryService.resolveMarketingActivityNames(any()) } returns emptyMap()

      val result = service.getMarketingStatistics(d1, d3)

      result.daily.size shouldBe 3
      result.summary.marketingOrderCount shouldBe 0L
      result.summary.totalBenefitCents shouldBe 0L
      result.breakdown shouldBe emptyList()
    }
  })
