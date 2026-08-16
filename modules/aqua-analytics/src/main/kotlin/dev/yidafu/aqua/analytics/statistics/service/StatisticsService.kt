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

package dev.yidafu.aqua.analytics.statistics.service

import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatusDistributionItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.analytics.statistics.dto.PeakHourDTO
import dev.yidafu.aqua.analytics.statistics.dto.PendingOrderBreakdownDTO
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
  ): OrderStatisticsDTO

  /**
   * 获取每日订单统计
   */
  fun getDailyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO>

  /**
   * 获取每周订单统计
   */
  fun getWeeklyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO>

  /**
   * 获取每月订单统计
   */
  fun getMonthlyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO>

  /**
   * 获取待处理订单存量快照（待支付/待派单/待配送三态合计）
   */
  fun getPendingOrderBreakdown(): PendingOrderBreakdownDTO

  /**
   * 获取日期窗口内订单状态分布（G37，7 态补零，合计 = 订单总数）
   */
  fun getOrderStatusDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<OrderStatusDistributionItemDTO>

  /**
   * 获取日期窗口内容单价分布（G37，默认金额区间补零，合计 = 订单总数）
   */
  fun getOrderValueDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<OrderValueBucketDTO>

  /**
   * 获取日期窗口内高峰时段分析（G37，0-23 时补零，合计 = 订单总数）
   */
  fun getPeakHourDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<PeakHourDTO>
}
