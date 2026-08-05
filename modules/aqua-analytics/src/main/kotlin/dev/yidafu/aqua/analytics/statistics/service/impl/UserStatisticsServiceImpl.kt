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

import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.analytics.statistics.dto.DailyLoginStatisticDTO
import dev.yidafu.aqua.analytics.statistics.dto.LoginStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.UserStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsUserRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.service.UserStatisticsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserStatisticsServiceImpl(
  private val userQueryApiService: UserQueryApiService,
  private val statisticsUserRepository: StatisticsUserRepositoryCustom,
  private val userActionLogRepository: UserActionLogRepository,
) : UserStatisticsService {
  override fun getUserStatistics(): UserStatisticsDTO {
    // 总用户数
    val totalUsers = userQueryApiService.findAll().size.toLong()

    // 今日新增用户数
    val today = LocalDate.now()
    val todayStart = today.atStartOfDay()
    val todayNewUsers = statisticsUserRepository.countUsersCreatedAfter(todayStart)

    // 本月新增用户数
    val monthStart = today.withDayOfMonth(1).atStartOfDay()
    val monthNewUsers = statisticsUserRepository.countUsersCreatedAfter(monthStart)

    // 活跃用户数（30天内有下单的用户）
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
  ): List<DailyStatistic> {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.plusDays(1).atStartOfDay()

    val dateCountMap = statisticsUserRepository.countDailyNewUsers(startDateTime, endDateTime)

    // Fill in missing dates with 0
    val filledResults = mutableListOf<DailyStatistic>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      val count = dateCountMap[currentDate] ?: 0
      filledResults.add(
        DailyStatistic(
          date = currentDate.toString(),
          orderCount = count.toInt(),
          orderProductCount = 0,
          revenue = 0f,
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
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(java.time.LocalTime.MAX)

    // 今日登录用户数
    val today = LocalDate.now()
    val todayStart = today.atStartOfDay()
    val todayEnd = today.atTime(java.time.LocalTime.MAX)
    val todayLogins =
      userActionLogRepository.countDistinctUsersByActionTypeAndDateRange(
        "USER_LOGIN",
        todayStart,
        todayEnd,
      )

    // 累计登录用户数（指定日期范围内）
    val totalLogins =
      userActionLogRepository.countDistinctUsersByActionTypeAndDateRange(
        "USER_LOGIN",
        startDateTime,
        endDateTime,
      )

    // 每日登录趋势
    val dailyLoginsMap = userActionLogRepository.countDailyLoginsByDateRange(startDateTime, endDateTime)

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
}
