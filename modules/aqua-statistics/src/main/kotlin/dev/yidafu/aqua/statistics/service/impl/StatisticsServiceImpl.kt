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

package dev.yidafu.aqua.statistics.service.impl

import dev.yidafu.aqua.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.statistics.dto.OrderStatisticsDTO
import dev.yidafu.aqua.statistics.service.StatisticsService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.order.domain.repository.OrderRepositoryCustom
import dev.yidafu.aqua.statistics.model.repository.StatisticsOrderRepositoryCustom
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields

@Service
class StatisticsServiceImpl(
  private val orderRepository: OrderRepositoryCustom,
  private val statisticsOrderRepository: StatisticsOrderRepositoryCustom,
) : StatisticsService {
  /**
   * 获取日期范围内的订单统计
   */
  override fun getOrderStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): OrderStatisticsDTO {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    val allStatuses = OrderModelStatus.entries
    val completedStatuses = listOf(OrderModelStatus.COMPLETED)

    val totalOrders =
      orderRepository.countOrdersByDateRange(
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
        statuses = null,
      )

    val totalAmountCents =
      orderRepository.sumAmountCentsByStatusAndDateRange(
        allStatuses,
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
      )

    val completedOrders =
      orderRepository.countOrdersByDateRange(
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
        statuses = completedStatuses,
      )

    val completedAmountCents =
      orderRepository.sumAmountCentsByStatusAndDateRange(
        completedStatuses,
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
      )

    return OrderStatisticsDTO(
      totalOrders = totalOrders,
      totalAmountCents = totalAmountCents,
      completedOrders = completedOrders,
      completedAmountCents = completedAmountCents,
    )
  }

  /**
   * 获取每日订单统计
   */
  override fun getDailyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO> {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    val dailyStats =
      statisticsOrderRepository.getDailyOrderStatistics(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        deliveryWorkerId = null,
        statuses = null,
      )

    // 构建日期到统计的映射
    val statsMap = dailyStats.associateBy { it.date }

    // 遍历日期范围，填充缺失日期（无订单的日期）
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      val stat = statsMap[currentDate]
      result.add(
        stat ?: DailyStatisticsDTO.default(currentDate),
      )
      currentDate = currentDate.plusDays(1)
    }

    return result
  }

  /**
   * 获取每周订单统计
   */
  override fun getWeeklyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO> {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    val weeklyStats =
      statisticsOrderRepository.getWeeklyOrderStatistics(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        deliveryWorkerId = null,
        statuses = null,
      )

    // 构建周开始日期到统计的映射
    val statsMap = weeklyStats.associateBy { it.date }

    // 遍历周范围，填充缺失周（无订单的周）
    val weekFields = WeekFields.of(DayOfWeek.MONDAY, 4)
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentWeekStart = startDate.with(weekFields.dayOfWeek(), 1)

    while (!currentWeekStart.isAfter(endDate)) {
      val stat = statsMap[currentWeekStart]
      result.add(
        stat ?: DailyStatisticsDTO.default(currentWeekStart),
      )
      currentWeekStart = currentWeekStart.plusWeeks(1)
    }

    return result
  }

  /**
   * 获取每月订单统计
   */
  override fun getMonthlyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO> {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    val monthlyStats =
      statisticsOrderRepository.getMonthlyOrderStatistics(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        deliveryWorkerId = null,
        statuses = null,
      )

    // 构建月份开始日期到统计的映射
    val statsMap = monthlyStats.associateBy { it.date }

    // 遍历月份范围，填充缺失月份（无订单的月份）
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentMonth = startDate.withDayOfMonth(1)

    while (!currentMonth.isAfter(endDate)) {
      val stat = statsMap[currentMonth]
      result.add(
        stat ?: DailyStatisticsDTO.default(currentMonth),
      )
      currentMonth = currentMonth.plusMonths(1)
    }

    return result
  }
}
