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

import dev.yidafu.aqua.common.utils.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 统计服务接口
 */
interface StatisticsService {
  /**
   * 获取日期范围内的订单统计
   */
  fun getOrderStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): OrderStatistics

  /**
   * 获取每日订单统计
   */
  fun getDailyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatistics>

  /**
   * 获取每周订单统计
   */
  fun getWeeklyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatistics>

  /**
   * 获取每月订单统计
   */
  fun getMonthlyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatistics>

  data class OrderStatistics(
    val totalOrders: Long,
    val totalAmountCents: Long,
    val completedOrders: Long,
    val completedAmountCents: Long,
  ) {
    val totalAmount: BigDecimal
      get() =
        MoneyUtils
          .fromCents(totalAmountCents)
    val completedAmount: BigDecimal
      get() =
        MoneyUtils
          .fromCents(completedAmountCents)
  }

  data class DailyStatistics(
    val date: LocalDate,
    val orderCount: Long,
    val orderProductCount: Long,
    val totalAmountCents: Long,
  ) {
    val totalAmount: BigDecimal
      get() =
        MoneyUtils
          .fromCents(totalAmountCents)

    companion object {
      fun default(date: LocalDate) = DailyStatistics(date, 0L, 0L, 0L)
    }
  }
}
