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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import java.time.LocalDateTime

/**
 * 商品销量统计 Repository 自定义接口
 */
interface ProductSalesStatisticsRepositoryCustom {
  /**
   * 按日期分组统计商品销量
   * @param startDateTime 开始时间
   * @param endDateTime 结束时间
   * @param productId 商品ID，可为空表示查询所有商品
   */
  fun getProductDailySales(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    productId: Long? = null,
  ): List<ProductDailySalesResult>

  /**
   * 获取所有商品列表（用于下拉选择）
   */
  fun getAllProductIdsAndNames(): List<ProductInfo>

  /**
   * 按状态分组的商品数（SQL 聚合，规则 13：不整表加载实体）。
   * 商品统计口径全量商品（F-STA-006）：OFFLINE/OUT_OF_STOCK 均计入总数，低库存预警同源（P1-4）。
   */
  fun countProductsByStatus(): Map<ProductModelStatus, Long>

  /**
   * 商品价值聚合（单行 SQL）：商品总数 / 总价 / 总销量，全状态口径。
   */
  fun aggregateProductValue(): ProductStatisticsAggregate

  /**
   * 低库存商品数（库存 ≤ 阈值，任意状态，含 OFFLINE/OUT_OF_STOCK —— R-PRO-02/P1-4）。
   */
  fun countLowStockProducts(threshold: Long): Long

  /**
   * 低库存商品明细（库存 ≤ 阈值，任意状态；按库存升序、缺货置顶；LIMIT 有界，规则 13）。
   */
  fun findLowStockProducts(
    threshold: Long,
    limit: Int,
  ): List<ProductStockInfo>

  /**
   * 商品销量排行（TOP N，已完成订单项聚合）：
   * 销量 = 已完成订单项数量合计、营收 = 已完成订单项金额合计，
   * 与「营收=已完成订单金额合计」全域口径及 productDailySales 同源（P2-7）。
   */
  fun getProductSalesRanking(limit: Int): List<ProductSalesRankingResult>
}

/**
 * 商品每日销量结果
 */
data class ProductDailySalesResult(
  val date: String,
  val productId: Long,
  val productName: String,
  val salesVolume: Long,
  val revenue: Long,
)

/**
 * 商品基本信息
 */
data class ProductInfo(
  val productId: Long,
  val productName: String,
)

/**
 * 商品价值聚合结果（全量商品，SQL 聚合）
 */
data class ProductStatisticsAggregate(
  val totalProducts: Long,
  val totalValueCents: Long,
  val totalSales: Long,
)

/**
 * 低库存商品信息（库存 ≤ 阈值，任意状态）
 */
data class ProductStockInfo(
  val productId: Long,
  val productName: String,
  val specification: String,
  val stock: Int,
)

/**
 * 商品销量排行项（已完成订单项聚合：销量/营收同源）
 */
data class ProductSalesRankingResult(
  val productId: Long,
  val productName: String,
  val salesVolume: Long,
  val revenueCents: Long,
)
