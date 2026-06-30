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

package dev.yidafu.aqua.statistics.service.impl

import dev.yidafu.aqua.api.service.product.ProductFavoriteService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductDailySales
import dev.yidafu.aqua.common.graphql.generated.ProductSalesStat
import dev.yidafu.aqua.common.graphql.generated.ProductSalesTrend
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.statistics.model.repository.ProductInfo
import dev.yidafu.aqua.statistics.model.repository.ProductSalesStatisticsRepositoryCustom
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
) {
  /**
   * 获取商品统计信息
   */
  fun getProductStatistics(): ProductStatistics {
    val allProducts = productQueryApiService.findByStatus(ProductModelStatus.ONLINE)
    val onlineProducts = allProducts.filter { it.status == ProductModelStatus.ONLINE }
    val offlineProducts = allProducts.filter { it.status == ProductModelStatus.OFFLINE }
    val lowStockThreshold = 10 // Default threshold
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
            revenue = product.price * product.salesVolume,
          )
        }

    // Get favorite statistics using ProductFavoriteService
    val allFavoriteStats = productFavoriteService.getProductFavoriteStats()
    val totalFavorites = allFavoriteStats.sumOf { it.favoriteCount }

    return ProductStatistics(
      totalProducts = allProducts.size,
      onlineProducts = onlineProducts.size,
      offlineProducts = offlineProducts.size,
      lowStockProducts = lowStockProducts.size,
      totalValue = totalValue,
      averagePrice = averagePrice,
      totalSales = totalSales,
      productRanking = productRanking,
      favoriteStatistics = allFavoriteStats,
      totalFavorites = totalFavorites.toInt(),
    )
  }

  /**
   * 获取商品每日销量统计
   * @param startDate 开始日期
   * @param endDate 结束日期
   */
  fun getProductDailySales(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<ProductDailySales> {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    val results =
      productSalesStatisticsRepository.getProductDailySales(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        productId = null,
      )

    // Create a map of existing results by date
    val resultsByDate = results.groupBy { it.date }

    // Generate all dates in the range and fill in zeros for missing dates
    val allDates = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }.toList()

    return allDates.flatMap { date ->
      val dateStr = date.toString()
      val dayResults = resultsByDate[dateStr]
      if (dayResults.isNullOrEmpty()) {
        // No sales on this date, return a single entry with zero
        listOf(
          ProductDailySales(
            date = dateStr,
            productId = 0L,
            productName = "无销售",
            salesVolume = 0,
            revenue = 0L,
          ),
        )
      } else {
        dayResults.map { result ->
          ProductDailySales(
            date = result.date,
            productId = result.productId,
            productName = result.productName,
            salesVolume = result.salesVolume.toInt(),
            revenue = result.revenue,
          )
        }
      }
    }
  }

  /**
   * 获取商品销量趋势
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param productId 商品ID（可选，为空则返回所有商品的汇总趋势）
   */
  fun getProductSalesTrend(
    startDate: LocalDate,
    endDate: LocalDate,
    productId: Long?,
  ): ProductSalesTrend {
    val startDateTime = startDate.atStartOfDay()
    val endDateTime = endDate.atTime(LocalTime.MAX)

    val results =
      productSalesStatisticsRepository.getProductDailySales(
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        productId = productId,
      )

    // Create a map of existing results by date
    val resultsByDate = results.groupBy { it.date }

    // Generate all dates in the range and fill in zeros for missing dates
    val allDates = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }.toList()

    val dailySales =
      allDates.flatMap { date ->
        val dateStr = date.toString()
        val dayResults = resultsByDate[dateStr]
        if (dayResults.isNullOrEmpty()) {
          listOf(
            ProductDailySales(
              date = dateStr,
              productId = productId ?: 0L,
              productName = "无销售",
              salesVolume = 0,
              revenue = 0L,
            ),
          )
        } else {
          dayResults.map { result ->
            ProductDailySales(
              date = result.date,
              productId = result.productId,
              productName = result.productName,
              salesVolume = result.salesVolume.toInt(),
              revenue = result.revenue,
            )
          }
        }
      }

    val firstResult = results.firstOrNull()

    return ProductSalesTrend(
      productId = productId ?: firstResult?.productId,
      productName = if (productId != null) firstResult?.productName else null,
      dailySales = dailySales,
    )
  }

  /**
   * 获取所有商品列表（用于下拉选择）
   */
  fun getAllProducts(): List<ProductInfo> = productSalesStatisticsRepository.getAllProductIdsAndNames()
}
