/*
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
 * along with this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 */

package dev.yidafu.aqua.api.service

import java.math.BigDecimal
import java.time.LocalDate

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
    val totalAmount: BigDecimal get() = dev.yidafu.aqua.common.utils.MoneyUtils.fromCents(totalAmountCents)
    val completedAmount: BigDecimal get() = dev.yidafu.aqua.common.utils.MoneyUtils.fromCents(completedAmountCents)
  }

  data class DailyStatistics(
    val date: LocalDate,
    val orderCount: Long,
    val totalAmountCents: Long,
  ) {
    val totalAmount: BigDecimal get() = dev.yidafu.aqua.common.utils.MoneyUtils.fromCents(totalAmountCents)
  }
}
