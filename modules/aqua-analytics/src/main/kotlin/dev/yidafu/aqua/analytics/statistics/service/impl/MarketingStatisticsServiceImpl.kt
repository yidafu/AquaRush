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

import dev.yidafu.aqua.analytics.statistics.StatisticsTime
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyMarketingUsageFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsSnapshotAggregationRepository
import dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind
import dev.yidafu.aqua.analytics.statistics.dto.MarketingDailyStatisticDTO
import dev.yidafu.aqua.analytics.statistics.dto.MarketingStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.MarketingStatisticsSummaryDTO
import dev.yidafu.aqua.analytics.statistics.dto.MarketingUsageBreakdownRowDTO
import dev.yidafu.aqua.analytics.statistics.service.MarketingStatisticsService
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.api.dto.MarketingActivityNameRef
import dev.yidafu.aqua.api.service.marketing.MarketingUsageQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.exception.BadRequestException
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 营销统计服务实现(F-STA-007)
 * 两段合并:昨日及以前读快照事实表(覆盖日期),今日/未覆盖日期走实时聚合(与快照写同口径);
 * 明细名称查询侧批量解析(中间表不落 name 列),赠品让利按当前目录价估算。
 */
@Service
class MarketingStatisticsServiceImpl(
  private val marketingFactRepository: DailyMarketingFactRepository,
  private val marketingUsageFactRepository: DailyMarketingUsageFactRepository,
  private val aggregationRepository: StatisticsSnapshotAggregationRepository,
  private val snapshotCoverageService: StatisticsSnapshotCoverageService,
  private val marketingUsageQueryService: MarketingUsageQueryService,
  private val productQueryApiService: ProductQueryApiService,
) : MarketingStatisticsService {
  override fun getMarketingStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): MarketingStatisticsDTO {
    if (startDate.isAfter(endDate)) {
      throw BadRequestException("日期范围无效:开始日期不能晚于结束日期")
    }
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)

    val dailyMap = mutableMapOf<LocalDate, MarketingDailyStatisticDTO>()
    val breakdownAcc = mutableMapOf<BreakdownKey, MarketingUsageBreakdownRowDTO>()

    // 历史段:快照事实表(覆盖区间)
    val factDates = snapshotCoverageService.coveredDates(startDate, minOf(endDate, yesterday))
    if (factDates.isNotEmpty()) {
      val factStart = factDates.min()
      val factEnd = factDates.max()
      marketingFactRepository.findByStatDateBetween(factStart, factEnd).forEach { fact ->
        dailyMap[fact.statDate] = fact.toDailyDTO()
      }
      marketingUsageFactRepository.findByStatDateBetween(factStart, factEnd).forEach { row ->
        accumulateBreakdown(breakdownAcc, row)
      }
    }

    // 未覆盖历史日期 + 今日:实时聚合(与快照写同口径)
    snapshotCoverageService.uncoveredRanges(startDate, endDate, factDates).forEach { (rangeStart, rangeEnd) ->
      aggregationRepository.aggregateMarketingFactsRange(rangeStart, rangeEnd).forEach { fact ->
        dailyMap[fact.statDate] = fact.toDailyDTO()
      }
      aggregationRepository.aggregateMarketingUsageFactsRange(rangeStart, rangeEnd).forEach { row ->
        accumulateBreakdown(breakdownAcc, row)
      }
    }

    // 日序列补零(图表 x 轴连续)
    val daily = dateRange(startDate, endDate).map { date -> dailyMap[date] ?: MarketingDailyStatisticDTO.zero(date) }

    val summary =
      MarketingStatisticsSummaryDTO(
        marketingOrderCount = daily.sumOf { it.marketingOrderCount },
        totalBenefitCents = daily.sumOf { it.couponBenefitCents + it.promotionBenefitCents + it.firstOrderBenefitCents },
        couponBenefitCents = daily.sumOf { it.couponBenefitCents },
        promotionBenefitCents = daily.sumOf { it.promotionBenefitCents },
        firstOrderBenefitCents = daily.sumOf { it.firstOrderBenefitCents },
        couponRedeemedCount = daily.sumOf { it.couponRedeemedCount },
        ticketSalesCount = daily.sumOf { it.ticketSalesCount },
        ticketSalesRevenueCents = daily.sumOf { it.ticketSalesRevenueCents },
        ticketRedeemedCount = daily.sumOf { it.ticketRedeemedCount },
        giftQuantity = daily.sumOf { it.giftQuantity },
      )

    // 明细:名称批量解析(中间表不落 name 列)+ 赠品让利估算(件数 × 赠品当前目录价)
    val refs = breakdownAcc.keys.map { MarketingActivityNameRef(it.kind.name, it.refId) }
    val resolvedNames = marketingUsageQueryService.resolveMarketingActivityNames(refs)
    val breakdown =
      breakdownAcc.values
        .map { row ->
          val info = resolvedNames[MarketingActivityNameRef(row.kind.name, row.refId)]
          val giftBenefit =
            if (row.kind == MarketingBreakdownKind.GIFT_ACTIVITY) {
              val giftProductId = info?.giftProductId
              if (giftProductId != null) row.usageCount * (productQueryApiService.findById(giftProductId)?.price ?: 0L) else 0L
            } else {
              0L
            }
          row.copy(name = info?.name ?: "", benefitCents = row.benefitCents + giftBenefit)
        }.sortedWith(
          compareBy<MarketingUsageBreakdownRowDTO> { it.kind.ordinal }
            .thenByDescending { it.benefitCents + it.ticketSalesRevenueCents },
        )

    return MarketingStatisticsDTO(summary = summary, daily = daily, breakdown = breakdown)
  }

  private data class BreakdownKey(
    val kind: MarketingBreakdownKind,
    val refId: Long,
  )

  /**
   * 明细跨日累加:按 (kind, ref_id) 合并(营销订单数在汇总口径按订单去重,不能由明细求和得出)。
   */
  private fun accumulateBreakdown(
    acc: MutableMap<BreakdownKey, MarketingUsageBreakdownRowDTO>,
    row: DailyMarketingUsageFactModel,
  ) {
    val kind = runCatching { MarketingBreakdownKind.fromValue(row.kind) }.getOrNull() ?: return
    val key = BreakdownKey(kind, row.refId)
    val existing = acc[key]
    acc[key] =
      if (existing == null) {
        MarketingUsageBreakdownRowDTO(
          kind = kind,
          refId = row.refId,
          name = "",
          orderCount = row.orderCount,
          benefitCents = row.benefitCents,
          usageCount = row.usageCount,
          ticketSalesCount = row.ticketSalesCount,
          ticketSalesRevenueCents = row.ticketSalesRevenueCents,
          ticketRedeemedCount = row.ticketRedeemedCount,
        )
      } else {
        existing.copy(
          orderCount = existing.orderCount + row.orderCount,
          benefitCents = existing.benefitCents + row.benefitCents,
          usageCount = existing.usageCount + row.usageCount,
          ticketSalesCount = existing.ticketSalesCount + row.ticketSalesCount,
          ticketSalesRevenueCents = existing.ticketSalesRevenueCents + row.ticketSalesRevenueCents,
          ticketRedeemedCount = existing.ticketRedeemedCount + row.ticketRedeemedCount,
        )
      }
  }

  private fun DailyMarketingFactModel.toDailyDTO(): MarketingDailyStatisticDTO =
    MarketingDailyStatisticDTO(
      date = statDate,
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

  private fun dateRange(
    start: LocalDate,
    end: LocalDate,
  ): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }
      .takeWhile { !it.isAfter(end) }
      .toList()
}
