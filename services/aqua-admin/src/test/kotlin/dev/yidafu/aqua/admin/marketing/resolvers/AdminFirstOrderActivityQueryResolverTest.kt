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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityListInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminFirstOrderActivityQueryResolverTest {
  private lateinit var firstOrderActivityQueryService: FirstOrderActivityQueryService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var resolver: AdminFirstOrderActivityQueryResolver

  @BeforeEach
  fun setUp() {
    firstOrderActivityQueryService = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    resolver = AdminFirstOrderActivityQueryResolver(firstOrderActivityQueryService, productQueryApiService)
  }

  @Test
  fun `firstOrderActivities should list with display status`() {
    // Given
    every { firstOrderActivityQueryService.listActivities("首单", PageRequest.of(0, 20)) } returns
      PageImpl(
        listOf(
          FirstOrderActivityModel(
            id = 10L,
            name = "首单立减10元",
            validFrom = LocalDateTime.now().minusDays(1),
            validTo = LocalDateTime.now().plusDays(30),
          ),
        ),
      )

    // When
    val result = resolver.firstOrderActivities(FirstOrderActivityListInput(keyword = "首单"))

    // Then
    assertEquals(1, result.content.size)
    assertEquals(PromotionActivityDisplayStatus.ONGOING, result.content[0].currentStatus)
    verify { firstOrderActivityQueryService.listActivities("首单", PageRequest.of(0, 20)) }
  }

  @Test
  fun `firstOrderActivity should return null when missing`() {
    // Given
    every { firstOrderActivityQueryService.getActivity(999L) } returns null

    // When
    val result = resolver.firstOrderActivity(999L)

    // Then
    assertNull(result)
  }
}
