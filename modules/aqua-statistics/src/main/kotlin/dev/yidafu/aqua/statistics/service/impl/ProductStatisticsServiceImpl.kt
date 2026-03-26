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

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.ProductFavoriteStat
import dev.yidafu.aqua.common.graphql.generated.ProductSalesStat
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import org.springframework.stereotype.Service

/**
 * 商品统计服务
 */
@Service
class ProductStatisticsServiceImpl(
  private val productRepository: ProductRepository,
  private val productFavoriteRepository: ProductFavoriteRepository,
) {
  /**
   * 获取商品统计信息
   */
  fun getProductStatistics(): ProductStatistics {
    val allProducts = productRepository.findAll()
    val onlineProducts = allProducts.filter { it.status == ProductModelStatus.ONLINE }
    val offlineProducts = allProducts.filter { it.status == ProductModelStatus.OFFLINE }
    val lowStockThreshold = 10 // Default threshold
    val lowStockProducts = allProducts.filter { it.stock <= lowStockThreshold }

    val totalValue = allProducts.sumOf { it.price }
    val averagePrice = if (allProducts.isNotEmpty()) totalValue / allProducts.size else 0L
    val totalSales = allProducts.sumOf { it.salesVolume }

    // Get product ranking by sales volume
    val productRanking: List<ProductSalesStat> = allProducts
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

    // Get favorite statistics
    val allFavorites = productFavoriteRepository.findAll()
    val totalFavorites = allFavorites.size
    val favoriteStats: List<ProductFavoriteStat> = allFavorites
      .groupBy { it.productId }
      .map { (productId, favorites) ->
        val product = allProducts.find { it.id == productId }
        ProductFavoriteStat(
          productId = productId,
          productName = product?.name ?: "未知商品",
          favoriteCount = favorites.size.toLong(),
        )
      }
      .sortedByDescending { it.favoriteCount }
      .take(10)

    return ProductStatistics(
      totalProducts = allProducts.size,
      onlineProducts = onlineProducts.size,
      offlineProducts = offlineProducts.size,
      lowStockProducts = lowStockProducts.size,
      totalValue = totalValue,
      averagePrice = averagePrice,
      totalSales = totalSales,
      productRanking = productRanking,
      favoriteStatistics = favoriteStats,
      totalFavorites = totalFavorites,
    )
  }
}