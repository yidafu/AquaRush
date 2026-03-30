/**
 * AquaRush Statistics GraphQL Resolver
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

package dev.yidafu.aqua.statistics.resolver

import dev.yidafu.aqua.statistics.service.StatisticsService
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import dev.yidafu.aqua.common.graphql.generated.MonthlyStatistic
import dev.yidafu.aqua.common.graphql.generated.OrderStatistics
import dev.yidafu.aqua.common.graphql.generated.WeeklyStatistic
import dev.yidafu.aqua.statistics.mapper.DailyStatisticMapper
import dev.yidafu.aqua.statistics.mapper.MonthlyStatisticMapper
import dev.yidafu.aqua.statistics.mapper.OrderStatisticsMapper
import dev.yidafu.aqua.statistics.mapper.WeeklyStatisticMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class StatisticsQueryResolver(
  private val statisticsService: StatisticsService,
) {
  @QueryMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_WORKER')")
  fun dailyStatistics(
    @Argument input: DateRangeInput,
  ): List<DailyStatistic> {
    val startDate = input.startDate.toLocalDate()
    val endDate = input.endDate.toLocalDate()
    return DailyStatisticMapper.mapList(statisticsService.getDailyStatistics(startDate, endDate))
  }

  @QueryMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_WORKER')")
  fun orderStatistics(
    @Argument input: DateRangeInput,
  ): OrderStatistics {
    val startDate = input.startDate.toLocalDate()
    val endDate = input.endDate.toLocalDate()
    val result = statisticsService.getOrderStatistics(startDate, endDate)
    return OrderStatisticsMapper.map(result)
  }

  @QueryMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_WORKER')")
  fun weeklyStatistics(
    @Argument input: DateRangeInput,
  ): List<WeeklyStatistic> {
    val startDate = input.startDate.toLocalDate()
    val endDate = input.endDate.toLocalDate()
    return WeeklyStatisticMapper.mapList(statisticsService.getWeeklyStatistics(startDate, endDate))
  }

  @QueryMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_WORKER')")
  fun monthlyStatistics(
    @Argument input: DateRangeInput,
  ): List<MonthlyStatistic> {
    val startDate = input.startDate.toLocalDate()
    val endDate = input.endDate.toLocalDate()
    return MonthlyStatisticMapper.mapList(statisticsService.getMonthlyStatistics(startDate, endDate))
  }
}
