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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class AdminProductQueryResolverTest :
  FunSpec({
    var productService: ProductServiceImpl = mockk()
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var resolver: AdminProductQueryResolver = mockk()

    fun createSampleProduct(): ProductModel =
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
      )

    beforeTest {
      productService = mockk(relaxed = true)
      systemConfigQueryApiService = mockk(relaxed = true)
      resolver = AdminProductQueryResolver(productService, systemConfigQueryApiService)
    }
    test("stockWarningThresholds should read system config with defaults") {
      // Given
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 15L
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.STOCK_WARNING_TIER_MID, 50L) } returns 60L

      // When
      val result = resolver.stockWarningThresholds()

      // Then
      result.lowThreshold shouldBe 15
      result.midThreshold shouldBe 60
    }
    test("productsPaginated should map page results") {
      // Given
      every { productService.productsPaginated(any(), PageRequest.of(0, 20)) } returns PageImpl(listOf(createSampleProduct()))

      // When
      val result = resolver.productsPaginated(ProductSearchInput())

      // Then
      result.list.size shouldBe 1
      result.list[0].name shouldBe "桶装水19L"
      verify { productService.productsPaginated(any(), PageRequest.of(0, 20)) }
    }
    test("product should map by id") {
      // Given
      every { productService.findById(1L) } returns createSampleProduct()

      // When
      val result = resolver.product(1L)

      // Then
      result.shouldNotBeNull()
      result.priceCents shouldBe 2100L
    }
    test("product should return null when missing") {
      // Given
      every { productService.findById(999L) } returns null

      // When
      val result = resolver.product(999L)

      // Then
      result.shouldBeNull()
    }
    test("activeProducts should search with keyword and only return ONLINE products with stock") {
      // Given
      val page = PageImpl(listOf(createSampleProduct()), PageRequest.of(1, 5), 1)
      every {
        productService.productsPaginated(
          match {
            it.keyword == "桶装水" &&
              it.status == ProductModelStatus.ONLINE &&
              it.minStock == 0
          },
          PageRequest.of(1, 5),
        )
      } returns page

      // When
      val result = resolver.activeProducts("桶装水", 1, 5)

      // Then
      result.list.size shouldBe 1
      verify {
        productService.productsPaginated(
          match {
            it.keyword == "桶装水" &&
              it.status == ProductModelStatus.ONLINE &&
              it.minStock == 0
          },
          PageRequest.of(1, 5),
        )
      }
    }
  })
