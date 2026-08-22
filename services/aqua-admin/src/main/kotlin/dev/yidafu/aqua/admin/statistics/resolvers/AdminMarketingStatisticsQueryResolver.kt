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

package dev.yidafu.aqua.admin.statistics.resolvers

import dev.yidafu.aqua.analytics.statistics.service.MarketingStatisticsService
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import dev.yidafu.aqua.common.graphql.generated.MarketingBreakdownKind
import dev.yidafu.aqua.common.graphql.generated.MarketingDailyStatistic
import dev.yidafu.aqua.common.graphql.generated.MarketingStatistics
import dev.yidafu.aqua.common.graphql.generated.MarketingStatisticsSummary
import dev.yidafu.aqua.common.graphql.generated.MarketingUsageBreakdownRow
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 营销统计查询解析器(F-STA-007,PERMISSION_STATISTICS_READ):
 * 第 3 层日汇总中间表(daily_marketing_facts / daily_marketing_usage_facts)查询侧两段合并,
 * 汇总/按日趋势/营销方式使用明细一次返回(单页面单请求,CLAUDE.md 规则 10)。
 * 口径见 PRD 08-statistics.md §F-STA-007 业务规则扩充。
 */
@Controller
class AdminMarketingStatisticsQueryResolver(
  private val marketingStatisticsService: MarketingStatisticsService,
) {
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun marketingStatistics(
    @Argument input: DateRangeInput,
  ): MarketingStatistics {
    val result =
      marketingStatisticsService.getMarketingStatistics(
        input.startDate.toLocalDate(),
        input.endDate.toLocalDate(),
      )
    val summary = result.summary
    return MarketingStatistics(
      summary =
        MarketingStatisticsSummary(
          marketingOrderCount = summary.marketingOrderCount.toInt(),
          totalBenefitCents = summary.totalBenefitCents,
          couponBenefitCents = summary.couponBenefitCents,
          promotionBenefitCents = summary.promotionBenefitCents,
          firstOrderBenefitCents = summary.firstOrderBenefitCents,
          couponRedeemedCount = summary.couponRedeemedCount.toInt(),
          ticketSalesCount = summary.ticketSalesCount.toInt(),
          ticketSalesRevenueCents = summary.ticketSalesRevenueCents,
          ticketRedeemedCount = summary.ticketRedeemedCount.toInt(),
          giftQuantity = summary.giftQuantity.toInt(),
        ),
      daily =
        result.daily.map { dto ->
          MarketingDailyStatistic(
            date = dto.date.toString(),
            marketingOrderCount = dto.marketingOrderCount.toInt(),
            couponBenefitCents = dto.couponBenefitCents,
            promotionBenefitCents = dto.promotionBenefitCents,
            firstOrderBenefitCents = dto.firstOrderBenefitCents,
            couponRedeemedCount = dto.couponRedeemedCount.toInt(),
            ticketSalesCount = dto.ticketSalesCount.toInt(),
            ticketSalesRevenueCents = dto.ticketSalesRevenueCents,
            ticketRedeemedCount = dto.ticketRedeemedCount.toInt(),
            giftQuantity = dto.giftQuantity.toInt(),
          )
        },
      breakdown =
        result.breakdown.map { row ->
          MarketingUsageBreakdownRow(
            kind = MarketingBreakdownKind.valueOf(row.kind.name),
            refId = row.refId,
            name = row.name,
            orderCount = row.orderCount.toInt(),
            benefitCents = row.benefitCents,
            usageCount = row.usageCount.toInt(),
            ticketSalesCount = row.ticketSalesCount.toInt(),
            ticketSalesRevenueCents = row.ticketSalesRevenueCents,
            ticketRedeemedCount = row.ticketRedeemedCount.toInt(),
          )
        },
    )
  }
}
