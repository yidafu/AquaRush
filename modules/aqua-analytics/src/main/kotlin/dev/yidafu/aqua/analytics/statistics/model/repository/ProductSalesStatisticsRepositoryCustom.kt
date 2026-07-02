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

package dev.yidafu.aqua.analytics.statistics.model.repository

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
