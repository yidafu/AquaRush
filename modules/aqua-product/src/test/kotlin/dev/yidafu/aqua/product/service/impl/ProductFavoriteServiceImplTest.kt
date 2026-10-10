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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

/**
 * 商品收藏服务单元测试: 收藏/取消/启用、分页过滤、统计、批量操作、导出
 */
class ProductFavoriteServiceImplTest :
  FunSpec({
    var productFavoriteRepository: ProductFavoriteRepository = mockk()
    var productRepository: ProductRepository = mockk()
    var userService: UserService = mockk()
    var productService: ProductService = mockk()
    var service: ProductFavoriteServiceImpl = mockk()

    fun product(
      id: Long = 1L,
      status: ProductModelStatus = ProductModelStatus.ONLINE,
    ): ProductModel =
      ProductModel().apply {
        this.id = id
        name = "P$id"
        this.price = 100L
        this.status = status
      }

    beforeTest {
      productFavoriteRepository = mockk(relaxed = true)
      productRepository = mockk(relaxed = true)
      userService = mockk(relaxed = true)
      productService = mockk(relaxed = true)
      service = ProductFavoriteServiceImpl(productFavoriteRepository, productRepository, userService, productService)
      every { productFavoriteRepository.save(any()) } answers { firstArg() }
    }
    test("addToFavorites creates new favorite") {
      every { productRepository.findById(1L) } returns Optional.of(product())
      every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returns null
      every { productFavoriteRepository.save(any()) } answers { firstArg() }

      val favorite = service.addToFavorites(1L, 1L)

      favorite.userId shouldBe 1L
      favorite.productId shouldBe 1L
      favorite.enable shouldBe true
    }
    test("addToFavorites throws when product missing") {
      every { productRepository.findById(1L) } returns Optional.empty()

      shouldThrow<NotFoundException> { service.addToFavorites(1L, 1L) }
    }
    test("addToFavorites throws when product offline") {
      every { productRepository.findById(1L) } returns Optional.of(product(status = ProductModelStatus.OFFLINE))

      shouldThrow<BadRequestException> { service.addToFavorites(1L, 1L) }
    }
    test("addToFavorites throws when already enabled") {
      every { productRepository.findById(1L) } returns Optional.of(product())
      every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returns
        ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)

      shouldThrow<BadRequestException> { service.addToFavorites(1L, 1L) }
    }
    test("addToFavorites re-enables disabled favorite") {
      every { productRepository.findById(1L) } returns Optional.of(product())
      val disabled = ProductFavoriteModel(userId = 1L, productId = 1L, enable = false)
      every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returnsMany listOf(disabled, disabled)
      every { productFavoriteRepository.updateEnableStatus(1L, 1L, true) } returns 1
      every { productFavoriteRepository.save(any()) } answers { firstArg() }

      val favorite = service.addToFavorites(1L, 1L)

      favorite.enable shouldBe true
    }
    test("getFavoriteProducts returns active products only") {
      val pageable = PageRequest.of(0, 10)
      val ids = listOf(1L, 2L)
      every { productFavoriteRepository.findFavoriteIdsByUserId(1L, pageable) } returns PageImpl(ids, pageable, 2L)
      every { productRepository.findAllById(ids) } returns listOf(product(1L), product(2L, ProductModelStatus.OFFLINE))

      val result = service.getFavoriteProducts(1L, pageable)

      result.content.size shouldBe 1
      result.content[0].id shouldBe 1L
    }
    test("getFavoriteProducts returns empty page when no favorites") {
      val pageable = PageRequest.of(0, 10)
      every { productFavoriteRepository.findFavoriteIdsByUserId(1L, pageable) } returns PageImpl(emptyList<Long>(), pageable, 0L)

      val result = service.getFavoriteProducts(1L, pageable)

      result.isEmpty shouldBe true
    }
    test("isProductFavorited delegates to repository") {
      every { productFavoriteRepository.existsByUserIdAndProductId(1L, 1L) } returns true

      service.isProductFavorited(1L, 1L) shouldBe true
    }
    test("getFavoritesCount delegates to repository") {
      every { productFavoriteRepository.countByUserId(1L) } returns 3L

      service.getFavoritesCount(1L) shouldBe 3L
    }
    test("getFavoriteProductIds delegates to repository") {
      every { productFavoriteRepository.findFavoriteProductIdsByUserId(1L) } returns listOf(1L, 2L)

      service.getFavoriteProductIds(1L) shouldBe listOf(1L, 2L)
    }
    test("toggleFavorite toggles existing favorite") {
      val existing = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
      every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returnsMany listOf(existing, existing)
      every { productFavoriteRepository.updateEnableStatus(1L, 1L, false) } returns 1

      val enabled = service.toggleFavorite(1L, 1L)

      enabled shouldBe false
      verify { productFavoriteRepository.save(any()) }
    }
    test("toggleFavorite creates favorite when absent") {
      every { productFavoriteRepository.findByUserIdAndProductId(1L, 1L) } returns null
      every { productRepository.findById(1L) } returns Optional.of(product())
      every { productFavoriteRepository.save(any()) } answers { firstArg() }

      service.toggleFavorite(1L, 1L) shouldBe true
    }
    test("getFavoriteAnalytics computes metrics") {
      every { productFavoriteRepository.count() } returns 10L
      every { productFavoriteRepository.countDistinctUsersWithFavorites() } returns 4L
      every { productFavoriteRepository.getAverageFavoritesPerUser() } returns 2.5
      every { productFavoriteRepository.countFavoritesSince(any()) } returnsMany listOf(1L, 3L, 6L)

      val analytics = service.getFavoriteAnalytics()

      analytics.totalFavorites shouldBe 10L
      analytics.activeUsersWithFavorites shouldBe 4L
      analytics.averageFavoritesPerUser shouldBe 2.5f
      analytics.newFavoritesToday shouldBe 1L
      analytics.newFavoritesThisWeek shouldBe 3L
      analytics.newFavoritesThisMonth shouldBe 6L
    }
    test("getFavoriteAnalytics handles zero active users") {
      every { productFavoriteRepository.count() } returns 0L
      every { productFavoriteRepository.countDistinctUsersWithFavorites() } returns 0L

      val analytics = service.getFavoriteAnalytics()

      analytics.averageFavoritesPerUser shouldBe 0.0f
    }
    test("getProductFavoriteStats maps counts with product names") {
      val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
      every { count.productId } returns 1L
      every { count.favoriteCount } returns 5L
      every { productFavoriteRepository.findTopFavoritedProducts(10) } returns listOf(count)
      every { productRepository.findAllById(listOf(1L)) } returns listOf(product(1L))

      val stats = service.getProductFavoriteStats()

      stats.size shouldBe 1
      stats[0].productId shouldBe 1L
      stats[0].productName shouldBe "P1"
      stats[0].favoriteCount shouldBe 5L
    }
    test("getAllProductsFavoriteStats aggregates favorites") {
      // 规则 10:总数与总收藏数走 SQL 聚合,不读全量聚合结果
      every { productFavoriteRepository.countFavoritedProducts() } returns 1L
      every { productFavoriteRepository.countTotalFavorites() } returns 4L

      val stats = service.getAllProductsFavoriteStats()

      stats.totalProducts shouldBe 1L
      stats.totalFavorites shouldBe 4L
      stats.averageFavoritesPerProduct shouldBe 4.0f
    }
    test("performBatchOperation supports export") {
      val result =
        service.performBatchOperation(
          BatchFavoriteOperationInput(
            operation = BatchFavoriteOperation.EXPORT_FAVORITES,
            productIds = listOf(1L),
            reason = "",
            userIds = listOf(1L),
          ),
        )

      result.success shouldBe true
      result.successCount shouldBe 1
    }
    test("performBatchOperation supports analyze") {
      val result =
        service.performBatchOperation(
          BatchFavoriteOperationInput(
            operation = BatchFavoriteOperation.ANALYZE_FAVORITES,
            productIds = listOf(1L),
            reason = "",
            userIds = listOf(1L),
          ),
        )

      result.success shouldBe true
    }
    test("performBatchOperation fails for clear operations") {
      val result =
        service.performBatchOperation(
          BatchFavoriteOperationInput(
            operation = BatchFavoriteOperation.CLEAR_ALL_FAVORITES,
            productIds = listOf(1L),
            reason = "",
            userIds = listOf(1L),
          ),
        )

      result.success shouldBe false
      result.failureCount shouldBe 1
    }
    test("exportFavorites returns failure when no data") {
      every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any(), any(), any()) } returns emptyList()

      val result = service.exportFavorites(ExportFavoritesInput(format = ExportFormat.CSV))

      result.success shouldBe false
      result.recordCount shouldBe 0
    }
    test("exportFavorites exports csv") {
      val favorite = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
      every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any(), any(), any()) } returns listOf(favorite)
      every { productRepository.findById(any()) } returns Optional.empty()

      val result = service.exportFavorites(ExportFavoritesInput(format = ExportFormat.CSV, includeProductInfo = false))

      result.success shouldBe true
      result.recordCount shouldBe 1
      result.fileName
        .split(".")
        .last()
        .uppercase() shouldBe ExportFormat.CSV.name
    }
    test("exportFavorites returns failure when no data with date range") {
      every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any(), any(), any()) } returns emptyList()

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

      result.success shouldBe false
    }
    test("getUserFavoriteEntities delegates to repository") {
      val pageable = PageRequest.of(0, 10)
      val page = PageImpl(listOf(ProductFavoriteModel(userId = 1L, productId = 1L)), pageable, 1L)
      every { productFavoriteRepository.findByUserId(1L, pageable) } returns page

      service.getUserFavoriteEntities(1L, pageable) shouldBe page
    }
    test("getProductsByFavorites returns paged items with page info") {
      val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
      every { count.productId } returns 1L
      every { count.favoriteCount } returns 3L
      every {
        productFavoriteRepository.findMostFavoritedProducts(PageRequest.of(0, 10), 1L)
      } returns PageImpl(listOf(count), PageRequest.of(0, 10), 1L)
      every { productRepository.findAllById(listOf(1L)) } returns listOf(product(1L))
      every { count.lastFavoritedAt } returns LocalDateTime.of(2025, 6, 1, 10, 0)

      val vo = service.getProductsByFavorites(page = 0, size = 10, minFavorites = 1)

      vo.list.size shouldBe 1
      vo.list[0].favoriteCount shouldBe 3L
      vo.pageInfo.total shouldBe 1
      vo.pageInfo.totalPages shouldBe 1
      vo.pageInfo.hasNext shouldBe false
    }
    test("getProductsByFavorites filters by min favorites and handles empty page") {
      val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
      every { count.productId } returns 1L
      every { count.favoriteCount } returns 1L
      every {
        productFavoriteRepository.findMostFavoritedProducts(PageRequest.of(5, 10), 5L)
      } returns PageImpl(emptyList(), PageRequest.of(5, 10), 0L)
      every {
        productFavoriteRepository.findMostFavoritedProducts(PageRequest.of(0, 10), 1L)
      } returns PageImpl(listOf(count), PageRequest.of(0, 10), 1L)
      every { productRepository.findAllById(listOf(1L)) } returns listOf(product(1L))

      val empty = service.getProductsByFavorites(page = 5, size = 10, minFavorites = 5)

      empty.list.size shouldBe 0
      empty.pageInfo.total shouldBe 0
      empty.pageInfo.hasPrevious shouldBe true

      val filtered = service.getProductsByFavorites(page = 0, size = 10, minFavorites = 1)

      filtered.list.size shouldBe 1
      filtered.pageInfo.pageNum shouldBe 0
    }
    test("getProductsByFavorites skips products that no longer exist") {
      val count = mockk<ProductFavoriteRepositoryCustom.ProductFavoriteCount>(relaxed = true)
      every { count.productId } returns 99L
      every { count.favoriteCount } returns 2L
      every {
        productFavoriteRepository.findMostFavoritedProducts(PageRequest.of(0, 10), null)
      } returns PageImpl(listOf(count), PageRequest.of(0, 10), 1L)
      every { productRepository.findAllById(listOf(99L)) } returns emptyList()

      val vo = service.getProductsByFavorites(page = 0, size = 10, minFavorites = null)

      vo.list.size shouldBe 0
      vo.pageInfo.total shouldBe 1
    }
    test("exportFavorites exports excel") {
      val favorite = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
      every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any(), any(), any()) } returns listOf(favorite)
      every { productRepository.findById(any()) } returns Optional.empty()

      val result = service.exportFavorites(ExportFavoritesInput(format = ExportFormat.EXCEL))

      result.success shouldBe true
      result.recordCount shouldBe 1
      result.fileName.endsWith(".xlsx") shouldBe true
    }
    test("exportFavorites json throws") {
      val favorite = ProductFavoriteModel(userId = 1L, productId = 1L, enable = true)
      every { productFavoriteRepository.findFavoritesForExport(any(), any(), any(), any(), any(), any()) } returns listOf(favorite)

      shouldThrow<BadRequestException> { service.exportFavorites(ExportFavoritesInput(format = ExportFormat.JSON)) }
    }
  })
