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

package dev.yidafu.aqua.statistics.model.repository

import java.time.LocalDateTime

/**
 * 用户统计 Repository 自定义接口
 */
interface StatisticsUserRepositoryCustom {
  /**
   * 统计指定日期范围内每日新增用户数
   */
  fun countDailyNewUsers(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<java.time.LocalDate, Long>

  /**
   * 统计指定日期之后创建的用户总数
   */
  fun countUsersCreatedAfter(dateTime: LocalDateTime): Long

  /**
   * 统计指定时间范围内有订单的用户数
   */
  fun countActiveUsersSince(dateTime: LocalDateTime): Long
}
