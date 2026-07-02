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

package dev.yidafu.aqua.api.dto

/**
 * 配送统计数据DTO
 */
data class DeliveryStatisticsDTO(
  val totalWorkers: Int,
  val onlineWorkers: Int,
  val pendingOrders: Int,
  val deliveringOrders: Int,
)

/**
 * 配送员当日统计数据DTO
 */
data class TodayStatisticsDTO(
  val totalOrders: Int,
  val completedOrders: Int,
  val unfinishedOrders: Int,
  val earningCents: Long,
)

/**
 * 每日统计DTO
 */
data class DailyStatDTO(
  val date: String,
  val orderCount: Int,
  val earningCents: Long,
)

/**
 * 周统计数据DTO
 */
data class WeekStatisticsDTO(
  val dailyStats: List<DailyStatDTO>,
  val totalOrders: Int,
  val totalEarningCents: Long,
)