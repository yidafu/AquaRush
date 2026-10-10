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
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsOrderRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsUserRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.dto.DailyLoginStatisticDTO
import dev.yidafu.aqua.analytics.statistics.dto.LoginStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderUserDistributionDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderUserTierDTO
import dev.yidafu.aqua.analytics.statistics.dto.UserStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.analytics.statistics.service.UserStatisticsService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.graphql.generated.DailyStatisticVo
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class UserStatisticsServiceImpl(
  private val userQueryApiService: UserQueryApiService,
  private val statisticsUserRepository: StatisticsUserRepositoryCustom,
  private val statisticsOrderRepository: StatisticsOrderRepositoryCustom,
  private val userActionLogRepository: UserActionLogRepository,
  private val dailyUserFactRepository: DailyUserFactRepository,
  private val snapshotCoverageService: StatisticsSnapshotCoverageService,
) : UserStatisticsService {
  override fun getUserStatistics(): UserStatisticsDTO {
    // 总用户数(条件 count,避免全表加载)
    val totalUsers = userQueryApiService.countAll()

    // 今日新增用户数
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val todayStart = today.atStartOfDay()
    val todayNewUsers = statisticsUserRepository.countUsersCreatedAfter(todayStart)

    // 本月新增用户数
    val monthStart = today.withDayOfMonth(1).atStartOfDay()
    val monthNewUsers = statisticsUserRepository.countUsersCreatedAfter(monthStart)

    // 活跃用户数(30天内有下单的用户)
    val thirtyDaysAgo = today.minusDays(30).atStartOfDay()
    val activeUsers = statisticsUserRepository.countActiveUsersSince(thirtyDaysAgo)

    return UserStatisticsDTO(
      totalUsers = totalUsers,
      todayNewUsers = todayNewUsers,
      monthNewUsers = monthNewUsers,
      activeUsers = activeUsers,
    )
  }

  override fun getDailyNewUsers(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticVo> {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)

    // 快照段:昨日及以前读 daily_user_facts(新增用户数)
    val factDates = snapshotCoverageService.coveredDates(startDate, minOf(endDate, yesterday))
    val dateCountMap = mutableMapOf<LocalDate, Long>()
    if (factDates.isNotEmpty()) {
      dailyUserFactRepository
        .findByStatDateBetween(factDates.min(), factDates.max())
        .forEach { fact -> dateCountMap[fact.statDate] = fact.newUserCount }
    }

    // 实时段:未覆盖历史日期 + 今日
    snapshotCoverageService.uncoveredRanges(startDate, endDate, factDates).forEach { (rangeStart, rangeEnd) ->
      statisticsUserRepository
        .countDailyNewUsers(rangeStart.atStartOfDay(), rangeEnd.plusDays(1).atStartOfDay())
        .forEach { (date, count) -> dateCountMap[date] = count }
    }

    // Fill in missing dates with 0
    val filledResults = mutableListOf<DailyStatisticVo>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      val count = dateCountMap[currentDate] ?: 0
      filledResults.add(
        DailyStatisticVo(
          date = currentDate.toString(),
          orderCount = count.toInt(),
          orderProductCount = 0,
          revenueCents = 0L,
          // 用户统计不涉及退款维度,退款金额恒为 0(shared-types 非空字段)
          refundCents = 0L,
        ),
      )
      currentDate = currentDate.plusDays(1)
    }

    return filledResults
  }

  override fun getLoginStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): LoginStatisticsDTO {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)

    // 快照段:昨日及以前读 daily_user_facts(登录用户数)
    val factDates = snapshotCoverageService.coveredDates(startDate, minOf(endDate, yesterday))
    val dailyLoginsMap = mutableMapOf<LocalDate, Long>()
    if (factDates.isNotEmpty()) {
      dailyUserFactRepository
        .findByStatDateBetween(factDates.min(), factDates.max())
        .forEach { fact -> dailyLoginsMap[fact.statDate] = fact.loginUserCount }
    }

    // 实时段:未覆盖历史日期 + 今日
    snapshotCoverageService.uncoveredRanges(startDate, endDate, factDates).forEach { (rangeStart, rangeEnd) ->
      userActionLogRepository
        .countDailyLoginsByDateRange(rangeStart.atStartOfDay(), rangeEnd.atTime(LocalTime.MAX))
        .forEach { (date, count) -> dailyLoginsMap[date] = count }
    }

    // 今日登录用户数(实时,与日期窗口无关的存量指标)
    val todayStart = today.atStartOfDay()
    val todayEnd = today.atTime(LocalTime.MAX)
    val todayLogins =
      userActionLogRepository.countDistinctUsersByActionTypeAndDateRange(
        "USER_LOGIN",
        todayStart,
        todayEnd,
      )

    // 累计登录用户数 = 窗口内去重用户数(区间级 distinct 无法由原子日汇总还原,保持实时查询,窗口有界)
    val totalLogins =
      userActionLogRepository.countDistinctUsersByActionTypeAndDateRange(
        "USER_LOGIN",
        startDate.atStartOfDay(),
        endDate.atTime(LocalTime.MAX),
      )

    val filledResults = mutableListOf<DailyLoginStatisticDTO>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      val count = dailyLoginsMap[currentDate] ?: 0
      filledResults.add(
        DailyLoginStatisticDTO(
          date = currentDate.toString(),
          loginCount = count.toInt(),
        ),
      )
      currentDate = currentDate.plusDays(1)
    }

    return LoginStatisticsDTO(
      todayLogins = todayLogins,
      totalLogins = totalLogins,
      dailyLogins = filledResults,
    )
  }

  override fun getOrderUserDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): OrderUserDistributionDTO {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    // 近30天下单用户聚合(orders 按 userId 去重分组计数,与活跃用户同窗口同源)
    val ordersPerUser = statisticsOrderRepository.countOrdersPerUserInRange(startDateTime, endDateTime)
    val orderingUsers = ordersPerUser.size.toLong()

    // 下单次数分层(1次/2~3次/4次以上,F-STA-003,G35)
    val oneOrder = ordersPerUser.values.count { it == 1L }.toLong()
    val twoToThree = ordersPerUser.values.count { it in 2L..3L }.toLong()
    val fourPlus = (ordersPerUser.size - (oneOrder + twoToThree)).coerceAtLeast(0L)
    val tiers =
      listOf(
        OrderUserTierDTO(label = "1次", userCount = oneOrder),
        OrderUserTierDTO(label = "2~3次", userCount = twoToThree),
        OrderUserTierDTO(label = "4次以上", userCount = fourPlus),
      )

    // 下单用户占比 = 下单用户数 / 总用户数;复购率 = 下单≥2次用户数 / 下单用户数(除零为 0)
    val totalUsers = userQueryApiService.countAll()
    val orderingUserRatio = if (totalUsers > 0) orderingUsers.toFloat() / totalUsers.toFloat() else 0f
    val repurchaseUsers = twoToThree + fourPlus
    val repurchaseRate = if (orderingUsers > 0) repurchaseUsers.toFloat() / orderingUsers.toFloat() else 0f

    return OrderUserDistributionDTO(
      orderingUsers = orderingUsers,
      orderingUserRatio = orderingUserRatio,
      repurchaseRate = repurchaseRate,
      tiers = tiers,
    )
  }
}
