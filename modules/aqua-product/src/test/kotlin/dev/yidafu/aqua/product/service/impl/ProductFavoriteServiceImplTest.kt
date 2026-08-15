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

import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.BatchFavoriteOperation
import dev.yidafu.aqua.common.graphql.generated.BatchFavoriteOperationInput
import dev.yidafu.aqua.common.graphql.generated.ExportFavoritesInput
import dev.yidafu.aqua.common.graphql.generated.ExportFormat
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepositoryCustom
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 商品收藏服务单元测试: 收藏/取消/启用、分页过滤、统计、批量操作、导出
 */
class ProductFavoriteServiceImplTest {
  private lateinit var productFavoriteRepository: ProductFavoriteRepository
  private lateinit var productRepository: ProductRepository
  private lateinit var userService: UserService
  private lateinit var productService: ProductService
  private lateinit var service: ProductFavoriteServiceImpl

  @BeforeEach
  fun setUp() {
    productFavoriteRepository = mockk(relaxed = true)
    productRepository = mockk(relaxed = true)
    userService = mockk(relaxed = true)
    productService = mockk(relaxed = true)
    service = ProductFavoriteServiceImpl(productFavoriteRepository, productRepository, userService, productService)
    every { productFavoriteRepository.save(any()) } answers { firstArg() }
  }

  private fun product(
    id: Long = 1L,
    status: ProductModelStatus = ProductModelStatus.ONLINE,
  ): ProductModel =
    ProductModel().apply {
      this.id = id
      name = "P$id"
      this.price = 100L
      this.status = status
    }

  @Test
  fun `addToFavorites creates new favorite`() {
    every { productRepository.findById(1L) } returns Optional.of(product())
    every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returns null
    every { productFavoriteRepository.save(any()) } answers { firstArg() }

    val favorite = service.addToFavorites(1L, 1L)

    assertEquals(1L, favorite.userId)
    assertEquals(1L, favorite.productId)
    assertTrue(favorite.enable)
  }

  @Test
  fun `addToFavorites throws when product missing`() {
    every { productRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<NotFoundException> { service.addToFavorites(1L, 1L) }
  }

  @Test
  fun `addToFavorites throws when product offline`() {
    every { productRepository.findById(1L) } returns Optional.of(product(status = ProductModelStatus.OFFLINE))

    assertFailsWith<BadRequestException> { service.addToFavorites(1L, 1L) }
  }

  @Test
  fun `addToFavorites throws when already enabled`() {
    every { productRepository.findById(1L) } returns Optional.of(product())
    every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returns
      ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)

    assertFailsWith<BadRequestException> { service.addToFavorites(1L, 1L) }
  }

  @Test
  fun `addToFavorites re-enables disabled favorite`() {
    every { productRepository.findById(1L) } returns Optional.of(product())
    val disabled = ProductFavoriteModel(userId = 1L, productId = 1L, enable = false)
    every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returnsMany listOf(disabled, disabled)
    every { productFavoriteRepository.updateEnableStatus(1L, 1L, true) } returns 1
    every { productFavoriteRepository.save(any()) } answers { firstArg() }

    val favorite = service.addToFavorites(1L, 1L)

    assertTrue(favorite.enable)
  }

  @Test
  fun `getFavoriteProducts returns active products only`() {
    val pageable = PageRequest.of(0, 10)
    val ids = listOf(1L, 2L)
    every { productFavoriteRepository.findFavoriteIdsByUserId(1L, pageable) } returns PageImpl(ids, pageable, 2L)
    every { productRepository.findAllById(ids) } returns listOf(product(1L), product(2L, ProductModelStatus.OFFLINE))

    val result = service.getFavoriteProducts(1L, pageable)

    assertEquals(1, result.content.size)
    assertEquals(1L, result.content[0].id)
  }

  @Test
  fun `getFavoriteProducts returns empty page when no favorites`() {
    val pageable = PageRequest.of(0, 10)
    every { productFavoriteRepository.findFavoriteIdsByUserId(1L, pageable) } returns PageImpl(emptyList<Long>(), pageable, 0L)

    val result = service.getFavoriteProducts(1L, pageable)

    assertTrue(result.isEmpty)
  }

  @Test
  fun `isProductFavorited delegates to repository`() {
    every { productFavoriteRepository.existsByUserIdAndProductId(1L, 1L) } returns true

    assertTrue(service.isProductFavorited(1L, 1L))
  }

  @Test
  fun `getFavoritesCount delegates to repository`() {
    every { productFavoriteRepository.countByUserId(1L) } returns 3L

    assertEquals(3L, service.getFavoritesCount(1L))
  }

