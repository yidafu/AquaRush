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

import dev.yidafu.aqua.analytics.statistics.StatisticsTime
import dev.yidafu.aqua.analytics.statistics.domain.repository.DailyProductFactRepository
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductInfo
import dev.yidafu.aqua.analytics.statistics.domain.repository.ProductSalesStatisticsRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.service.StatisticsSnapshotCoverageService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductDailySales
import dev.yidafu.aqua.common.graphql.generated.ProductSalesStat
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.common.graphql.generated.StockWarningItem
import dev.yidafu.aqua.common.graphql.generated.StockWarningStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

/**
 * 商品统计服务
 */
@Service
class ProductStatisticsServiceImpl(
  private val productQueryApiService: ProductQueryApiService,
  private val productFavoriteService: ProductFavoriteService,
  private val productSalesStatisticsRepository: ProductSalesStatisticsRepositoryCustom,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
  private val dailyProductFactRepository: DailyProductFactRepository,
  private val snapshotCoverageService: StatisticsSnapshotCoverageService,
) {
  /**
   * 获取商品统计信息
   */
  fun getProductStatistics(): ProductStatistics {
    val allProducts = productQueryApiService.findByStatus(ProductModelStatus.ONLINE)
    val onlineProducts = allProducts.filter { it.status == ProductModelStatus.ONLINE }
    val offlineProducts = allProducts.filter { it.status == ProductModelStatus.OFFLINE }
    // 低库存阈值由系统参数提供(F-SYS-003,R-PRO-02),缺省 10
    val lowStockThreshold = systemConfigQueryApiService.getLongValue(SystemSettingKeys.LOW_STOCK_THRESHOLD, 10L)
    val lowStockProducts = allProducts.filter { (it.stock ?: 0) <= lowStockThreshold }

    val totalValue = allProducts.sumOf { it.price }
    val averagePrice = if (allProducts.isNotEmpty()) totalValue / allProducts.size else 0L
    val totalSales = allProducts.sumOf { it.salesVolume }

    // Get product ranking by sales volume
    val productRanking: List<ProductSalesStat> =
      allProducts
        .filter { it.status == ProductModelStatus.ONLINE }
        .sortedByDescending { it.salesVolume }
        .take(10)
        .map { product ->
          ProductSalesStat(
            productId = product.id ?: 0L,
            productName = product.name,
            salesVolume = product.salesVolume,
            revenueCents = product.price * product.salesVolume,
          )
        }

    // Get favorite statistics using ProductFavoriteService
    val allFavoriteStats = productFavoriteService.getProductFavoriteStats()
    val totalFavorites = allFavoriteStats.sumOf { it.favoriteCount }

    // 库存预警明细(F-STA-006,G39):库存 ≤ 阈值(与 lowStockProducts 计数同源同口径,R-PRO-02);
    // 缺货(库存=0)置顶、其余按库存升序;状态:缺货 OUT_OF_STOCK / 预警 WARNING
    val stockWarningItems: List<StockWarningItem> =
      allProducts
        .filter { (it.stock ?: 0) <= lowStockThreshold }
        .sortedWith(compareBy({ it.stock ?: 0 }, { it.id ?: 0L }))
        .map { product ->
          StockWarningItem(
            productId = product.id ?: 0L,
            productName = product.name,
            specification = product.specification,
            currentStock = product.stock ?: 0,
            threshold = lowStockThreshold.toInt(),
            status =
              if ((product.stock ?: 0) == 0) {
                StockWarningStatus.OUT_OF_STOCK
              } else {
                StockWarningStatus.WARNING
              },
          )
        }

    return ProductStatistics(
      totalProducts = allProducts.size,
      onlineProducts = onlineProducts.size,
      offlineProducts = offlineProducts.size,
      lowStockProducts = lowStockProducts.size,
      totalValueCents = totalValue,
      averagePriceCents = averagePrice,
      totalSales = totalSales,
      productRanking = productRanking,
      favoriteStatistics = allFavoriteStats,
      totalFavorites = totalFavorites.toInt(),
      stockWarningItems = stockWarningItems,
    )
  }

  /**
   * 获取商品每日销量统计
   * 两段合并:昨日及以前读 daily_product_facts(已完成口径在查询时组合),今日/未覆盖日期走实时 SQL。
   * @param startDate 开始日期
   * @param endDate 结束日期
   */
  fun getProductDailySales(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<ProductDailySales> {
    val today = LocalDate.now(StatisticsTime.STATS_ZONE)
    val yesterday = today.minusDays(1)
    val historicalEnd = minOf(endDate, yesterday)

    // 快照段:COMPLETED 状态过滤在查询时组合;商品名易变,查询时批量取名
    val factDates = snapshotCoverageService.coveredDates(startDate, historicalEnd)
    val factRows = mutableMapOf<Pair<LocalDate, Long>, Pair<Long, Long>>() // (date, productId) -> (quantity, revenue)
    if (factDates.isNotEmpty()) {
      dailyProductFactRepository
        .findByStatDateBetween(factDates.min(), factDates.max())
        .filter { it.status == OrderModelStatus.COMPLETED.name }
        .forEach { fact ->
          val key = fact.statDate to fact.productId
          val current = factRows[key]
          factRows[key] = (current?.first ?: 0L) + fact.quantity to (current?.second ?: 0L) + fact.revenueCents
        }
    }

    // 实时段:未覆盖历史日期 + 今日
    val liveResults =
      snapshotCoverageService
        .uncoveredRanges(startDate, endDate, factDates)
        .flatMap { (rangeStart, rangeEnd) ->
          productSalesStatisticsRepository.getProductDailySales(
            startDateTime = rangeStart.atStartOfDay(),
            endDateTime = rangeEnd.atTime(LocalTime.MAX),
            productId = null,
          )
        }

    // 批量取商品名(快照段事实表只存 product_id;实时段已含名)
    val factProductIds = factRows.keys.map { it.second }.distinct()
    val productNames =
      if (factProductIds.isEmpty()) {
        emptyMap()
      } else {
        productQueryApiService.findByIds(factProductIds).associate { it.id!! to it.name }
      }

    // 合并:快照段优先,实时段补齐(按 (date, productId) 去重)
    val merged = mutableMapOf<Pair<LocalDate, Long>, Triple<String, Long, Long>>()
    factRows.forEach { (key, counts) ->
      merged[key] = Triple(productNames[key.second] ?: "未知商品", counts.first, counts.second)
    }
    liveResults.forEach { result ->
      val date = LocalDate.parse(result.date)
      val key = date to result.productId
      if (merged[key] == null) {
        merged[key] = Triple(result.productName, result.salesVolume.toLong(), result.revenue)
      }
    }

    // 生成范围内所有日期,缺失日期补「无销售」
    val allDates = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }.toList()

    return allDates.flatMap { date ->
      val dayResults =
        merged.entries
          .filter { it.key.first == date }
          .sortedByDescending { it.value.third }
          .map { (key, value) ->
            ProductDailySales(
              date = date.toString(),
              productId = key.second,
              productName = value.first,
              salesVolume = value.second.toInt(),
              revenueCents = value.third,
            )
          }
      if (dayResults.isEmpty()) {
        // No sales on this date, return a single entry with zero
        listOf(
          ProductDailySales(
            date = date.toString(),
            productId = 0L,
            productName = "无销售",
            salesVolume = 0,
            revenueCents = 0L,
          ),
        )
      } else {
        dayResults
      }
    }
  }

  /**
   * 获取所有商品列表（用于下拉选择）
   */
  fun getAllProducts(): List<ProductInfo> = productSalesStatisticsRepository.getAllProductIdsAndNames()
}
