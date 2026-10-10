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

import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyProductFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyUserFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductDailySalesResult
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductInfo
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductSalesRankingResult
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductSalesStatisticsRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductStatisticsAggregate
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductStockInfo
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductFavoriteStatVo
import dev.yidafu.aqua.common.graphql.generated.StockWarningStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

/**
 * 商品统计服务单元测试(PRD F-STA/R-PRO-02: 全量商品口径 P1-4、排行已完成订单营收 P2-7、低库存/每日销量补零、两段合并)
 */
class ProductStatisticsServiceImplTest :
  FunSpec({
    var productQueryApiService: ProductQueryApiService = mockk()
    var productFavoriteService: ProductFavoriteService = mockk()
    var productSalesStatisticsRepository: ProductSalesStatisticsRepositoryCustom = mockk()
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var dailyProductFactRepository: DailyProductFactRepository = mockk()
    var dailyUserFactRepository: DailyUserFactRepository = mockk()
    var snapshotCoverageService: StatisticsSnapshotCoverageService = mockk()
    var service: ProductStatisticsServiceImpl = mockk()

    fun stubStatisticsAggregation(
      statusCounts: Map<ProductModelStatus, Long>,
      aggregate: ProductStatisticsAggregate,
      lowStockCount: Long,
      lowStockItems: List<ProductStockInfo>,
      ranking: List<ProductSalesRankingResult>,
    ) {
      every { productSalesStatisticsRepository.countProductsByStatus() } returns statusCounts
      every { productSalesStatisticsRepository.aggregateProductValue() } returns aggregate
      every { productSalesStatisticsRepository.countLowStockProducts(any()) } returns lowStockCount
      every { productSalesStatisticsRepository.findLowStockProducts(any(), any()) } returns lowStockItems
      every { productSalesStatisticsRepository.getProductSalesRanking(any()) } returns ranking
    }

    beforeTest {
      productQueryApiService = mockk(relaxed = true)
      productFavoriteService = mockk(relaxed = true)
      productSalesStatisticsRepository = mockk(relaxed = true)
      systemConfigQueryApiService = mockk(relaxed = true)
      dailyProductFactRepository = mockk(relaxed = true)
      dailyUserFactRepository = mockk(relaxed = true)
      snapshotCoverageService = StatisticsSnapshotCoverageService(dailyUserFactRepository)
      service =
        ProductStatisticsServiceImpl(
          productQueryApiService,
          productFavoriteService,
          productSalesStatisticsRepository,
          systemConfigQueryApiService,
          dailyProductFactRepository,
          snapshotCoverageService,
        )
    }
    test("getProductStatistics counts all statuses and low stock includes OFFLINE/OUT_OF_STOCK (P1-4)") {
      stubStatisticsAggregation(
        statusCounts =
          mapOf(
            ProductModelStatus.ONLINE to 2L,
            ProductModelStatus.OFFLINE to 1L,
            ProductModelStatus.OUT_OF_STOCK to 1L,
          ),
        aggregate = ProductStatisticsAggregate(totalProducts = 4L, totalValueCents = 900L, totalSales = 125L),
        lowStockCount = 3L,
        lowStockItems =
          listOf(
            ProductStockInfo(productId = 1L, productName = "缺货水", specification = "500ml", stock = 0),
            ProductStockInfo(productId = 3L, productName = "百岁山", specification = "500ml", stock = 3),
            // 下架商品库存≤阈值同样计入低库存预警(P1-4)
            ProductStockInfo(productId = 4L, productName = "下架低库存", specification = "500ml", stock = 10),
          ),
        ranking = emptyList(),
      )
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L
      every { productFavoriteService.getProductFavoriteStats() } returns
        listOf(
          ProductFavoriteStatVo(favoriteCount = 4L, productId = 1L, productName = "怡宝"),
          ProductFavoriteStatVo(favoriteCount = 2L, productId = 2L, productName = "农夫山泉"),
        )

      val result = service.getProductStatistics()

      // 商品总数 = 全量商品,不再等于在售数(P1-4)
      result.totalProducts shouldBe 4
      result.onlineProducts shouldBe 2
      result.offlineProducts shouldBe 1
      result.outOfStockProducts shouldBe 1
      // 低库存计数与明细均含 OFFLINE 商品
      result.lowStockProducts shouldBe 3
      result.totalValueCents shouldBe 900L
      result.averagePriceCents shouldBe 225L // 900 / 4
      result.totalSales shouldBe 125
      result.totalFavorites shouldBe 6
      result.favoriteStatistics.size shouldBe 2
      // 库存预警明细(缺货置顶 → 按库存升序)
      result.stockWarningItems.size shouldBe 3
      result.stockWarningItems[0].productName shouldBe "缺货水"
      result.stockWarningItems[0].status shouldBe StockWarningStatus.OUT_OF_STOCK
      result.stockWarningItems[1].productName shouldBe "百岁山"
      result.stockWarningItems[2].productName shouldBe "下架低库存"
      result.stockWarningItems[2].threshold shouldBe 10
    }
    test("getProductStatistics ranking revenue comes from completed order aggregation (P2-7)") {
      // 排行营收 = 已完成订单项金额合计,非 price × salesVolume 估算
      stubStatisticsAggregation(
        statusCounts = mapOf(ProductModelStatus.ONLINE to 3L),
        aggregate = ProductStatisticsAggregate(totalProducts = 3L, totalValueCents = 600L, totalSales = 30L),
        lowStockCount = 0L,
        lowStockItems = emptyList(),
        ranking =
          listOf(
            ProductSalesRankingResult(productId = 2L, productName = "农夫山泉", salesVolume = 50L, revenueCents = 15000L),
            ProductSalesRankingResult(productId = 1L, productName = "怡宝", salesVolume = 30L, revenueCents = 6000L),
          ),
      )
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L) } returns 10L
      every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

      val result = service.getProductStatistics()

      result.productRanking.size shouldBe 2
      result.productRanking[0].productId shouldBe 2L
      result.productRanking[0].productName shouldBe "农夫山泉"
      result.productRanking[0].salesVolume shouldBe 50
      result.productRanking[0].revenueCents shouldBe 15000L
      result.productRanking[1].revenueCents shouldBe 6000L
    }
    test("getProductStatistics handles empty products") {
      stubStatisticsAggregation(
        statusCounts = emptyMap(),
        aggregate = ProductStatisticsAggregate(totalProducts = 0L, totalValueCents = 0L, totalSales = 0L),
        lowStockCount = 0L,
        lowStockItems = emptyList(),
        ranking = emptyList(),
      )
      every { productFavoriteService.getProductFavoriteStats() } returns emptyList()

      val result = service.getProductStatistics()

      result.totalProducts shouldBe 0
      result.onlineProducts shouldBe 0
      result.offlineProducts shouldBe 0
      result.outOfStockProducts shouldBe 0
      result.lowStockProducts shouldBe 0
      result.totalValueCents shouldBe 0L
      result.averagePriceCents shouldBe 0L
      result.totalSales shouldBe 0
      result.totalFavorites shouldBe 0
      result.productRanking.size shouldBe 0
      result.stockWarningItems.size shouldBe 0
    }
    test("getProductStatistics builds stock warning items sorted with out-of-stock first (G39)") {
      // G39: 库存预警明细 —— 库存≤阈值(含缺货),缺货(库存=0)置顶,按库存升序,阈值与状态正确
      stubStatisticsAggregation(
        statusCounts =
          mapOf(
            ProductModelStatus.ONLINE to 3L,
            ProductModelStatus.OUT_OF_STOCK to 1L,
          ),
        aggregate = ProductStatisticsAggregate(totalProducts = 4L, totalValueCents = 1100L, totalSales = 30L),
        lowStockCount = 3L,
        lowStockItems =
          listOf(
            ProductStockInfo(productId = 1L, productName = "缺货水", specification = "500ml", stock = 0),
            ProductStockInfo(productId = 2L, productName = "低库存水", specification = "500ml", stock = 3),
            ProductStockInfo(productId = 3L, productName = "边界水", specification = "500ml", stock = 10),
          ),
        ranking = emptyList(),
      )
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
    test("getProductDailySales reads historical segment from product facts (两段合并)") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 3)
      every { dailyUserFactRepository.findFirstByOrderByStatDateAsc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 1))
      every { dailyUserFactRepository.findFirstByOrderByStatDateDesc() } returns DailyUserFactModel(statDate = LocalDate.of(2025, 6, 3))
      every {
        dailyProductFactRepository.findByStatDateBetween(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 3))
      } returns
        listOf(
          DailyProductFactModel(
            statDate = LocalDate.of(2025, 6, 1),
            status = OrderModelStatus.COMPLETED.name,
            productId = 1L,
            quantity = 3L,
            revenueCents = 600L,
            orderCount = 1L,
          ),
          // 非 COMPLETED 状态不计入商品日销
          DailyProductFactModel(
            statDate = LocalDate.of(2025, 6, 1),
            status = OrderModelStatus.PENDING_DISPATCH.name,
            productId = 1L,
            quantity = 99L,
            revenueCents = 9999L,
            orderCount = 1L,
          ),
        )
      every { productQueryApiService.findByIds(listOf(1L)) } returns
        listOf(
          ProductModel().apply {
            this.id = 1L
            this.name = "怡宝"
          },
        )

      val result = service.getProductDailySales(start, end)

      result.size shouldBe 3
      result[0].date shouldBe "2025-06-01"
      result[0].productId shouldBe 1L
      result[0].productName shouldBe "怡宝"
      result[0].salesVolume shouldBe 3
      result[0].revenueCents shouldBe 600L
      // 无销售日期 → 占位
      result[1].productName shouldBe "无销售"
    }
    test("getAllProducts returns product infos") {
      val infos = listOf(ProductInfo(productId = 1L, productName = "怡宝"))
      every { productSalesStatisticsRepository.getAllProductIdsAndNames() } returns infos

      service.getAllProducts() shouldBe infos
    }
  })