  @Test
  fun `getFavoriteProductIds delegates to repository`() {
    every { productFavoriteRepository.findFavoriteProductIdsByUserId(1L) } returns listOf(1L, 2L)

    assertEquals(listOf(1L, 2L), service.getFavoriteProductIds(1L))
  }

  @Test
  fun `toggleFavorite toggles existing favorite`() {
    val existing = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
    every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returnsMany listOf(existing, existing)
    every { productFavoriteRepository.updateEnableStatus(1L, 1L, false) } returns 1

    val enabled = service.toggleFavorite(1L, 1L)

    assertFalse(enabled)
    verify { productFavoriteRepository.save(any()) }
  }

  @Test
  fun `toggleFavorite creates favorite when absent`() {
    every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returns null
    every { productRepository.findById(1L) } returns Optional.of(product())
    every { productFavoriteRepository.save(any()) } answers { firstArg() }

    assertTrue(service.toggleFavorite(1L, 1L))
  }

  @Test
  fun `getFavoriteAnalytics computes metrics`() {
    every { productFavoriteRepository.count() } returns 10L
    every { productFavoriteRepository.countDistinctUsersWithFavorites() } returns 4L
    every { productFavoriteRepository.getAverageFavoritesPerUser() } returns 2.5
    every { productFavoriteRepository.countFavoritesSince(any()) } returnsMany listOf(1L, 3L, 6L)

    val analytics = service.getFavoriteAnalytics()

    assertEquals(10L, analytics.totalFavorites)
    assertEquals(4L, analytics.activeUsersWithFavorites)
    assertEquals(2.5f, analytics.averageFavoritesPerUser)
    assertEquals(1L, analytics.newFavoritesToday)
    assertEquals(3L, analytics.newFavoritesThisWeek)
    assertEquals(6L, analytics.newFavoritesThisMonth)
  }

  @Test
  fun `getFavoriteAnalytics handles zero active users`() {
    every { productFavoriteRepository.count() } returns 0L
    every { productFavoriteRepository.countDistinctUsersWithFavorites() } returns 0L

    val analytics = service.getFavoriteAnalytics()

    assertEquals(0.0f, analytics.averageFavoritesPerUser)
  }

  @Test
  fun `getProductFavoriteStats maps counts with product names`() {
    val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
    every { count.productId } returns 1L
    every { count.favoriteCount } returns 5L
    every { productFavoriteRepository.findMostFavoritedProducts() } returns listOf(count)
    every { productRepository.findAll() } returns listOf(product(1L))

    val stats = service.getProductFavoriteStats()

    assertEquals(1, stats.size)
    assertEquals(1L, stats[0].productId)
    assertEquals("P1", stats[0].productName)
    assertEquals(5L, stats[0].favoriteCount)
  }

  @Test
  fun `getAllProductsFavoriteStats aggregates favorites`() {
    val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
    every { count.favoriteCount } returns 4L
    every { productFavoriteRepository.findMostFavoritedProducts() } returns listOf(count)

    val stats = service.getAllProductsFavoriteStats()

    assertEquals(1L, stats.totalProducts)
    assertEquals(4L, stats.totalFavorites)
    assertEquals(4.0f, stats.averageFavoritesPerProduct)
  }

  @Test
  fun `performBatchOperation supports export`() {
    val result =
      service.performBatchOperation(
        BatchFavoriteOperationInput(
          operation = BatchFavoriteOperation.EXPORT_FAVORITES,
          productIds = listOf(1L),
          reason = "",
          userIds = listOf(1L),
        ),
      )

    assertTrue(result.success)
    assertEquals(1, result.successCount)
  }

  @Test
  fun `performBatchOperation supports analyze`() {
    val result =
      service.performBatchOperation(
        BatchFavoriteOperationInput(
          operation = BatchFavoriteOperation.ANALYZE_FAVORITES,
          productIds = listOf(1L),
          reason = "",
          userIds = listOf(1L),
        ),
      )

    assertTrue(result.success)
  }

  @Test
  fun `performBatchOperation fails for clear operations`() {
    val result =
      service.performBatchOperation(
        BatchFavoriteOperationInput(
          operation = BatchFavoriteOperation.CLEAR_ALL_FAVORITES,
          productIds = listOf(1L),
          reason = "",
          userIds = listOf(1L),
        ),
      )

    assertFalse(result.success)
    assertEquals(1, result.failureCount)
  }

