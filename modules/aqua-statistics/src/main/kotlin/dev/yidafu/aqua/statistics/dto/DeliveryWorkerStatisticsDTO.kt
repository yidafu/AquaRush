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

package dev.yidafu.aqua.statistics.dto

/**
 * 送水员统计结果DTO
 */
data class DeliveryWorkerStatisticsDTO(
  val totalWorkers: Long,
  val todayActiveWorkers: Long,
  val deliveringOrders: Long,
  val todayCompletedOrders: Long,
)

/**
 * 送水员排行榜项DTO
 */
data class DeliveryWorkerRankingItemDTO(
  val workerId: Long,
  val name: String,
  val todayCompletedOrders: Int,
  val rating: Float,
  val totalEarnings: Long,
)