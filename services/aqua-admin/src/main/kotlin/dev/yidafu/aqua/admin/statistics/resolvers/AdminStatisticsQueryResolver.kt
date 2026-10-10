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

import dev.yidafu.aqua.analytics.statistics.service.DeliveryWorkerStatisticsService
import dev.yidafu.aqua.analytics.statistics.service.UserStatisticsService
import dev.yidafu.aqua.common.graphql.generated.DailyLoginStatisticVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerOverviewVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerRankingItemVo
import dev.yidafu.aqua.common.graphql.generated.LoginStatisticsVo
import dev.yidafu.aqua.common.graphql.generated.OrderUserDistributionVo
import dev.yidafu.aqua.common.graphql.generated.OrderUserTierVo
import dev.yidafu.aqua.common.graphql.generated.UserStatisticsVo
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate

/**
 * 管理端统计查询解析器
 * 提供用户统计、配送员统计等管理功能，仅管理员可访问
 */
@Controller
class AdminStatisticsQueryResolver(
  private val userStatisticsService: UserStatisticsService,
  private val deliveryWorkerStatisticsService: DeliveryWorkerStatisticsService,
) {
  /**
   * 获取用户统计数据（管理员功能）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun userStatistics(): UserStatisticsVo {
    val result = userStatisticsService.getUserStatistics()
    val thirtyDaysAgo = LocalDate.now().minusDays(30)
    val dailyStats = userStatisticsService.getDailyNewUsers(thirtyDaysAgo, LocalDate.now())

    // 获取登录统计
    val loginResult = userStatisticsService.getLoginStatistics(thirtyDaysAgo, LocalDate.now())
    val loginStats =
      loginResult.dailyLogins.map {
        DailyLoginStatisticVo(
          date = it.date,
          loginCount = it.loginCount,
        )
      }
    val loginStatistics =
      LoginStatisticsVo(
        todayLogins = loginResult.todayLogins.toInt(),
        totalLogins = loginResult.totalLogins.toInt(),
        dailyLogins = loginStats,
      )

    // 下单用户分布(G35,近30天窗口,与 activeUsers 同窗口)
    val distribution = userStatisticsService.getOrderUserDistribution(thirtyDaysAgo, LocalDate.now())
    val orderDistribution =
      OrderUserDistributionVo(
        orderingUsers = distribution.orderingUsers.toInt(),
        orderingUserRatio = distribution.orderingUserRatio,
        repurchaseRate = distribution.repurchaseRate,
        tiers =
          distribution.tiers.map {
            OrderUserTierVo(
              label = it.label,
              userCount = it.userCount.toInt(),
            )
          },
      )

    return UserStatisticsVo(
      totalUsers = result.totalUsers.toInt(),
      todayNewUsers = result.todayNewUsers.toInt(),
      monthNewUsers = result.monthNewUsers.toInt(),
      activeUsers = result.activeUsers.toInt(),
      dailyNewUsers = dailyStats,
      loginStatistics = loginStatistics,
      orderDistribution = orderDistribution,
    )
  }

  /**
   * 获取送水员统计数据（管理员功能）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun deliveryWorkerStatistics(): DeliveryWorkerOverviewVo {
    val result = deliveryWorkerStatisticsService.getDeliveryWorkerStatistics()
    return DeliveryWorkerOverviewVo(
      totalWorkers = result.totalWorkers.toInt(),
      todayActiveWorkers = result.todayActiveWorkers.toInt(),
      deliveringOrders = result.deliveringOrders.toInt(),
      todayCompletedOrders = result.todayCompletedOrders.toInt(),
      averageOrdersPerWorker = result.averageOrdersPerWorker,
      averageFulfillmentMinutes = result.averageFulfillmentMinutes,
    )
  }

  /**
   * 获取送水员排行榜（管理员功能）
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_STATISTICS_READ')")
  fun deliveryWorkerRanking(
    @Argument limit: Int = 10,
  ): List<DeliveryWorkerRankingItemVo> =
    deliveryWorkerStatisticsService
      .getDeliveryWorkerRanking(limit)
      .map {
        DeliveryWorkerRankingItemVo(
          workerId = it.workerId.toString(),
          name = it.name,
          todayCompletedOrders = it.todayCompletedOrders,
          rating = it.rating,
          totalEarningsCents = it.totalEarnings,
        )
      }
}
