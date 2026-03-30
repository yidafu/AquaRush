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

package dev.yidafu.aqua.statistics.service

import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import java.time.LocalDate

/**
 * 用户统计服务接口
 */
interface UserStatisticsService {
  /**
   * 获取用户统计数据
   */
  fun getUserStatistics(): UserStatisticsResult

  /**
   * 获取每日新增用户趋势
   */
  fun getDailyNewUsers(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatistic>

  /**
   * 获取用户登录统计
   */
  fun getLoginStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): LoginStatisticsResult

  data class UserStatisticsResult(
    val totalUsers: Long,
    val todayNewUsers: Long,
    val monthNewUsers: Long,
    val activeUsers: Long,
  )

  data class LoginStatisticsResult(
    val todayLogins: Long,
    val totalLogins: Long,
    val dailyLogins: List<DailyLoginStatistic>,
  )

  data class DailyLoginStatistic(
    val date: String,
    val loginCount: Int,
  )
}
