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
 * 送水员统计 Repository 自定义接口
 */
interface StatisticsDeliveryWorkerRepositoryCustom {
  /**
   * 统计今日有活跃订单的送水员数
   */
  fun countTodayActiveWorkers(sinceDateTime: LocalDateTime): Long

  /**
   * 统计指定时间范围内的配送中订单数
   */
  fun countDeliveringOrders(): Long

  /**
   * 统计指定时间范围内已完成的订单数
   */
  fun countCompletedOrdersSince(dateTime: LocalDateTime): Long
}
