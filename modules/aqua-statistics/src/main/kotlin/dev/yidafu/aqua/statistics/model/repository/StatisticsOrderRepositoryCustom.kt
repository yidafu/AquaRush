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

import dev.yidafu.aqua.statistics.service.StatisticsService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 订单统计 Repository 自定义接口
 * 提供按日期、周、月聚合的订单统计查询
 */
interface StatisticsOrderRepositoryCustom {
  /**
   * 按日期分组统计订单数量和金额
   */
  fun getDailyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long? = null,
    statuses: List<OrderModelStatus>? = null,
  ): List<StatisticsService.DailyStatistics>

  /**
   * 按周分组统计订单数量和金额（周一作为周开始）
   */
  fun getWeeklyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long? = null,
    statuses: List<OrderModelStatus>? = null,
  ): List<StatisticsService.DailyStatistics>

  /**
   * 按月分组统计订单数量和金额
   */
  fun getMonthlyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long? = null,
    statuses: List<OrderModelStatus>? = null,
  ): List<StatisticsService.DailyStatistics>
}
