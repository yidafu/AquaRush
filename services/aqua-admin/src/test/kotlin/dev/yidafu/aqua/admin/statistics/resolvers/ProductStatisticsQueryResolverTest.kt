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

import dev.yidafu.aqua.analytics.statistics.service.impl.ProductStatisticsServiceImpl
import dev.yidafu.aqua.common.graphql.generated.DateRangeInput
import dev.yidafu.aqua.common.graphql.generated.ProductDailySales
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ProductStatisticsQueryResolverTest {
  private lateinit var productStatisticsService: ProductStatisticsServiceImpl
  private lateinit var resolver: ProductStatisticsQueryResolver

  @BeforeEach
  fun setUp() {
    productStatisticsService = mockk(relaxed = true)
    resolver = ProductStatisticsQueryResolver(productStatisticsService)
  }

  @Test
  fun `productStatistics should delegate to service`() {
    // Given
    val stats = mockk<ProductStatistics>(relaxed = true)
    every { stats.totalProducts } returns 12
    every { stats.onlineProducts } returns 10
    every { productStatisticsService.getProductStatistics() } returns stats

    // When
    val result = resolver.productStatistics()

    // Then
    assertEquals(12, result.totalProducts)
    assertEquals(10, result.onlineProducts)
    verify { productStatisticsService.getProductStatistics() }
  }

  @Test
  fun `productDailySales should query by date range`() {
    // Given
    every { productStatisticsService.getProductDailySales(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2)) } returns
      listOf(
        ProductDailySales(
          date = "2025-06-01",
          productId = 1L,
          productName = "桶装水19L",
          revenueCents = 4200L,
          salesVolume = 2,
        ),
      )
    val input =
      DateRangeInput(
        startDate = LocalDateTime.of(2025, 6, 1, 0, 0),
        endDate = LocalDateTime.of(2025, 6, 2, 0, 0),
      )

    // When
    val result = resolver.productDailySales(input)

    // Then
    assertEquals(1, result.size)
    assertEquals("桶装水19L", result[0].productName)
    verify { productStatisticsService.getProductDailySales(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2)) }
  }
}
