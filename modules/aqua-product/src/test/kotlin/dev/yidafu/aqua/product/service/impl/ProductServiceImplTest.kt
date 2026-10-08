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

package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 商品服务单元测试: 创建/更新/状态变更、库存增减与缺货状态机、销量、统计
 */
class ProductServiceImplTest :
  FunSpec({
    var productRepository: ProductRepository = mockk()
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: ProductServiceImpl = mockk()

    fun product(
      id: Long = 1L,
      status: ProductModelStatus = ProductModelStatus.ONLINE,
      stock: Int = 10,
      price: Long = 200L,
    ): ProductModel =
      ProductModel().apply {
        this.id = id
        name = "怡宝"
        this.price = price
        this.stock = stock
        this.salesVolume = 0
        this.status = status
      }

    beforeTest {
      productRepository = mockk(relaxed = true)
      systemConfigQueryApiService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service = ProductServiceImpl(productRepository, systemConfigQueryApiService, bizLogger)
      // relaxed mock 的 Pair 解构会触发 ClassCastException, 显式 stub 操作人
      every { bizLogger.resolveOperator() } returns (1L to "admin")
    }
    test("findById returns product when exists") {
      val p = product()
      every { productRepository.findById(1L) } returns Optional.of(p)

      service.findById(1L) shouldBe p
    }
    test("findById returns null when missing") {
      every { productRepository.findById(1L) } returns Optional.empty()

      service.findById(1L).shouldBeNull()
    }
    test("createProduct saves and logs operation") {
      val request =
        CreateProductInput(
          name = "农夫山泉",
          coverImageUrl = "http://x/a.jpg",
          priceCents = 300L,
          specification = "550ml",
          stock = 5,
          salesVolume = 0,
          sortOrder = 1,
          status = ProductStatus.ONLINE,
        )
      every { productRepository.save(any()) } answers { (firstArg<ProductModel>()).also { it.id = 1L } }

      val saved = service.createProduct(request)

      saved.name shouldBe "农夫山泉"
      saved.price shouldBe 300L
      saved.status shouldBe ProductModelStatus.ONLINE
      verify { bizLogger.logProductOperation(any(), any(), any(), any(), any(), any()) }
    }
    test("updateProduct applies non-null fields only") {
      val p = product()
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.save(any()) } answers { firstArg() }

      val updated =
        service.updateProduct(
          productId = 1L,
          name = "新名字",
          priceCents = 500L,
          coverImageUrl = null,
          description = null,
          stock = null,
          status = ProductStatus.OFFLINE,
        )

      updated.name shouldBe "新名字"
      updated.price shouldBe 500L
      updated.status shouldBe ProductModelStatus.OFFLINE
      verify { bizLogger.logProductOperation(any(), any(), any(), any(), any(), any()) }
    }
    test("updateProduct throws when product missing") {
      every { productRepository.findById(1L) } returns Optional.empty()

      shouldThrow<IllegalArgumentException> {
        service.updateProduct(productId = 1L, name = "x", priceCents = null, coverImageUrl = null, description = null, stock = null)
      }
    }
    test("updateProductStatus changes status and logs before after") {
      val p = product(status = ProductModelStatus.ONLINE)
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.save(any()) } answers { firstArg() }

      val updated = service.updateProductStatus(1L, ProductStatus.OFFLINE)

      updated.status shouldBe ProductModelStatus.OFFLINE
      verify { bizLogger.logProductOperation(any(), "PRODUCT_STATUS_CHANGED", any(), any(), any(), any()) }
    }
    test("increaseStock adds quantity and revives out of stock product") {
      val p = product(status = ProductModelStatus.OUT_OF_STOCK, stock = 0)
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.save(any()) } answers { firstArg() }

      service.increaseStock(1L, 5) shouldBe true
      p.stock shouldBe 5
      p.status shouldBe ProductModelStatus.ONLINE
    }
    test("increaseStock returns false when product missing") {
      every { productRepository.findById(1L) } returns Optional.empty()

      service.increaseStock(1L, 5) shouldBe false
    }
    test("decreaseStock reduces quantity and marks out of stock at zero") {
      val p = product(stock = 3)
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.save(any()) } answers { firstArg() }

      service.decreaseStock(1L, 3) shouldBe true
      p.stock shouldBe 0
      p.status shouldBe ProductModelStatus.OUT_OF_STOCK
    }
    test("decreaseStock refuses when quantity exceeds stock") {
      val p = product(stock = 2)
      every { productRepository.findById(1L) } returns Optional.of(p)

      service.decreaseStock(1L, 5) shouldBe false
      p.stock shouldBe 2
    }
    test("decreaseStock returns false when product missing") {
      every { productRepository.findById(1L) } returns Optional.empty()

      service.decreaseStock(1L, 5) shouldBe false
    }
    test("incrementSalesVolume adds quantity") {
      val p = product()
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.save(any()) } answers { firstArg() }

      service.incrementSalesVolume(1L, 7)

      p.salesVolume shouldBe 7
      verify { productRepository.save(p) }
    }
    test("updateSalesVolume sets volume") {
      val p = product()
      every { productRepository.findById(1L) } returns Optional.of(p)
      every { productRepository.save(any()) } answers { firstArg() }

      service.updateSalesVolume(1L, 9)

      p.salesVolume shouldBe 9
    }
    test("getProductStatistics aggregates statuses and low stock") {
      val online = product(1L, ProductModelStatus.ONLINE, stock = 50, price = 200L)
      val offline = product(2L, ProductModelStatus.OFFLINE, stock = 8, price = 300L)
      val out = product(3L, ProductModelStatus.OUT_OF_STOCK, stock = 0, price = 400L)
      every { productRepository.findAll() } returns listOf(online, offline, out)
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L

      val stats = service.getProductStatistics()

      stats.totalProducts shouldBe 3
      stats.onlineProducts shouldBe 1
      stats.offlineProducts shouldBe 1
      stats.lowStockProducts shouldBe 2
      stats.totalValueCents shouldBe 900L
      stats.averagePriceCents shouldBe 300L
    }
    test("getProductStatistics handles empty product list") {
      every { productRepository.findAll() } returns emptyList()

      val stats = service.getProductStatistics()

      stats.totalProducts shouldBe 0
      stats.averagePriceCents shouldBe 0L
      stats.lowStockProducts shouldBe 0
    }
  })
