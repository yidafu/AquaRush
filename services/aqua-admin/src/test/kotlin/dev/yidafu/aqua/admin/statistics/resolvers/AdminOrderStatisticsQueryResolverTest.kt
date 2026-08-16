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

package dev.yidafu.aqua.admin.statistics.resolvers

import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderStatusDistributionItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.analytics.statistics.dto.PeakHourDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 订单统计解析器单元测试（G42 接线 + G38 口径：营收 = 已完成订单金额，客单价 = 已完成营收 / 已完成订单数）
 */
class AdminOrderStatisticsQueryResolverTest :
  FunSpec({
    var statisticsService: StatisticsService = mockk()
    var resolver: AdminOrderStatisticsQueryResolver = mockk()

    beforeTest {
      statisticsService = mockk(relaxed = true)
      resolver = AdminOrderStatisticsQueryResolver(statisticsService)
    }
    val input =
      DateRangeInput(
        startDate = LocalDateTime.of(2025, 6, 1, 0, 0),
        endDate = LocalDateTime.of(2025, 6, 30, 23, 59, 59),
      )

    test("orderStatistics should map DTO with completed-only revenue") {
      // Given
      every {
        statisticsService.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        OrderStatisticsDTO(
          totalOrders = 10L,
          totalAmountCents = 5000L,
          completedOrders = 6L,
          completedAmountCents = 3000L,
        )

      // When
      val result = resolver.orderStatistics(input)

      // Then
      result.totalOrders shouldBe 10
      result.completedOrders shouldBe 6
      result.totalRevenueCents shouldBe 3000L
      result.averageOrderValueCents shouldBe 500L // 3000 / 6
      result.dateRange.startDate shouldBe input.startDate
      result.dateRange.endDate shouldBe input.endDate
      verify { statisticsService.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)) }
    }
    test("orderStatistics should default averageOrderValueCents to zero without completed orders") {
      // Given
      every {
        statisticsService.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        OrderStatisticsDTO(
          totalOrders = 3L,
          totalAmountCents = 9999L,
          completedOrders = 0L,
          completedAmountCents = 0L,
        )

      // When
      val result = resolver.orderStatistics(input)

      // Then
      result.totalOrders shouldBe 3
      result.completedOrders shouldBe 0
      result.totalRevenueCents shouldBe 0L
      result.averageOrderValueCents shouldBe 0L
    }
    test("orderStatistics should map G37 status/value/hour distributions") {
      // Given
      every {
        statisticsService.getOrderStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        OrderStatisticsDTO(
          totalOrders = 8L,
          totalAmountCents = 5000L,
          completedOrders = 6L,
          completedAmountCents = 3000L,
        )
      every {
        statisticsService.getOrderStatusDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          OrderStatusDistributionItemDTO(OrderModelStatus.COMPLETED, 6L),
          OrderStatusDistributionItemDTO(OrderModelStatus.CANCELLED, 2L),
        )
      every {
        statisticsService.getOrderValueDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          OrderValueBucketDTO(fromCents = 0L, toCents = 2000L, label = "0-20元", count = 3L),
          OrderValueBucketDTO(fromCents = 20000L, toCents = null, label = "200元以上", count = 1L),
        )
      every {
        statisticsService.getPeakHourDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          PeakHourDTO(hour = 9, count = 4L),
          PeakHourDTO(hour = 18, count = 6L),
        )

      // When
      val result = resolver.orderStatistics(input)

      // Then
      result.totalOrders shouldBe 8
      result.statusDistribution.size shouldBe 2
      result.statusDistribution[0].status.name shouldBe "COMPLETED"
      result.statusDistribution[0].count shouldBe 6
      result.statusDistribution[1].status.name shouldBe "CANCELLED"
      result.statusDistribution[1].count shouldBe 2
      result.orderValueDistribution.size shouldBe 2
      result.orderValueDistribution[0].fromCents shouldBe 0L
      result.orderValueDistribution[0].toCents shouldBe 2000L
      result.orderValueDistribution[0].label shouldBe "0-20元"
      result.orderValueDistribution[0].count shouldBe 3
      result.orderValueDistribution[1].fromCents shouldBe 20000L
      result.orderValueDistribution[1].toCents shouldBe null
      result.orderValueDistribution[1].count shouldBe 1
      result.peakHourDistribution.size shouldBe 2
      result.peakHourDistribution[0].hour shouldBe 9
      result.peakHourDistribution[0].count shouldBe 4
      result.peakHourDistribution[1].hour shouldBe 18
      result.peakHourDistribution[1].count shouldBe 6
      verify { statisticsService.getOrderStatusDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)) }
      verify { statisticsService.getOrderValueDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)) }
      verify { statisticsService.getPeakHourDistribution(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)) }
    }
    test("weeklyStatistics should map week buckets") {
      // Given
      every {
        statisticsService.getWeeklyStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          DailyStatisticsDTO(
            date = LocalDate.of(2025, 6, 2), // 周一
            orderCount = 4L,
            orderProductCount = 5L,
            totalAmountCents = 800L,
          ),
        )

      // When
      val result = resolver.weeklyStatistics(input)

      // Then
      result.size shouldBe 1
      result[0].weekNumber shouldBe 23 // ISO 周 23
      result[0].year shouldBe 2025
      result[0].startDate shouldBe "2025-06-02"
      result[0].endDate shouldBe "2025-06-08"
      result[0].orderCount shouldBe 4
      result[0].orderProductCount shouldBe 5
      result[0].revenueCents shouldBe 800L
    }
    test("monthlyStatistics should map month buckets") {
      // Given
      every {
        statisticsService.getMonthlyStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))
      } returns
        listOf(
          DailyStatisticsDTO(
            date = LocalDate.of(2025, 6, 1),
            orderCount = 7L,
            orderProductCount = 9L,
            totalAmountCents = 1400L,
          ),
        )

      // When
      val result = resolver.monthlyStatistics(input)

      // Then
      result.size shouldBe 1
      result[0].month shouldBe 6
      result[0].year shouldBe 2025
      result[0].monthName shouldBe "6月"
      result[0].orderCount shouldBe 7
      result[0].orderProductCount shouldBe 9
      result[0].revenueCents shouldBe 1400L
    }
  })
