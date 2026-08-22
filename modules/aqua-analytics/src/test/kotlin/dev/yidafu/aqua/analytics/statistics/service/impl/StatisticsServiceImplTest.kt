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

import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyOrderFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyRefundFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsOrderRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

/**
 * 订单统计服务单元测试(PRD F-STA-002: 订单数/营收口径、缺失日期补零、周月聚合、退款/净营收;
 * 第 3 层两段合并:历史段读快照事实表,未覆盖日期/今日回退实时 SQL)
 */
class StatisticsServiceImplTest :
  FunSpec({
    var orderQueryApiService: OrderQueryApiService = mockk()
    var paymentQueryService: PaymentQueryService = mockk()
    var statisticsOrderRepository: StatisticsOrderRepositoryCustom = mockk()
    var dailyOrderFactRepository: DailyOrderFactRepository = mockk()
    var dailyRefundFactRepository: DailyRefundFactRepository = mockk()
    var dailyUserFactRepository: DailyUserFactRepository = mockk()
    var snapshotCoverageService: StatisticsSnapshotCoverageService = mockk()
    var service: StatisticsServiceImpl = mockk()

    beforeTest {
      orderQueryApiService = mockk(relaxed = true)
      paymentQueryService = mockk(relaxed = true)
      statisticsOrderRepository = mockk(relaxed = true)
      dailyOrderFactRepository = mockk(relaxed = true)
      dailyRefundFactRepository = mockk(relaxed = true)
      dailyUserFactRepository = mockk(relaxed = true)
      // 真实覆盖判定服务 + 空用户事实表 → coveredDates 恒空 → 全部走实时 SQL(快照缺失回退)
      snapshotCoverageService = StatisticsSnapshotCoverageService(dailyUserFactRepository)
      service =
        StatisticsServiceImpl(
          orderQueryApiService,
          paymentQueryService,
          statisticsOrderRepository,
          dailyOrderFactRepository,
          dailyRefundFactRepository,
          snapshotCoverageService,
        )
    }
    test("getOrderStatistics aggregates totals and completed") {
      every { orderQueryApiService.countByStatusAndCreatedAtBetween(any(), any(), any(), any()) } returnsMany listOf(10L, 6L)
      every { orderQueryApiService.sumAmountCentsByStatusAndCreatedAtBetween(any(), any(), any(), any()) } returnsMany listOf(5000L, 3000L)
      every { paymentQueryService.sumRefundedAmountCents(any(), any()) } returns 500L

      val result = service.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.totalOrders shouldBe 10L
      result.totalAmountCents shouldBe 5000L
      result.completedOrders shouldBe 6L
      result.completedAmountCents shouldBe 3000L
      // 退款/净营收(F-STA-002: 净营收 = 已完成营收 − 已退款金额)
      result.refundedAmountCents shouldBe 500L
      result.netRevenueCents shouldBe 2500L
    }
    test("getOrderStatistics refund defaults to zero when no refund records") {
      every { orderQueryApiService.countByStatusAndCreatedAtBetween(any(), any(), any(), any()) } returnsMany listOf(1L, 1L)
      every { orderQueryApiService.sumAmountCentsByStatusAndCreatedAtBetween(any(), any(), any(), any()) } returnsMany listOf(100L, 100L)
      every { paymentQueryService.sumRefundedAmountCents(any(), any()) } returns 0L

      val result = service.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.refundedAmountCents shouldBe 0L
      result.netRevenueCents shouldBe 100L
    }
    test("getOrderStatistics reads historical segment from order/refund facts (两段合并)") {
      // 快照覆盖 2025-06-01..06-30:全部历史段读事实表,不触发实时 SQL
      every { dailyUserFactRepository.findFirstByOrderByStatDateAsc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 1))
      every { dailyUserFactRepository.findFirstByOrderByStatDateDesc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 30))
      every {
        dailyOrderFactRepository.findByStatDateBetween(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          DailyOrderFactModel(
            statDate = LocalDate.of(2025, 6, 10),
            status = OrderModelStatus.COMPLETED.name,
            orderCount = 4L,
            amountCents = 2000L,
            itemQuantity = 6L,
          ),
          DailyOrderFactModel(
            statDate = LocalDate.of(2025, 6, 10),
            status = OrderModelStatus.PENDING_DISPATCH.name,
            orderCount = 2L,
            amountCents = 999L,
            itemQuantity = 2L,
          ),
        )
      every {
        dailyRefundFactRepository.findByStatDateBetween(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          DailyRefundFactModel(
            statDate = LocalDate.of(2025, 6, 11),
            status = RefundStatus.COMPLETED.name,
            refundType = RefundType.ORDER_REFUND.name,
            refundAmountCents = 300L,
          ),
          DailyRefundFactModel(
            statDate = LocalDate.of(2025, 6, 12),
            status = RefundStatus.COMPLETED.name,
            refundType = RefundType.DEPOSIT_REFUND.name,
            refundAmountCents = 999L,
          ),
          DailyRefundFactModel(
            statDate = LocalDate.of(2025, 6, 13),
            status = RefundStatus.PENDING.name,
            refundType = RefundType.ORDER_REFUND.name,
            refundAmountCents = 777L,
          ),
        )

      val result = service.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.totalOrders shouldBe 6L // 4 + 2 全状态
      result.totalAmountCents shouldBe 2999L // 2000 + 999
      result.completedOrders shouldBe 4L
      result.completedAmountCents shouldBe 2000L
      // 仅 COMPLETED + ORDER_REFUND 计入(排除 DEPOSIT_REFUND 与 PENDING,R-PAY-02/G06)
      result.refundedAmountCents shouldBe 300L
    }
    test("getDailyStatistics fills missing dates with zero") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 3)
      val stat = DailyStatisticsDTO(start, orderCount = 3L, orderProductCount = 5L, totalAmountCents = 1200L)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(
          listOf(stat),
          listOf(stat), // revenue pass (COMPLETED only)
        )

      val result = service.getDailyStatistics(start, end)

      result.size shouldBe 3
      result[0].orderCount shouldBe 3L
      result[0].totalAmountCents shouldBe 1200L
      result[1] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 6, 2))
      result[2] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 6, 3))
    }
    test("getDailyStatistics reads covered historical dates from facts and keeps live for today") {
      // 快照覆盖 2025-06-01..06-29;查询 [6/1, 6/30] → 历史段事实表,今日(6/30)实时
      every { dailyUserFactRepository.findFirstByOrderByStatDateAsc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 1))
      every { dailyUserFactRepository.findFirstByOrderByStatDateDesc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 29))
      every {
        dailyOrderFactRepository.findByStatDateBetween(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 29))
      } returns
        listOf(
          DailyOrderFactModel(
            statDate = LocalDate.of(2025, 6, 10),
            status = OrderModelStatus.COMPLETED.name,
            orderCount = 3L,
            amountCents = 1500L,
            itemQuantity = 5L,
          ),
        )
      every { dailyRefundFactRepository.findByStatDateBetween(any(), any()) } returns emptyList()
      // 实时段(今日 6/30):6/30 当天 2 单完成 1 单
      val todayStat = DailyStatisticsDTO(LocalDate.of(2025, 6, 30), orderCount = 2L, orderProductCount = 2L, totalAmountCents = 888L)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(
          listOf(todayStat),
          listOf(todayStat), // revenue pass
        )

      val result = service.getDailyStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      // 6/10 快照值:全状态 3 单,已完成营收 1500
      result.first { it.date == LocalDate.of(2025, 6, 10) }.orderCount shouldBe 3L
      result.first { it.date == LocalDate.of(2025, 6, 10) }.totalAmountCents shouldBe 1500L
      // 6/30 实时值
      result.first { it.date == LocalDate.of(2025, 6, 30) }.orderCount shouldBe 2L
      // 其余日期补零
      result.first { it.date == LocalDate.of(2025, 6, 1) }.orderCount shouldBe 0L
    }
    test("getDailyStatistics uses revenue from completed-only aggregation") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 1)
      val allStats = DailyStatisticsDTO(start, orderCount = 8L, orderProductCount = 10L, totalAmountCents = 9999L)
      val completedStats = DailyStatisticsDTO(start, orderCount = 2L, orderProductCount = 2L, totalAmountCents = 700L)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(
          listOf(allStats),
          listOf(completedStats),
        )

      val result = service.getDailyStatistics(start, end)

      result.size shouldBe 1
      // 订单数来自全量口径, 营收来自已完成口径
      result[0].orderCount shouldBe 8L
      result[0].totalAmountCents shouldBe 700L
    }
    test("getWeeklyStatistics fills missing weeks with default") {
      val start = LocalDate.of(2025, 6, 1) // 周日
      val end = LocalDate.of(2025, 6, 22)
      // 6/2(周一) 当周有 4 单/5 件/已完成营收 800(已完成口径在日序列上已组合)
      val stat = DailyStatisticsDTO(LocalDate.of(2025, 6, 2), orderCount = 4L, orderProductCount = 5L, totalAmountCents = 9999L)
      val completedStat = DailyStatisticsDTO(LocalDate.of(2025, 6, 2), orderCount = 4L, orderProductCount = 5L, totalAmountCents = 800L)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(
          listOf(stat),
          listOf(completedStat), // revenue pass (COMPLETED only)
        )

      val result = service.getWeeklyStatistics(start, end)

      // 6/1 所在 ISO 周从 5/26 周一开始; 遍历 5/26, 6/2, 6/9, 6/16 共 4 周
      result.size shouldBe 4
      result[0] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 5, 26))
      result[1].orderCount shouldBe 4L
      result[1].orderProductCount shouldBe 5L
      result[1].totalAmountCents shouldBe 800L
      result[2] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 6, 9))
      result[3] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 6, 16))
    }
    test("getMonthlyStatistics fills missing months with default") {
      val start = LocalDate.of(2025, 1, 15)
      val end = LocalDate.of(2025, 3, 20)
      val jan = DailyStatisticsDTO(LocalDate.of(2025, 1, 20), orderCount = 5L, orderProductCount = 5L, totalAmountCents = 1000L)
      val mar = DailyStatisticsDTO(LocalDate.of(2025, 3, 5), orderCount = 7L, orderProductCount = 7L, totalAmountCents = 1400L)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(
          listOf(jan, mar),
          listOf(jan, mar), // revenue pass
        )

      val result = service.getMonthlyStatistics(start, end)

      result.size shouldBe 3
      result[0].date shouldBe LocalDate.of(2025, 1, 1)
      result[0].orderCount shouldBe 5L
      result[0].totalAmountCents shouldBe 1000L
      // 2 月无订单 → 默认值
      result[1] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 2, 1))
      result[2].date shouldBe LocalDate.of(2025, 3, 1)
      result[2].orderCount shouldBe 7L
      result[2].totalAmountCents shouldBe 1400L
    }
    test("getDailyStatistics merges refund amounts per day (F-STA-002)") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 3)
      val stat = DailyStatisticsDTO(start, orderCount = 3L, orderProductCount = 5L, totalAmountCents = 1200L)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(
          listOf(stat),
          listOf(stat),
        )
      every { paymentQueryService.getDailyRefundedAmountCents(any(), any()) } returns
        mapOf(start to 200L, LocalDate.of(2025, 6, 2) to 100L)

      val result = service.getDailyStatistics(start, end)

      result.size shouldBe 3
      result[0].refundedAmountCents shouldBe 200L
      result[1].refundedAmountCents shouldBe 100L
      result[2].refundedAmountCents shouldBe 0L
    }
    test("getWeeklyStatistics rolls up daily refunds into week buckets (F-STA-002)") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 22)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(emptyList(), emptyList())
      // 6/3+6/5 同属 6/2(周一)周 → 300;6/10 属 6/9 周 → 50
      every { paymentQueryService.getDailyRefundedAmountCents(any(), any()) } returns
        mapOf(
          LocalDate.of(2025, 6, 3) to 200L,
          LocalDate.of(2025, 6, 5) to 100L,
          LocalDate.of(2025, 6, 10) to 50L,
        )

      val result = service.getWeeklyStatistics(start, end)

      result.size shouldBe 4
      result[0].refundedAmountCents shouldBe 0L // 5/26 周
      result[1].refundedAmountCents shouldBe 300L // 6/2 周
      result[2].refundedAmountCents shouldBe 50L // 6/9 周
      result[3].refundedAmountCents shouldBe 0L // 6/16 周
    }
    test("getMonthlyStatistics rolls up daily refunds into month buckets (F-STA-002)") {
      val start = LocalDate.of(2025, 1, 15)
      val end = LocalDate.of(2025, 3, 20)
      every { statisticsOrderRepository.getDailyOrderStatistics(any(), any(), any(), any()) } returnsMany
        listOf(emptyList(), emptyList())
      every { paymentQueryService.getDailyRefundedAmountCents(any(), any()) } returns
        mapOf(
          LocalDate.of(2025, 1, 20) to 100L,
          LocalDate.of(2025, 2, 10) to 60L,
          LocalDate.of(2025, 3, 5) to 40L,
        )

      val result = service.getMonthlyStatistics(start, end)

      result.size shouldBe 3
      result[0].refundedAmountCents shouldBe 100L // 1 月
      result[1].refundedAmountCents shouldBe 60L // 2 月无订单也有退款
      result[2].refundedAmountCents shouldBe 40L // 3 月
    }
    test("getPendingOrderBreakdown maps counts by status") {
      every {
        statisticsOrderRepository.countByStatusIn(any())
      } returns
        mapOf(
          OrderModelStatus.PENDING_PAYMENT to 3L,
          OrderModelStatus.PENDING_DISPATCH to 2L,
          OrderModelStatus.PENDING_DELIVERY to 1L,
        )

      val result = service.getPendingOrderBreakdown()

      result.pendingPayment shouldBe 3L
      result.pendingDispatch shouldBe 2L
      result.pendingDelivery shouldBe 1L
      result.total shouldBe 6L
    }
    test("getPendingOrderBreakdown defaults to zero for missing statuses") {
      every { statisticsOrderRepository.countByStatusIn(any()) } returns emptyMap()

      val result = service.getPendingOrderBreakdown()

      result.pendingPayment shouldBe 0L
      result.pendingDispatch shouldBe 0L
      result.pendingDelivery shouldBe 0L
    }
    test("getOrderStatusDistribution zero-fills all seven statuses (G37)") {
      every { statisticsOrderRepository.countOrdersGroupByStatus(any(), any()) } returns
        mapOf(
          OrderModelStatus.COMPLETED to 5L,
          OrderModelStatus.CANCELLED to 2L,
        )

      val result = service.getOrderStatusDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.size shouldBe 7
      result.first { it.status == OrderModelStatus.COMPLETED }.count shouldBe 5L
      result.first { it.status == OrderModelStatus.CANCELLED }.count shouldBe 2L
      result.filter { it.count > 0L }.sumOf { it.count } shouldBe 7L
      // 未出现的状态补零
      result.first { it.status == OrderModelStatus.PENDING_PAYMENT }.count shouldBe 0L
      result.first { it.status == OrderModelStatus.REFUNDED }.count shouldBe 0L
    }
    test("getOrderStatusDistribution reads historical segment from facts (G37 状态分布快照化)") {
      every { dailyUserFactRepository.findFirstByOrderByStatDateAsc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 1))
      every { dailyUserFactRepository.findFirstByOrderByStatDateDesc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 30))
      every {
        dailyOrderFactRepository.findByStatDateBetween(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          DailyOrderFactModel(
            statDate = LocalDate.of(2025, 6, 10),
            status = OrderModelStatus.COMPLETED.name,
            orderCount = 5L,
            amountCents = 1L,
            itemQuantity = 1L,
          ),
          DailyOrderFactModel(
            statDate = LocalDate.of(2025, 6, 10),
            status = OrderModelStatus.CANCELLED.name,
            orderCount = 2L,
            amountCents = 1L,
            itemQuantity = 1L,
          ),
        )
      // 快照已覆盖全部历史 → 实时 countOrdersGroupByStatus 不应被调用(不 stub,relaxed 返回空 map)

      val result = service.getOrderStatusDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.first { it.status == OrderModelStatus.COMPLETED }.count shouldBe 5L
      result.first { it.status == OrderModelStatus.CANCELLED }.count shouldBe 2L
      result.filter { it.count > 0L }.sumOf { it.count } shouldBe 7L
    }
    test("getOrderValueDistribution zero-fills default buckets (G37)") {
      every { statisticsOrderRepository.countOrdersByAmountBucket(any(), any(), any()) } returns
        mapOf(
          "0-20元" to 3L,
          "200元以上" to 1L,
        )

      val result = service.getOrderValueDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.size shouldBe 5
      result.map { it.label } shouldBe listOf("0-20元", "20-50元", "50-100元", "100-200元", "200元以上")
      result.first { it.label == "0-20元" }.count shouldBe 3L
      result.first { it.label == "200元以上" }.count shouldBe 1L
      // 未出现的区间补零
      result.first { it.label == "20-50元" }.count shouldBe 0L
      result.first { it.label == "50-100元" }.count shouldBe 0L
    }
    test("getPeakHourDistribution zero-fills all 24 hours (G37)") {
      every { statisticsOrderRepository.countOrdersGroupByHour(any(), any()) } returns
        mapOf(9 to 4L, 18 to 6L)

      val result = service.getPeakHourDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.size shouldBe 24
      result[0].hour shouldBe 0
      result[9].count shouldBe 4L
      result[18].count shouldBe 6L
      result[23].hour shouldBe 23
      result.filter { it.count > 0L }.sumOf { it.count } shouldBe 10L
      result[3].count shouldBe 0L
    }
  })
