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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 商品服务单元测试: 创建/更新/状态变更、库存增减与缺货状态机、销量、统计
 */
class ProductServiceImplTest {
  private lateinit var productRepository: ProductRepository
  private lateinit var systemConfigQueryApiService: SystemConfigQueryApiService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: ProductServiceImpl

  @BeforeEach
  fun setUp() {
    productRepository = mockk(relaxed = true)
    systemConfigQueryApiService = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    service = ProductServiceImpl(productRepository, systemConfigQueryApiService, bizLogger)
    // relaxed mock 的 Pair 解构会触发 ClassCastException, 显式 stub 操作人
    every { bizLogger.resolveOperator() } returns (1L to "admin")
  }

  private fun product(
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

  @Test
  fun `findById returns product when exists`() {
    val p = product()
    every { productRepository.findById(1L) } returns Optional.of(p)

    assertEquals(p, service.findById(1L))
  }

  @Test
  fun `findById returns null when missing`() {
    every { productRepository.findById(1L) } returns Optional.empty()

    assertNull(service.findById(1L))
  }

  @Test
  fun `createProduct saves and logs operation`() {
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

    assertEquals("农夫山泉", saved.name)
    assertEquals(300L, saved.price)
    assertEquals(ProductModelStatus.ONLINE, saved.status)
    verify { bizLogger.logProductOperation(any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun `updateProduct applies non-null fields only`() {
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

    assertEquals("新名字", updated.name)
    assertEquals(500L, updated.price)
    assertEquals(ProductModelStatus.OFFLINE, updated.status)
    verify { bizLogger.logProductOperation(any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun `updateProduct throws when product missing`() {
    every { productRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<IllegalArgumentException> {
      service.updateProduct(productId = 1L, name = "x", priceCents = null, coverImageUrl = null, description = null, stock = null)
    }
  }

  @Test
  fun `updateProductStatus changes status and logs before after`() {
    val p = product(status = ProductModelStatus.ONLINE)
    every { productRepository.findById(1L) } returns Optional.of(p)
    every { productRepository.save(any()) } answers { firstArg() }

    val updated = service.updateProductStatus(1L, ProductStatus.OFFLINE)

    assertEquals(ProductModelStatus.OFFLINE, updated.status)
    verify { bizLogger.logProductOperation(any(), "PRODUCT_STATUS_CHANGED", any(), any(), any(), any()) }
  }

  @Test
  fun `increaseStock adds quantity and revives out of stock product`() {
    val p = product(status = ProductModelStatus.OUT_OF_STOCK, stock = 0)
    every { productRepository.findById(1L) } returns Optional.of(p)
    every { productRepository.save(any()) } answers { firstArg() }

    assertTrue(service.increaseStock(1L, 5))
    assertEquals(5, p.stock)
    assertEquals(ProductModelStatus.ONLINE, p.status)
  }

  @Test
  fun `increaseStock returns false when product missing`() {
    every { productRepository.findById(1L) } returns Optional.empty()

    assertFalse(service.increaseStock(1L, 5))
  }

  @Test
  fun `decreaseStock reduces quantity and marks out of stock at zero`() {
    val p = product(stock = 3)
    every { productRepository.findById(1L) } returns Optional.of(p)
    every { productRepository.save(any()) } answers { firstArg() }

    assertTrue(service.decreaseStock(1L, 3))
    assertEquals(0, p.stock)
    assertEquals(ProductModelStatus.OUT_OF_STOCK, p.status)
  }

  @Test
  fun `decreaseStock refuses when quantity exceeds stock`() {
    val p = product(stock = 2)
    every { productRepository.findById(1L) } returns Optional.of(p)

    assertFalse(service.decreaseStock(1L, 5))
    assertEquals(2, p.stock)
  }

  @Test
  fun `decreaseStock returns false when product missing`() {
    every { productRepository.findById(1L) } returns Optional.empty()

    assertFalse(service.decreaseStock(1L, 5))
  }

  @Test
  fun `incrementSalesVolume adds quantity`() {
    val p = product()
    every { productRepository.findById(1L) } returns Optional.of(p)
    every { productRepository.save(any()) } answers { firstArg() }

    service.incrementSalesVolume(1L, 7)

    assertEquals(7, p.salesVolume)
    verify { productRepository.save(p) }
  }

  @Test
  fun `updateSalesVolume sets volume`() {
    val p = product()
    every { productRepository.findById(1L) } returns Optional.of(p)
    every { productRepository.save(any()) } answers { firstArg() }

    service.updateSalesVolume(1L, 9)

    assertEquals(9, p.salesVolume)
  }

  @Test
  fun `getProductStatistics aggregates statuses and low stock`() {
    val online = product(1L, ProductModelStatus.ONLINE, stock = 50, price = 200L)
    val offline = product(2L, ProductModelStatus.OFFLINE, stock = 8, price = 300L)
    val out = product(3L, ProductModelStatus.OUT_OF_STOCK, stock = 0, price = 400L)
    every { productRepository.findAll() } returns listOf(online, offline, out)
    every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L

    val stats = service.getProductStatistics()

    assertEquals(3, stats.totalProducts)
    assertEquals(1, stats.onlineProducts)
    assertEquals(1, stats.offlineProducts)
    assertEquals(2, stats.lowStockProducts)
    assertEquals(900L, stats.totalValueCents)
    assertEquals(300L, stats.averagePriceCents)
  }

  @Test
  fun `getProductStatistics handles empty product list`() {
    every { productRepository.findAll() } returns emptyList()

    val stats = service.getProductStatistics()

    assertEquals(0, stats.totalProducts)
    assertEquals(0L, stats.averagePriceCents)
    assertEquals(0, stats.lowStockProducts)
  }
}
