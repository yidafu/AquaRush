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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductSearchInput
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdminProductQueryResolverTest {
  private lateinit var productService: ProductServiceImpl
  private lateinit var systemConfigQueryApiService: SystemConfigQueryApiService
  private lateinit var resolver: AdminProductQueryResolver

  private fun createSampleProduct(): ProductModel =
    ProductModel(
      id = 1L,
      name = "桶装水19L",
      price = 2100,
      originalPrice = null,
      depositPrice = null,
      coverImageUrl = "http://x/cover.png",
      imageGallery = null,
      specification = "19L",
      waterSource = "山泉水",
      mineralContent = null,
      stock = 100,
      salesVolume = 0,
      status = ProductModelStatus.ONLINE,
      sortOrder = 1,
      tags = null,
      detailContent = null,
      certificateImages = null,
      deliverySettings = null,
    )

  @BeforeEach
  fun setUp() {
    productService = mockk(relaxed = true)
    systemConfigQueryApiService = mockk(relaxed = true)
    resolver = AdminProductQueryResolver(productService, systemConfigQueryApiService)
  }

  @Test
  fun `stockWarningThresholds should read system config with defaults`() {
    // Given
    every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 15L
    every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.STOCK_WARNING_TIER_MID, 50L) } returns 60L

    // When
    val result = resolver.stockWarningThresholds()

    // Then
    assertEquals(15, result.lowThreshold)
    assertEquals(60, result.midThreshold)
  }

  @Test
  fun `productsPaginated should map page results`() {
    // Given
    every { productService.productsPaginated(any(), PageRequest.of(0, 20)) } returns PageImpl(listOf(createSampleProduct()))

    // When
    val result = resolver.productsPaginated(ProductSearchInput())

    // Then
    assertEquals(1, result.list.size)
    assertEquals("桶装水19L", result.list[0].name)
    verify { productService.productsPaginated(any(), PageRequest.of(0, 20)) }
  }

  @Test
  fun `product should map by id`() {
    // Given
    every { productService.findById(1L) } returns createSampleProduct()

    // When
    val result = resolver.product(1L)

    // Then
    assertNotNull(result)
    assertEquals(2100L, result.priceCents)
  }

  @Test
  fun `product should return null when missing`() {
    // Given
    every { productService.findById(999L) } returns null

    // When
    val result = resolver.product(999L)

    // Then
    assertNull(result)
  }

  @Test
  fun `activeProducts should search with keyword`() {
    // Given
    every { productService.productsPaginated(any(), PageRequest.of(1, 5)) } returns PageImpl(emptyList())

    // When
    val result = resolver.activeProducts("桶装水", 1, 5)

    // Then
    assertEquals(0, result.list.size)
    verify { productService.productsPaginated(any(), PageRequest.of(1, 5)) }
  }
}