  @Test
  fun `exportFavorites returns failure when no data`() {
    every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any()) } returns emptyList()

    val result = service.exportFavorites(ExportFavoritesInput(format = ExportFormat.CSV))

    assertFalse(result.success)
    assertEquals(0, result.recordCount)
  }

  @Test
  fun `exportFavorites exports csv`() {
    val favorite = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
    every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any()) } returns listOf(favorite)
    every { productRepository.findById(any()) } returns Optional.empty()

    val result = service.exportFavorites(ExportFavoritesInput(format = ExportFormat.CSV, includeProductInfo = false))

    assertTrue(result.success)
    assertEquals(1, result.recordCount)
    assertEquals(
      ExportFormat.CSV.name,
      result.fileName
        .split(".")
        .last()
        .uppercase(),
    )
  }

  @Test
  fun `exportFavorites returns failure when no data with date range`() {
    every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any()) } returns emptyList()

    val result =
      service.exportFavorites(
        ExportFavoritesInput(
          format = ExportFormat.EXCEL,
          userId = 1L,
          productIds = listOf(1L),
          dateFrom = LocalDateTime.of(2025, 1, 1, 0, 0),
          dateTo = LocalDateTime.of(2025, 12, 31, 23, 59),
        ),
      )

    assertFalse(result.success)
  }

  @Test
  fun `getUserFavoriteEntities delegates to repository`() {
    val pageable = PageRequest.of(0, 10)
    val page = PageImpl(listOf(ProductFavoriteModel(userId = 1L, productId = 1L)), pageable, 1L)
    every { productFavoriteRepository.findByUserId(1L, pageable) } returns page

    assertEquals(page, service.getUserFavoriteEntities(1L, pageable))
  }

  @Test
  fun `getProductsByFavorites returns paged items with page info`() {
    val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
    every { count.productId } returns 1L
    every { count.favoriteCount } returns 3L
    every { productFavoriteRepository.findMostFavoritedProducts() } returns listOf(count)
    every { productRepository.findById(1L) } returns Optional.of(product(1L))
    every { productFavoriteRepository.findByProductId(1L) } returns
      listOf(ProductFavoriteModel(userId = 1L, productId = 1L, createdAt = LocalDateTime.of(2025, 6, 1, 10, 0)))

    val vo = service.getProductsByFavorites(page = 0, size = 10, minFavorites = 1)

    assertEquals(1, vo.list.size)
    assertEquals(3L, vo.list[0].favoriteCount)
    assertEquals(1, vo.pageInfo.total)
    assertEquals(1, vo.pageInfo.totalPages)
    assertEquals(false, vo.pageInfo.hasNext)
  }

  @Test
  fun `getProductsByFavorites filters by min favorites and handles empty page`() {
    val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
    every { count.productId } returns 1L
    every { count.favoriteCount } returns 1L
    every { productFavoriteRepository.findMostFavoritedProducts() } returns listOf(count)
    every { productRepository.findById(1L) } returns Optional.of(product(1L))

    val empty = service.getProductsByFavorites(page = 5, size = 10, minFavorites = 5)

    assertEquals(0, empty.list.size)
    assertEquals(0, empty.pageInfo.total)
    assertEquals(true, empty.pageInfo.hasPrevious)

    val filtered = service.getProductsByFavorites(page = 0, size = 10, minFavorites = 1)

    assertEquals(1, filtered.list.size)
    assertEquals(0, filtered.pageInfo.pageNum)
  }

  @Test
  fun `getProductsByFavorites skips products that no longer exist`() {
    val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
    every { count.productId } returns 99L
    every { count.favoriteCount } returns 2L
    every { productFavoriteRepository.findMostFavoritedProducts() } returns listOf(count)
    every { productRepository.findById(99L) } returns Optional.empty()

    val vo = service.getProductsByFavorites(page = 0, size = 10, minFavorites = null)

    assertEquals(0, vo.list.size)
    assertEquals(1, vo.pageInfo.total)
  }

  @Test
  fun `exportFavorites exports excel`() {
    val favorite = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
    every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any()) } returns listOf(favorite)
    every { productRepository.findById(any()) } returns Optional.empty()

    val result = service.exportFavorites(ExportFavoritesInput(format = ExportFormat.EXCEL))

    assertTrue(result.success)
    assertEquals(1, result.recordCount)
    assertTrue(result.fileName.endsWith(".xlsx"))
  }

  @Test
  fun `exportFavorites json throws`() {
    val favorite = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
    every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any()) } returns listOf(favorite)

    assertFailsWith<BadRequestException> { service.exportFavorites(ExportFavoritesInput(format = ExportFormat.JSON)) }
  }
}
