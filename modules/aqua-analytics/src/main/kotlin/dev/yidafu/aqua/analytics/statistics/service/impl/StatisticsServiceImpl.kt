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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsOrderRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatusDistributionItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.analytics.statistics.dto.PeakHourDTO
import dev.yidafu.aqua.analytics.statistics.dto.PendingOrderBreakdownDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields

@Service
class StatisticsServiceImpl(
  private val orderQueryApiService: OrderQueryApiService,
  private val statisticsOrderRepository: StatisticsOrderRepositoryCustom,
) : StatisticsService {
  /**
   * 客单价分布默认金额区间(单位分,PRD F-STA-005 G37 客单价分布):
   * 0-20/20-50/50-100/100-200/200+ 元,末桶开区间(toCents = null,仓储层 CASE ELSE)。
   * 区间边界为服务层常量,便于后续「区间可配置」扩展。
   */
  private val defaultOrderValueBuckets: List<OrderValueBucketDTO> =
    listOf(
      OrderValueBucketDTO(fromCents = 0L, toCents = 2000L, label = "0-20元"),
      OrderValueBucketDTO(fromCents = 2000L, toCents = 5000L, label = "20-50元"),
      OrderValueBucketDTO(fromCents = 5000L, toCents = 10000L, label = "50-100元"),
      OrderValueBucketDTO(fromCents = 10000L, toCents = 20000L, label = "100-200元"),
      OrderValueBucketDTO(fromCents = 20000L, toCents = null, label = "200元以上"),
    )

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
      orderQueryApiService.countByStatusAndCreatedAtBetween(
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
        statuses = null,
      )

    val totalAmountCents =
      orderQueryApiService.sumAmountCentsByStatusAndCreatedAtBetween(
        allStatuses,
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
      )

    val completedOrders =
      orderQueryApiService.countByStatusAndCreatedAtBetween(
        startDateTime,
        endDateTime,
        deliveryWorkerId = null,
        statuses = completedStatuses,
      )

    val completedAmountCents =
      orderQueryApiService.sumAmountCentsByStatusAndCreatedAtBetween(
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
   * 订单数口径：当日全部订单；营收口径：已完成订单金额合计（PRD F-STA-002 定稿）
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

    // 营收按已完成订单口径聚合（与订单数分两次聚合，口径不同）
    val dailyRevenueStats =
      statisticsOrderRepository.getDailyOrderStatistics(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        deliveryWorkerId = null,
        statuses = listOf(OrderModelStatus.COMPLETED),
      )
    val revenueMap = dailyRevenueStats.associateBy { it.date }

    // 构建日期到统计的映射
    val statsMap = dailyStats.associateBy { it.date }

    // 遍历日期范围，填充缺失日期（无订单的日期）
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      val stat = statsMap[currentDate]
      result.add(
        stat?.copy(totalAmountCents = revenueMap[currentDate]?.totalAmountCents ?: 0L)
          ?: DailyStatisticsDTO.default(currentDate),
      )
      currentDate = currentDate.plusDays(1)
    }

    return result
  }

  /**
   * 获取每周订单统计
   * 订单数口径：当周全部订单；营收口径：已完成订单金额合计（与 getDailyStatistics 对齐）
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

    // 营收按已完成订单口径聚合（与订单数分两次聚合，口径不同）
    val weeklyRevenueStats =
      statisticsOrderRepository.getWeeklyOrderStatistics(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        deliveryWorkerId = null,
        statuses = listOf(OrderModelStatus.COMPLETED),
      )
    val revenueMap = weeklyRevenueStats.associateBy { it.date }

    // 构建周开始日期到统计的映射
    val statsMap = weeklyStats.associateBy { it.date }

    // 遍历周范围，填充缺失周（无订单的周）
    val weekFields = WeekFields.of(DayOfWeek.MONDAY, 4)
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentWeekStart = startDate.with(weekFields.dayOfWeek(), 1)

    while (!currentWeekStart.isAfter(endDate)) {
      val stat = statsMap[currentWeekStart]
      result.add(
        stat?.copy(totalAmountCents = revenueMap[currentWeekStart]?.totalAmountCents ?: 0L)
          ?: DailyStatisticsDTO.default(currentWeekStart),
      )
      currentWeekStart = currentWeekStart.plusWeeks(1)
    }

    return result
  }

  /**
   * 获取每月订单统计
   * 订单数口径：当月全部订单；营收口径：已完成订单金额合计（与 getDailyStatistics 对齐）
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

    // 营收按已完成订单口径聚合（与订单数分两次聚合，口径不同）
    val monthlyRevenueStats =
      statisticsOrderRepository.getMonthlyOrderStatistics(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        deliveryWorkerId = null,
        statuses = listOf(OrderModelStatus.COMPLETED),
      )
    val revenueMap = monthlyRevenueStats.associateBy { it.date }

    // 构建月份开始日期到统计的映射
    val statsMap = monthlyStats.associateBy { it.date }

    // 遍历月份范围，填充缺失月份（无订单的月份）
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentMonth = startDate.withDayOfMonth(1)

    while (!currentMonth.isAfter(endDate)) {
      val stat = statsMap[currentMonth]
      result.add(
        stat?.copy(totalAmountCents = revenueMap[currentMonth]?.totalAmountCents ?: 0L)
          ?: DailyStatisticsDTO.default(currentMonth),
      )
      currentMonth = currentMonth.plusMonths(1)
    }

    return result
  }

  /**
   * 获取待处理订单存量快照（待支付/待派单/待配送三态合计，当前状态，与日期窗口无关）
   */
  override fun getPendingOrderBreakdown(): PendingOrderBreakdownDTO {
    val counts =
      statisticsOrderRepository.countByStatusIn(
        listOf(
          OrderModelStatus.PENDING_PAYMENT,
          OrderModelStatus.PENDING_DISPATCH,
          OrderModelStatus.PENDING_DELIVERY,
        ),
      )
    return PendingOrderBreakdownDTO(
      pendingPayment = counts[OrderModelStatus.PENDING_PAYMENT] ?: 0L,
      pendingDispatch = counts[OrderModelStatus.PENDING_DISPATCH] ?: 0L,
      pendingDelivery = counts[OrderModelStatus.PENDING_DELIVERY] ?: 0L,
    )
  }

  /**
   * 获取日期窗口内订单状态分布（G37，7 态补零，合计 = 订单总数 totalOrders）
   */
  override fun getOrderStatusDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<OrderStatusDistributionItemDTO> {
    val counts =
      statisticsOrderRepository.countOrdersGroupByStatus(
        startDateTime = startDate.atStartOfDay(),
        endDateTime = endDate.atTime(LocalTime.MAX),
      )
    return OrderModelStatus.entries.map { status ->
      OrderStatusDistributionItemDTO(status = status, count = counts[status] ?: 0L)
    }
  }

  /**
   * 获取日期窗口内容单价分布（G37，默认金额区间补零，合计 = 订单总数 totalOrders）
   * 金额按订单 amountCents 只计一次（与 P1-3 口径一致）。
   */
  override fun getOrderValueDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<OrderValueBucketDTO> {
    val counts =
      statisticsOrderRepository.countOrdersByAmountBucket(
        startDateTime = startDate.atStartOfDay(),
        endDateTime = endDate.atTime(LocalTime.MAX),
        buckets = defaultOrderValueBuckets,
      )
    return defaultOrderValueBuckets.map { bucket ->
      bucket.copy(count = counts[bucket.label] ?: 0L)
    }
  }

  /**
   * 获取日期窗口内高峰时段分析（G37，0-23 时补零，合计 = 订单总数 totalOrders，按 createdAt 小时聚合）
   */
  override fun getPeakHourDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<PeakHourDTO> {
    val counts =
      statisticsOrderRepository.countOrdersGroupByHour(
        startDateTime = startDate.atStartOfDay(),
        endDateTime = endDate.atTime(LocalTime.MAX),
      )
    return (0..23).map { hour ->
      PeakHourDTO(hour = hour, count = counts[hour] ?: 0L)
    }
  }
}
