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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductDailySalesResult
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductInfo
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductSalesStatisticsRepositoryCustom
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductFavoriteStat
import dev.yidafu.aqua.common.graphql.generated.StockWarningStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

/**
 * 商品统计服务单元测试(PRD F-STA/R-PRO-02: 上下架/低库存/排行榜/每日销量补零)
 */
class ProductStatisticsServiceImplTest :
  FunSpec({
    var productQueryApiService: ProductQueryApiService = mockk()
    var productFavoriteService: ProductFavoriteService = mockk()
    var productSalesStatisticsRepository: ProductSalesStatisticsRepositoryCustom = mockk()
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var service: ProductStatisticsServiceImpl = mockk()

    fun product(
      id: Long,
      name: String,
      price: Long,
      stock: Int,
      salesVolume: Int,
      status: ProductModelStatus,
    ): ProductModel =
      ProductModel().apply {
        this.id = id
        this.name = name
        this.price = price
        this.stock = stock
        this.salesVolume = salesVolume
        this.status = status
      }

    beforeTest {
      productQueryApiService = mockk(relaxed = true)
      productFavoriteService = mockk(relaxed = true)
      productSalesStatisticsRepository = mockk(relaxed = true)
      systemConfigQueryApiService = mockk(relaxed = true)
      service =
        ProductStatisticsServiceImpl(
          productQueryApiService,
          productFavoriteService,
          productSalesStatisticsRepository,
          systemConfigQueryApiService,
        )
    }
    test("getProductStatistics computes counts value and ranking") {
      val online = product(1L, "怡宝", 200L, stock = 50, salesVolume = 100, status = ProductModelStatus.ONLINE)
      val lowStock = product(2L, "农夫山泉", 300L, stock = 3, salesVolume = 20, status = ProductModelStatus.ONLINE)
      val offline = product(3L, "百岁山", 400L, stock = 10, salesVolume = 5, status = ProductModelStatus.OFFLINE)
      every { productQueryApiService.findByStatus(ProductModelStatus.ONLINE) } returns listOf(online, lowStock, offline)
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L
      every { productFavoriteService.getProductFavoriteStats() } returns
        listOf(
          ProductFavoriteStat(favoriteCount = 4L, productId = 1L, productName = "怡宝"),
          ProductFavoriteStat(favoriteCount = 2L, productId = 2L, productName = "农夫山泉"),
        )

      val result = service.getProductStatistics()

      result.totalProducts shouldBe 3
      result.onlineProducts shouldBe 2
      result.offlineProducts shouldBe 1
      // stock<=10 的商品 (lowStock 3, offline 10) 都算低库存
      result.lowStockProducts shouldBe 2
      result.totalValueCents shouldBe 900L
      result.averagePriceCents shouldBe 300L
      result.totalSales shouldBe 125
      result.totalFavorites shouldBe 6
      result.favoriteStatistics.size shouldBe 2
    }
    test("getProductStatistics ranks online products by sales desc and caps at 10") {
      val products = (1L..12L).map { i -> product(i, "P$i", 100L, stock = 99, salesVolume = i.toInt(), status = ProductModelStatus.ONLINE) }
      every { productQueryApiService.findByStatus(ProductModelStatus.ONLINE) } returns products
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L
      every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

      val result = service.getProductStatistics()

      result.productRanking.size shouldBe 10
      result.productRanking.first().productId shouldBe 12L
      result.productRanking.first().revenueCents shouldBe 100L * 12
      result.productRanking.last().productId shouldBe 3L
    }
    test("getProductStatistics handles empty products") {
      every { productQueryApiService.findByStatus(ProductModelStatus.ONLINE) } returns emptyList()
      every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

      val result = service.getProductStatistics()

      result.totalProducts shouldBe 0
      result.totalValueCents shouldBe 0L
      result.averagePriceCents shouldBe 0L
      result.totalSales shouldBe 0
      result.totalFavorites shouldBe 0
      result.productRanking.size shouldBe 0
      result.stockWarningItems.size shouldBe 0
    }
    test("getProductStatistics builds stock warning items sorted with out-of-stock first") {
      // G39: 库存预警明细 —— 库存≤阈值(含缺货),缺货(库存=0)置顶,按库存升序,阈值与状态正确
      val out = product(1L, "缺货水", 200L, stock = 0, salesVolume = 10, status = ProductModelStatus.ONLINE)
      val low = product(2L, "低库存水", 300L, stock = 3, salesVolume = 20, status = ProductModelStatus.ONLINE)
      val boundary = product(3L, "边界水", 400L, stock = 10, salesVolume = 5, status = ProductModelStatus.ONLINE)
      val safe = product(4L, "充足水", 500L, stock = 50, salesVolume = 5, status = ProductModelStatus.ONLINE)
      every { productQueryApiService.findByStatus(ProductModelStatus.ONLINE) } returns listOf(out, low, boundary, safe)
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L
      every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

      val result = service.getProductStatistics()

      result.lowStockProducts shouldBe 3
      result.stockWarningItems.size shouldBe 3
      // 缺货置顶 → 其余按库存升序
      result.stockWarningItems[0].productName shouldBe "缺货水"
      result.stockWarningItems[0].currentStock shouldBe 0
      result.stockWarningItems[0].status shouldBe StockWarningStatus.OUT_OF_STOCK
      result.stockWarningItems[1].productName shouldBe "低库存水"
      result.stockWarningItems[1].currentStock shouldBe 3
      result.stockWarningItems[1].status shouldBe StockWarningStatus.WARNING
      result.stockWarningItems[2].productName shouldBe "边界水"
      result.stockWarningItems[2].threshold shouldBe 10
      result.stockWarningItems[2].status shouldBe StockWarningStatus.WARNING
      // 阈值与计数同源(R-PRO-02)
      result.stockWarningItems.all { it.threshold == 10 } shouldBe true
    }
    test("getProductDailySales fills missing dates with zero entry") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 3)
      every {
        productSalesStatisticsRepository.getProductDailySales(any(), any(), any())
      } returns
        listOf(
          ProductDailySalesResult(date = "2025-06-01", productId = 1L, productName = "怡宝", salesVolume = 3L, revenue = 600L),
        )

      val result = service.getProductDailySales(start, end)

      result.size shouldBe 3
      result[0].date shouldBe "2025-06-01"
      result[0].productId shouldBe 1L
      result[0].revenueCents shouldBe 600L
      // 无销售日期 → 占位
      result[1].productName shouldBe "无销售"
      result[1].salesVolume shouldBe 0
      result[1].revenueCents shouldBe 0L
    }
    test("getAllProducts returns product infos") {
      val infos = listOf(ProductInfo(productId = 1L, productName = "怡宝"))
      every { productSalesStatisticsRepository.getAllProductIdsAndNames() } returns infos

      service.getAllProducts() shouldBe infos
    }
  })
