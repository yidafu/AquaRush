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

package dev.yidafu.aqua.analytics.statistics.dto

/**
 * 用户统计结果DTO
 */
data class UserStatisticsDTO(
  val totalUsers: Long,
  val todayNewUsers: Long,
  val monthNewUsers: Long,
  val activeUsers: Long,
)

/**
 * 登录统计结果DTO
 */
data class LoginStatisticsDTO(
  val todayLogins: Long,
  val totalLogins: Long,
  val dailyLogins: List<DailyLoginStatisticDTO>,
)

/**
 * 每日登录统计DTO
 */
data class DailyLoginStatisticDTO(
  val date: String,
  val loginCount: Int,
)