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
import dev.yidafu.aqua.analytics.statistics.dto.PendingOrderBreakdownDTO
import dev.yidafu.aqua.analytics.statistics.service.StatisticsService
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class DailyStatisticsQueryResolverTest {
  private lateinit var statisticsService: StatisticsService
  private lateinit var resolver: DailyStatisticsQueryResolver

  @BeforeEach
  fun setUp() {
    statisticsService = mockk(relaxed = true)
    resolver = DailyStatisticsQueryResolver(statisticsService)
  }

  @Test
  fun `dailyStatistics should map daily stats`() {
    // Given
    every { statisticsService.getDailyStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2)) } returns
      listOf(
        DailyStatisticsDTO(
          date = LocalDate.of(2025, 6, 1),
          orderCount = 10L,
          orderProductCount = 12L,
          totalAmountCents = 25000L,
        ),
      )
    val input =
      DateRangeInput(
        startDate = LocalDateTime.of(2025, 6, 1, 0, 0),
        endDate = LocalDateTime.of(2025, 6, 2, 0, 0),
      )

    // When
    val result = resolver.dailyStatistics(input)

    // Then
    assertEquals(1, result.size)
    assertEquals("2025-06-01", result[0].date)
    assertEquals(10, result[0].orderCount)
    assertEquals(12, result[0].orderProductCount)
    assertEquals(25000L, result[0].revenueCents)
    verify { statisticsService.getDailyStatistics(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2)) }
  }

  @Test
  fun `pendingOrderBreakdown should map snapshot`() {
    // Given
    every { statisticsService.getPendingOrderBreakdown() } returns
      PendingOrderBreakdownDTO(pendingPayment = 2L, pendingDispatch = 3L, pendingDelivery = 4L)

    // When
    val result = resolver.pendingOrderBreakdown()

    // Then
    assertEquals(2, result.pendingPayment)
    assertEquals(3, result.pendingDispatch)
    assertEquals(4, result.pendingDelivery)
    assertEquals(9, result.total)
  }
}
