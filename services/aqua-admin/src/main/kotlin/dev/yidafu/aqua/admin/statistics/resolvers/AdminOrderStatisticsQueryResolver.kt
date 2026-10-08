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

import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.common.graphql.generated.DateRange
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import dev.yidafu.aqua.common.graphql.generated.MonthlyStatistic
import dev.yidafu.aqua.common.graphql.generated.OrderStatistics
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.OrderStatusStatistic
import dev.yidafu.aqua.common.graphql.generated.OrderValueBucketStatistic
import dev.yidafu.aqua.common.graphql.generated.PeakHourStatistic
import dev.yidafu.aqua.common.graphql.generated.WeeklyStatistic
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.DayOfWeek
import java.time.temporal.WeekFields

/**
 * 订单统计查询解析器（G42 剩余部分接线）
 * 提供 orderStatistics / weeklyStatistics / monthlyStatistics 查询，仅授予 STATISTICS_READ 权限的角色可访问
 * 营收口径全域统一 = 已完成订单金额合计（PRD/08-statistics.md §域级口径）
 */
@Controller
class AdminOrderStatisticsQueryResolver(
  private val statisticsService: StatisticsService,
) {
  private val isoWeekFields: WeekFields = WeekFields.of(DayOfWeek.MONDAY, 4)

  /**
   * 日期范围内订单统计
   * 营收总额 = 已完成订单金额合计；客单价 = 已完成营收 / 已完成订单数（G38 口径）
   * 状态分布/客单价分布/高峰时段分析（G37）按所选范围全状态聚合，合计 = totalOrders
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun orderStatistics(
    @Argument input: DateRangeInput,
  ): OrderStatistics {
    val startDate = input.startDate.toLocalDate()
    val endDate = input.endDate.toLocalDate()
    val result = statisticsService.getOrderStatistics(startDate, endDate)
    val averageOrderValueCents =
      if (result.completedOrders > 0L) {
        result.completedAmountCents / result.completedOrders
      } else {
        0L
      }
    return OrderStatistics(
      totalOrders = result.totalOrders.toInt(),
      completedOrders = result.completedOrders.toInt(),
      totalRevenueCents = result.completedAmountCents,
      averageOrderValueCents = averageOrderValueCents,
      refundAmountCents = result.refundedAmountCents,
      netRevenueCents = result.netRevenueCents,
      dateRange =
        DateRange(
          startDate = input.startDate,
          endDate = input.endDate,
        ),
      statusDistribution =
        statisticsService
          .getOrderStatusDistribution(startDate, endDate)
          .map {
            OrderStatusStatistic(
              status = OrderStatus.valueOf(it.status.name),
              count = it.count.toInt(),
            )
          },
      orderValueDistribution =
        statisticsService
          .getOrderValueDistribution(startDate, endDate)
          .map {
            OrderValueBucketStatistic(
              fromCents = it.fromCents,
              toCents = it.toCents,
              label = it.label,
              count = it.count.toInt(),
            )
          },
      peakHourDistribution =
        statisticsService
          .getPeakHourDistribution(startDate, endDate)
          .map {
            PeakHourStatistic(
              hour = it.hour,
              count = it.count.toInt(),
            )
          },
    )
  }

  /**
   * 按周订单统计（date 为周一；营收口径 = 已完成订单金额合计）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun weeklyStatistics(
    @Argument input: DateRangeInput,
  ): List<WeeklyStatistic> =
    statisticsService
      .getWeeklyStatistics(input.startDate.toLocalDate(), input.endDate.toLocalDate())
      .map { it.toWeeklyStatistic() }

  /**
   * 按月订单统计（date 为当月 1 日；营收口径 = 已完成订单金额合计）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun monthlyStatistics(
    @Argument input: DateRangeInput,
  ): List<MonthlyStatistic> =
    statisticsService
      .getMonthlyStatistics(input.startDate.toLocalDate(), input.endDate.toLocalDate())
      .map { it.toMonthlyStatistic() }

  private fun DailyStatisticsDTO.toWeeklyStatistic(): WeeklyStatistic {
    val weekStart = date
    return WeeklyStatistic(
      weekNumber = weekStart.get(isoWeekFields.weekOfWeekBasedYear()),
      year = weekStart.get(isoWeekFields.weekBasedYear()),
      startDate = weekStart.toString(),
      endDate = weekStart.plusDays(6).toString(),
      orderCount = orderCount.toInt(),
      orderProductCount = orderProductCount.toInt(),
      revenueCents = totalAmountCents,
      refundCents = refundedAmountCents,
    )
  }

  private fun DailyStatisticsDTO.toMonthlyStatistic(): MonthlyStatistic {
    val monthStart = date
    return MonthlyStatistic(
      month = monthStart.monthValue,
      year = monthStart.year,
      monthName = "${monthStart.monthValue}月",
      orderCount = orderCount.toInt(),
      orderProductCount = orderProductCount.toInt(),
      revenueCents = totalAmountCents,
      refundCents = refundedAmountCents,
    )
  }
}
