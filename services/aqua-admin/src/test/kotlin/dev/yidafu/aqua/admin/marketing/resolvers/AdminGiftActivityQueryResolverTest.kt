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

import dev.yidafu.aqua.api.service.marketing.GiftActivityQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.graphql.generated.GiftActivityListInput
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

class AdminGiftActivityQueryResolverTest {
  private lateinit var giftActivityQueryService: GiftActivityQueryService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var resolver: AdminGiftActivityQueryResolver

  @BeforeEach
  fun setUp() {
    giftActivityQueryService = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    resolver = AdminGiftActivityQueryResolver(giftActivityQueryService, productQueryApiService)
  }

  @Test
  fun `giftActivities should list with display status`() {
    // Given
    every { giftActivityQueryService.listActivities("赠", PageRequest.of(0, 20)) } returns
      PageImpl(
        listOf(
          GiftActivityModel(
            id = 10L,
            name = "买赠活动",
            giftProductId = 2L,
            giftQuantity = 1,
            validFrom = LocalDateTime.now().minusDays(1),
            validTo = LocalDateTime.now().plusDays(30),
          ),
        ),
      )

    // When
    val result = resolver.giftActivities(GiftActivityListInput(keyword = "赠"))

    // Then
    assertEquals(1, result.content.size)
    assertEquals(PromotionActivityDisplayStatus.ONGOING, result.content[0].currentStatus)
    verify { giftActivityQueryService.listActivities("赠", PageRequest.of(0, 20)) }
  }

  @Test
  fun `giftActivity should return null when missing`() {
    // Given
    every { giftActivityQueryService.getActivity(999L) } returns null

    // When
    val result = resolver.giftActivity(999L)

    // Then
    assertNull(result)
  }
}
