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

import dev.yidafu.aqua.analytics.statistics.StatisticsTime
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyOrderFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRefundFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRevenueFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsOrderRepository
import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatusDistributionItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.analytics.statistics.dto.PeakHourDTO
import dev.yidafu.aqua.analytics.statistics.dto.PendingOrderBreakdownDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields

@Service
class StatisticsServiceImpl(
  private val orderQueryApiService: OrderQueryApiService,
  private val paymentQueryService: PaymentQueryService,
  private val statisticsOrderRepository: StatisticsOrderRepository,
  private val dailyOrderFactRepository: DailyOrderFactRepository,
  private val dailyRefundFactRepository: DailyRefundFactRepository,
  private val dailyRevenueFactRepository: DailyRevenueFactRepository,
  private val snapshotCoverageService: StatisticsSnapshotCoverageService,
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
   * 两段合并:昨日及以前读快照事实表(口径查询时组合),今日走实时 SQL。
   * 订单数口径:按下单日(created_at)归属窗口;营收/已完成口径:按完成日(completed_at)归属窗口。
   */
  override fun getOrderStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): OrderStatisticsDTO {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)

    // 历史段:快照事实表(有覆盖则读快照,否则回退实时)
    val factDates = snapshotCoverageService.coveredDates(startDate, minOf(endDate, yesterday))
    val factRanges = contiguousRanges(factDates)

    var totalOrders = 0L
    var totalAmountCents = 0L
    var completedOrders = 0L
    var completedAmountCents = 0L
    var refundedAmountCents = 0L

    factRanges.forEach { (rangeStart, rangeEnd) ->
      val orderFacts = dailyOrderFactRepository.findByStatDateBetween(rangeStart, rangeEnd)
      val revenueFacts = dailyRevenueFactRepository.findByStatDateBetween(rangeStart, rangeEnd)
      val refundFacts = dailyRefundFactRepository.findByStatDateBetween(rangeStart, rangeEnd)
      totalOrders += orderFacts.sumOf { it.orderCount }
      totalAmountCents += orderFacts.sumOf { it.amountCents }
      // 已完成口径(营收/已完成订单数)按完成日归属:读营收事实表(completed_at 日),而非订单事实表的
      // COMPLETED 行(按下单日归属)——跨日完成订单不再既漏下单日、又漏完成日
      completedOrders += revenueFacts.sumOf { it.orderCount }
      completedAmountCents += revenueFacts.sumOf { it.amountCents }
      refundedAmountCents +=
        refundFacts
          .filter { it.status == RefundStatus.COMPLETED.name && it.refundType == RefundType.ORDER_REFUND.name }
          .sumOf { it.refundAmountCents }
    }

    // 未覆盖历史日期 + 今日:实时 SQL 聚合
    val liveRanges = snapshotCoverageService.uncoveredRanges(startDate, endDate, factDates)
    liveRanges.forEach { (rangeStart, rangeEnd) ->
      val startDateTime = rangeStart.atStartOfDay()
      val endDateTime = rangeEnd.atTime(LocalTime.MAX)
      totalOrders +=
        orderQueryApiService.countByStatusAndCreatedAtBetween(
          startDateTime,
          endDateTime,
          deliveryWorkerId = null,
          statuses = null,
        )
      totalAmountCents +=
        orderQueryApiService.sumAmountCentsByStatusAndCreatedAtBetween(
          OrderModelStatus.entries,
          startDateTime,
          endDateTime,
          deliveryWorkerId = null,
        )
      // 已完成口径按完成时间归属(completed_at 窗口,与快照段营收事实表同口径)
      completedOrders +=
        orderQueryApiService.countByStatusAndCompletedAtBetween(
          startDateTime,
          endDateTime,
          deliveryWorkerId = null,
          statuses = listOf(OrderModelStatus.COMPLETED),
        )
      completedAmountCents +=
        orderQueryApiService.sumAmountCentsByStatusAndCompletedAtBetween(
          listOf(OrderModelStatus.COMPLETED),
          startDateTime,
          endDateTime,
          deliveryWorkerId = null,
        )
      refundedAmountCents += paymentQueryService.sumRefundedAmountCents(startDateTime, endDateTime)
    }

    return OrderStatisticsDTO(
      totalOrders = totalOrders,
      totalAmountCents = totalAmountCents,
      completedOrders = completedOrders,
      completedAmountCents = completedAmountCents,
      refundedAmountCents = refundedAmountCents,
    )
  }

  /**
   * 获取每日订单统计
   * 订单数口径:当日全部订单(按下单日);营收口径:已完成订单金额合计,按完成日归属(PRD F-STA-002 定稿)
   * 两段合并:昨日及以前读快照(营收读 daily_revenue_facts 按 completed_at 日,退款按 processed_at 归属在查询时组合),
   * 今日走实时 SQL;缺失快照日期回退实时。
   */
  override fun getDailyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO> {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)

    val historicalEnd = minOf(endDate, yesterday)
    val factDates = if (startDate <= historicalEnd) snapshotCoverageService.coveredDates(startDate, historicalEnd) else emptySet()
    val statsMap = mutableMapOf<LocalDate, DailyStatisticsDTO>()

    // 快照段:每日期 orderCount=Σ 全状态(按下单日),itemQuantity=Σ 件数,
    // revenue=Σ 营收事实表金额(按 completed_at 完成日,跨日完成订单计入完成日),refund=Σ COMPLETED+ORDER_REFUND
    if (factDates.isNotEmpty()) {
      val (factStart, factEnd) = factDates.min() to factDates.max()
      val orderFacts = dailyOrderFactRepository.findByStatDateBetween(factStart, factEnd)
      val revenueFacts = dailyRevenueFactRepository.findByStatDateBetween(factStart, factEnd)
      val refundFacts = dailyRefundFactRepository.findByStatDateBetween(factStart, factEnd)
      val orderByDate = orderFacts.groupBy { it.statDate }
      val revenueByDate = revenueFacts.groupBy { it.statDate }
      val refundByDate = refundFacts.groupBy { it.statDate }
      factDates.forEach { date ->
        val orders = orderByDate[date].orEmpty()
        val revenue = revenueByDate[date].orEmpty()
        val refunds = refundByDate[date].orEmpty()
        statsMap[date] =
          DailyStatisticsDTO(
            date = date,
            orderCount = orders.sumOf { it.orderCount },
            orderProductCount = orders.sumOf { it.itemQuantity },
            totalAmountCents = revenue.sumOf { it.amountCents },
            refundedAmountCents =
              refunds
                .filter { it.status == RefundStatus.COMPLETED.name && it.refundType == RefundType.ORDER_REFUND.name }
                .sumOf { it.refundAmountCents },
          )
      }
    }

    // 实时段:未覆盖历史日期 + 今日
    val liveRanges = snapshotCoverageService.uncoveredRanges(startDate, endDate, factDates)
    liveRanges.forEach { (rangeStart, rangeEnd) ->
      liveDailyStatistics(rangeStart, rangeEnd).forEach { statsMap[it.date] = it }
    }

    // 遍历日期范围,填充缺失日期(无订单的日期)
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      result.add(statsMap[currentDate] ?: DailyStatisticsDTO.default(currentDate))
      currentDate = currentDate.plusDays(1)
    }
    return result
  }

  /**
   * 获取每周订单统计
   * 周聚合在查询时组合:对两段合并后的日序列按 ISO 周(周一)滚动聚合(PRD F-STA-002 口径)。
   */
  override fun getWeeklyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO> {
    val daily = getDailyStatistics(startDate, endDate)
    val weekFields = WeekFields.of(DayOfWeek.MONDAY, 4)
    val weeklyMap = daily.groupBy { it.date.with(weekFields.dayOfWeek(), 1) }

    val result = mutableListOf<DailyStatisticsDTO>()
    var currentWeekStart = startDate.with(weekFields.dayOfWeek(), 1)
    while (!currentWeekStart.isAfter(endDate)) {
      val weekStats = weeklyMap[currentWeekStart]
      result.add(
        weekStats?.reduce { acc, dto -> acc.sumWith(dto) }?.copy(date = currentWeekStart)
          ?: DailyStatisticsDTO.default(currentWeekStart),
      )
      currentWeekStart = currentWeekStart.plusWeeks(1)
    }
    return result
  }

  /**
   * 获取每月订单统计
   * 月聚合在查询时组合:对两段合并后的日序列按自然月滚动聚合。
   */
  override fun getMonthlyStatistics(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyStatisticsDTO> {
    val daily = getDailyStatistics(startDate, endDate)
    val monthlyMap = daily.groupBy { it.date.withDayOfMonth(1) }

    val result = mutableListOf<DailyStatisticsDTO>()
    var currentMonth = startDate.withDayOfMonth(1)
    while (!currentMonth.isAfter(endDate)) {
      val monthStats = monthlyMap[currentMonth]
      result.add(
        monthStats?.reduce { acc, dto -> acc.sumWith(dto) }?.copy(date = currentMonth)
          ?: DailyStatisticsDTO.default(currentMonth),
      )
      currentMonth = currentMonth.plusMonths(1)
    }
    return result
  }

  /**
   * 获取待处理订单存量快照(待支付/待派单/待配送三态合计,当前状态,与日期窗口无关)
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
   * 获取日期窗口内订单状态分布(G37,7 态补零,合计 = 订单总数)
   * 快照化:历史段由订单事实表按 status 分组求和,未覆盖日期与今日走实时 SQL。
   */
  override fun getOrderStatusDistribution(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<OrderStatusDistributionItemDTO> {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)
    val factDates =
      if (startDate <=
        minOf(endDate, yesterday)
      ) {
        snapshotCoverageService.coveredDates(startDate, minOf(endDate, yesterday))
      } else {
        emptySet()
      }

    val counts = mutableMapOf<OrderModelStatus, Long>()

    if (factDates.isNotEmpty()) {
      val (factStart, factEnd) = factDates.min() to factDates.max()
      dailyOrderFactRepository
        .findByStatDateBetween(factStart, factEnd)
        .forEach { fact ->
          val status = OrderModelStatus.fromValue(fact.status)
          counts[status] = (counts[status] ?: 0L) + fact.orderCount
        }
    }

    snapshotCoverageService.uncoveredRanges(startDate, endDate, factDates).forEach { (rangeStart, rangeEnd) ->
      statisticsOrderRepository
        .countOrdersGroupByStatus(rangeStart.atStartOfDay(), rangeEnd.atTime(LocalTime.MAX))
        .forEach { (status, count) -> counts[status] = (counts[status] ?: 0L) + count }
    }

    return OrderModelStatus.entries.map { status ->
      OrderStatusDistributionItemDTO(status = status, count = counts[status] ?: 0L)
    }
  }

  /**
   * 获取日期窗口内容单价分布(G37,默认金额区间补零,合计 = 订单总数)
   * 原子日汇总无法还原单笔订单金额,保持实时 SQL 聚合(窗口内订单量级可控)。
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
   * 获取日期窗口内高峰时段分析(G37,0-23 时补零,合计 = 订单总数,按 createdAt 小时聚合)
   * 原子日汇总无小时维度,保持实时 SQL 聚合。
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

  /**
   * 实时聚合 [startDate, endDate] 的每日统计(订单数全状态按下单日 + 营收已完成口径按完成日 + 退款按日)。
   */
  private fun liveDailyStatistics(
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
    // 营收按完成时间归属日:跨日完成订单(created_at 在窗口外、completed_at 在窗口内)计入完成日
    val dailyRevenueStats = statisticsOrderRepository.getDailyRevenueStatistics(startDateTime, endDateTime)
    val revenueMap = dailyRevenueStats.associateBy { it.date }
    val refundMap = paymentQueryService.getDailyRefundedAmountCents(startDateTime, endDateTime)

    val statsMap = dailyStats.associateBy { it.date }
    val result = mutableListOf<DailyStatisticsDTO>()
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
      val stat = statsMap[currentDate]
      // 营收来自独立的完成日聚合:即使当天无新下单(订单数=0),有跨日完成订单也如实展示营收
      result.add(
        DailyStatisticsDTO(
          date = currentDate,
          orderCount = stat?.orderCount ?: 0L,
          orderProductCount = stat?.orderProductCount ?: 0L,
          totalAmountCents = revenueMap[currentDate]?.totalAmountCents ?: 0L,
          refundedAmountCents = 0L,
        ),
      )
      currentDate = currentDate.plusDays(1)
    }
    return result.map { it.copy(refundedAmountCents = refundMap[it.date] ?: 0L) }
  }

  /**
   * 将日期集合折叠为连续区间列表(供快照段批量读取)。
   */
  private fun contiguousRanges(dates: Set<LocalDate>): List<Pair<LocalDate, LocalDate>> {
    if (dates.isEmpty()) return emptyList()
    val sorted = dates.sorted()
    val ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var rangeStart = sorted.first()
    var rangeEnd = sorted.first()
    for (date in sorted.drop(1)) {
      if (date == rangeEnd.plusDays(1)) {
        rangeEnd = date
      } else {
        ranges.add(rangeStart to rangeEnd)
        rangeStart = date
        rangeEnd = date
      }
    }
    ranges.add(rangeStart to rangeEnd)
    return ranges
  }

  /**
   * 日统计滚动聚合(周/月):四口径相加,date 取聚合周期起点。
   */
  private fun DailyStatisticsDTO.sumWith(other: DailyStatisticsDTO): DailyStatisticsDTO =
    DailyStatisticsDTO(
      date = date,
      orderCount = orderCount + other.orderCount,
      orderProductCount = orderProductCount + other.orderProductCount,
      totalAmountCents = totalAmountCents + other.totalAmountCents,
      refundedAmountCents = refundedAmountCents + other.refundedAmountCents,
    )
}
