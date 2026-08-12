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

import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import dev.yidafu.aqua.common.graphql.generated.PendingOrderBreakdown
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 仪表盘统计查询解析器
 * 提供每日订单统计与待处理订单存量快照，仅授予 STATISTICS_READ 权限的角色可访问
 */
@Controller
class DailyStatisticsQueryResolver(
  private val statisticsService: StatisticsService,
) {
  /**
   * 按日订单统计（订单数口径：当日全部订单；营收口径：已完成订单金额合计）
   * date 输出 YYYY-MM-DD（LocalDate.toString()），前端切片依赖此格式
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun dailyStatistics(
    @Argument input: DateRangeInput,
  ): List<DailyStatistic> =
    statisticsService
      .getDailyStatistics(input.startDate.toLocalDate(), input.endDate.toLocalDate())
      .map {
        DailyStatistic(
          date = it.date.toString(),
          orderCount = it.orderCount.toInt(),
          orderProductCount = it.orderProductCount.toInt(),
          revenueCents = it.totalAmountCents,
        )
      }

  /**
   * 待处理订单存量快照（待支付/待派单/待配送三态合计）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun pendingOrderBreakdown(): PendingOrderBreakdown {
    val result = statisticsService.getPendingOrderBreakdown()
    return PendingOrderBreakdown(
      pendingPayment = result.pendingPayment.toInt(),
      pendingDispatch = result.pendingDispatch.toInt(),
      pendingDelivery = result.pendingDelivery.toInt(),
      total = result.total.toInt(),
    )
  }
}
