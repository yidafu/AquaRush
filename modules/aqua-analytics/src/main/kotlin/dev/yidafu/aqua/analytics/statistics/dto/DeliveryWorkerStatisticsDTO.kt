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
 * 送水员统计结果DTO
 */
data class DeliveryWorkerStatisticsDTO(
  val totalWorkers: Long,
  val todayActiveWorkers: Long,
  val deliveringOrders: Long,
  val todayCompletedOrders: Long,
  // 人均单量 = 今日完成订单总数 / 今日在岗送水员数(在岗为 0 时为 null,G36)
  val averageOrdersPerWorker: Float? = null,
  // 平均履约时长(分钟)= Σ(completedAt − 派单时间) / 今日完成订单数;无完成订单时为 null(G36)
  val averageFulfillmentMinutes: Float? = null,
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
