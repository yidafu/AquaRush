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
import dev.yidafu.aqua.common.graphql.generated.ProductDailySalesVo
import dev.yidafu.aqua.common.graphql.generated.ProductStatisticsVo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class ProductStatisticsQueryResolverTest :
  FunSpec({
    var productStatisticsService: ProductStatisticsServiceImpl = mockk()
    var resolver: ProductStatisticsQueryResolver = mockk()

    beforeTest {
      productStatisticsService = mockk(relaxed = true)
      resolver = ProductStatisticsQueryResolver(productStatisticsService)
    }
    test("productStatistics should delegate to service") {
      // Given
      val stats = mockk<ProductStatisticsVo>(relaxed = true)
      every { stats.totalProducts } returns 12
      every { stats.onlineProducts } returns 10
      every { productStatisticsService.getProductStatistics() } returns stats

      // When
      val result = resolver.productStatistics()

      // Then
      result.totalProducts shouldBe 12
      result.onlineProducts shouldBe 10
      verify { productStatisticsService.getProductStatistics() }
    }
    test("productDailySales should query by date range") {
      // Given
      every { productStatisticsService.getProductDailySales(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2)) } returns
        listOf(
          ProductDailySalesVo(
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
      result.size shouldBe 1
      result[0].productName shouldBe "桶装水19L"
      verify { productStatisticsService.getProductDailySales(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2)) }
    }
  })
