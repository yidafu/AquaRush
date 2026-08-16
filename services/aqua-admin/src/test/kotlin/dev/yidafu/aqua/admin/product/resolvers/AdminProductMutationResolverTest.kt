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

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateProductInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.product.service.impl.ProductServiceImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AdminProductMutationResolverTest :
  FunSpec({
    var productService: ProductServiceImpl = mockk()
    var bizLogger: BizLogger = mockk()
    var resolver: AdminProductMutationResolver = mockk()

    fun createSampleProduct(status: ProductModelStatus = ProductModelStatus.ONLINE): ProductModel =
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
        status = status,
        sortOrder = 1,
        tags = null,
        detailContent = null,
        certificateImages = null,
        deliverySettings = null,
      )

    fun validCreateInput(): CreateProductInput =
      CreateProductInput(
        coverImageUrl = "http://x/cover.png",
        name = "桶装水19L",
        priceCents = 2100,
        salesVolume = 0,
        sortOrder = 1,
        specification = "19L",
        status = ProductStatus.ONLINE,
        stock = 100,
      )

    beforeTest {
      productService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      resolver = AdminProductMutationResolver(productService, bizLogger)
    }
    test("createProduct should create product successfully") {
      // Given
      val input = validCreateInput()
      every { productService.createProduct(any()) } returns createSampleProduct()

      // When
      val result = resolver.createProduct(input)

      // Then
      result.name shouldBe "桶装水19L"
      verify { productService.createProduct(input) }
    }
    test("createProduct should reject blank name") {
      // Given
      val input = validCreateInput().copy(name = "  ")

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.createProduct(input) }
      exception.message!!.contains("产品名称不能为空") shouldBe true
    }
    test("createProduct should reject name longer than 100 chars") {
      // Given
      val input = validCreateInput().copy(name = "水".repeat(101))

      // When & Then
      shouldThrow<BadRequestException> { resolver.createProduct(input) }
    }
    test("createProduct should reject non positive price") {
      // Given
      val input = validCreateInput().copy(priceCents = 0)

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.createProduct(input) }
      exception.message!!.contains("产品价格必须大于0") shouldBe true
    }
    test("createProduct should reject negative stock") {
      // Given
      val input = validCreateInput().copy(stock = -1)

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.createProduct(input) }
      exception.message!!.contains("产品库存不能为负数") shouldBe true
    }
    test("updateProduct should update existing product") {
      // Given
      every { productService.findById(1L) } returns createSampleProduct()
      every {
        productService.updateProduct(
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
        )
      } returns createSampleProduct()
      val input = UpdateProductInput(name = "新桶装水", priceCents = 2500)

      // When
      val result = resolver.updateProduct(1L, input)

      // Then
      result.shouldNotBeNull()
      verify {
        productService.updateProduct(
          1L,
          "新桶装水",
          2500L,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
        )
      }
    }
    test("updateProduct should throw when product not found") {
      // Given
      every { productService.findById(999L) } returns null

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.updateProduct(999L, UpdateProductInput(name = "x")) }
      exception.message!!.contains("产品不存在") shouldBe true
    }
    test("deleteProduct should delete existing product") {
      // Given
      every { productService.findById(1L) } returns createSampleProduct()
      every { bizLogger.resolveOperator() } returns (1L to "admin")

      // When
      val result = resolver.deleteProduct(1L)

      // Then
      result shouldBe true
      verify { bizLogger.logProductOperation(any(), any(), any(), any(), any(), any()) }
    }
    test("deleteProduct should throw when product not found") {
      // Given
      every { productService.findById(999L) } returns null

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.deleteProduct(999L) }
      exception.message!!.contains("产品不存在") shouldBe true
    }
    test("onlineProduct should set status to online") {
      // Given
      every { productService.findById(1L) } returnsMany
        listOf(
          createSampleProduct(ProductModelStatus.OFFLINE),
          createSampleProduct(ProductModelStatus.ONLINE),
        )

      // When
      val result = resolver.onlineProduct(1L)

      // Then
      result.status shouldBe ProductModelStatus.ONLINE
      verify { productService.updateProductStatus(1L, ProductStatus.ONLINE) }
    }
    test("onlineProduct should throw when already online") {
      // Given
      every { productService.findById(1L) } returns createSampleProduct(ProductModelStatus.ONLINE)

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.onlineProduct(1L) }
      exception.message!!.contains("产品已上线") shouldBe true
    }
    test("offlineProduct should set status to offline") {
      // Given
      every { productService.findById(1L) } returnsMany
        listOf(
          createSampleProduct(ProductModelStatus.ONLINE),
          createSampleProduct(ProductModelStatus.OFFLINE),
        )

      // When
      val result = resolver.offlineProduct(1L)

      // Then
      result.status shouldBe ProductModelStatus.OFFLINE
      verify { productService.updateProductStatus(1L, ProductStatus.OFFLINE) }
    }
    test("offlineProduct should throw when already offline") {
      // Given
      every { productService.findById(1L) } returns createSampleProduct(ProductModelStatus.OFFLINE)

      // When & Then
      val exception = shouldThrow<BadRequestException> { resolver.offlineProduct(1L) }
      exception.message!!.contains("产品已下线") shouldBe true
    }
    test("updateProductStatus should throw when product not found") {
      // Given
      every { productService.findById(999L) } returns null

      // When & Then
      shouldThrow<BadRequestException> { resolver.updateProductStatus(999L, ProductStatus.ONLINE) }
    }
  })
