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
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

/**
 * 订单统计服务单元测试(PRD F-STA-002: 订单数/营收口径、缺失日期补零、周月聚合)
 */
class StatisticsServiceImplTest :
  FunSpec({
    var orderQueryApiService: OrderQueryApiService = mockk()
    var statisticsOrderRepository: StatisticsOrderRepositoryCustom = mockk()
    var service: StatisticsServiceImpl = mockk()

    beforeTest {
      orderQueryApiService = mockk(relaxed = true)
      statisticsOrderRepository = mockk(relaxed = true)
      service = StatisticsServiceImpl(orderQueryApiService, statisticsOrderRepository)
    }
    test("getOrderStatistics aggregates totals and completed") {
      every { orderQueryApiService.countByStatusAndCreatedAtBetween(any(), any(), any(), any()) } returnsMany listOf(10L, 6L)
      every { orderQueryApiService.sumAmountCentsByStatusAndCreatedAtBetween(any(), any(), any(), any()) } returnsMany listOf(5000L, 3000L)

      val result = service.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))

      result.totalOrders shouldBe 10L
      result.totalAmountCents shouldBe 5000L
      result.completedOrders shouldBe 6L
      result.completedAmountCents shouldBe 3000L
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
      // 返回 6/2(周一) 那一周的统计
      val weekStart = LocalDate.of(2025, 6, 2)
      val stat = DailyStatisticsDTO(weekStart, orderCount = 4L, orderProductCount = 4L, totalAmountCents = 800L)
      every { statisticsOrderRepository.getWeeklyOrderStatistics(any(), any(), any(), any()) } returns listOf(stat)

      val result = service.getWeeklyStatistics(start, end)

      // 6/1 所在 ISO 周从 5/26 周一开始; 遍历 5/26, 6/2, 6/9, 6/16 共 4 周
      result.size shouldBe 4
      result[0] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 5, 26))
      result[1] shouldBe stat
      result[2] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 6, 9))
      result[3] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 6, 16))
    }
    test("getMonthlyStatistics fills missing months with default") {
      val start = LocalDate.of(2025, 1, 15)
      val end = LocalDate.of(2025, 3, 20)
      val jan = DailyStatisticsDTO(LocalDate.of(2025, 1, 1), orderCount = 5L, orderProductCount = 5L, totalAmountCents = 1000L)
      val mar = DailyStatisticsDTO(LocalDate.of(2025, 3, 1), orderCount = 7L, orderProductCount = 7L, totalAmountCents = 1400L)
      every { statisticsOrderRepository.getMonthlyOrderStatistics(any(), any(), any(), any()) } returns listOf(jan, mar)

      val result = service.getMonthlyStatistics(start, end)

      result.size shouldBe 3
      result[0] shouldBe jan
      // 2 月无订单 → 默认值
      result[1] shouldBe DailyStatisticsDTO.default(LocalDate.of(2025, 2, 1))
      result[2] shouldBe mar
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
  })
