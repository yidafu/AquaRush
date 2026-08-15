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

import dev.yidafu.aqua.api.service.marketing.GroupProductPriceQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.common.graphql.generated.GroupProductPriceListInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

class AdminGroupProductPriceQueryResolverTest {
  private lateinit var groupProductPriceQueryService: GroupProductPriceQueryService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var resolver: AdminGroupProductPriceQueryResolver

  @BeforeEach
  fun setUp() {
    groupProductPriceQueryService = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    resolver = AdminGroupProductPriceQueryResolver(groupProductPriceQueryService, productQueryApiService)
  }

  @Test
  fun `groupProductPrices should list with product info`() {
    // Given
    every { groupProductPriceQueryService.listByGroup(10L, PageRequest.of(0, 20)) } returns
      PageImpl(listOf(GroupProductPriceModel(id = 1L, customerGroupId = 10L, productId = 1L, priceCents = 1900)))
    every { productQueryApiService.findById(1L) } returns
      dev.yidafu.aqua.common.domain.model.ProductModel(
        id = 1L,
        name = "桶装水19L",
        price = 2100,
        coverImageUrl = "http://x/c.png",
        specification = "19L",
        stock = 100,
        salesVolume = 0,
        status = dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus.ONLINE,
      )

    // When
    val result = resolver.groupProductPrices(GroupProductPriceListInput(customerGroupId = 10L))

    // Then
    assertEquals(1, result.content.size)
    assertEquals(1900L, result.content[0].priceCents)
    verify { groupProductPriceQueryService.listByGroup(10L, PageRequest.of(0, 20)) }
  }
}
