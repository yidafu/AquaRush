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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * 商品统计服务单元测试(PRD F-STA/R-PRO-02: 上下架/低库存/排行榜/每日销量补零)
 */
class ProductStatisticsServiceImplTest {
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var productFavoriteService: ProductFavoriteService
  private lateinit var productSalesStatisticsRepository: ProductSalesStatisticsRepositoryCustom
  private lateinit var systemConfigQueryApiService: SystemConfigQueryApiService
  private lateinit var service: ProductStatisticsServiceImpl

  @BeforeEach
  fun setUp() {
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

  private fun product(
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

  @Test
  fun `getProductStatistics computes counts value and ranking`() {
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

    assertEquals(3, result.totalProducts)
    assertEquals(2, result.onlineProducts)
    assertEquals(1, result.offlineProducts)
    // stock<=10 的商品 (lowStock 3, offline 10) 都算低库存
    assertEquals(2, result.lowStockProducts)
    assertEquals(900L, result.totalValueCents)
    assertEquals(300L, result.averagePriceCents)
    assertEquals(125, result.totalSales)
    assertEquals(6, result.totalFavorites)
    assertEquals(2, result.favoriteStatistics.size)
  }

  @Test
  fun `getProductStatistics ranks online products by sales desc and caps at 10`() {
    val products = (1L..12L).map { i -> product(i, "P$i", 100L, stock = 99, salesVolume = i.toInt(), status = ProductModelStatus.ONLINE) }
    every { productQueryApiService.findByStatus(ProductModelStatus.ONLINE) } returns products
    every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L
    every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

    val result = service.getProductStatistics()

    assertEquals(10, result.productRanking.size)
    assertEquals(12L, result.productRanking.first().productId)
    assertEquals(100L * 12, result.productRanking.first().revenueCents)
    assertEquals(3L, result.productRanking.last().productId)
  }

  @Test
  fun `getProductStatistics handles empty products`() {
    every { productQueryApiService.findByStatus(ProductModelStatus.ONLINE) } returns emptyList()
    every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

    val result = service.getProductStatistics()

    assertEquals(0, result.totalProducts)
    assertEquals(0L, result.totalValueCents)
    assertEquals(0L, result.averagePriceCents)
    assertEquals(0, result.totalSales)
    assertEquals(0, result.totalFavorites)
    assertEquals(0, result.productRanking.size)
  }

  @Test
  fun `getProductDailySales fills missing dates with zero entry`() {
    val start = LocalDate.of(2025, 6, 1)
    val end = LocalDate.of(2025, 6, 3)
    every {
      productSalesStatisticsRepository.getProductDailySales(any(), any(), any())
    } returns
      listOf(
        ProductDailySalesResult(date = "2025-06-01", productId = 1L, productName = "怡宝", salesVolume = 3L, revenue = 600L),
      )

    val result = service.getProductDailySales(start, end)

    assertEquals(3, result.size)
    assertEquals("2025-06-01", result[0].date)
    assertEquals(1L, result[0].productId)
    assertEquals(600L, result[0].revenueCents)
    // 无销售日期 → 占位
    assertEquals("无销售", result[1].productName)
    assertEquals(0, result[1].salesVolume)
    assertEquals(0L, result[1].revenueCents)
  }

  @Test
  fun `getAllProducts returns product infos`() {
    val infos = listOf(ProductInfo(productId = 1L, productName = "怡宝"))
    every { productSalesStatisticsRepository.getAllProductIdsAndNames() } returns infos

    assertEquals(infos, service.getAllProducts())
  }
}
