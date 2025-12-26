/*
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

package dev.yidafu.aqua.admin.product.resolvers

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.generated.*
import dev.yidafu.aqua.api.service.ProductFavoriteService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 管理端用户收藏查询解析器
 * 提供用户收藏管理相关的查询功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminProductFavoriteQueryResolver(
  private val productFavoriteService: ProductFavoriteService
) {
  private val logger = LoggerFactory.getLogger(AdminProductFavoriteQueryResolver::class.java)

  /**
   * 获取全部商品的收藏统计数据
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun allProductsFavoriteStats(): AllProductsFavoriteStats {
    return try {
      productFavoriteService.getAllProductsFavoriteStats()
    } catch (e: Exception) {
      logger.error("Failed to get all products favorite stats", e)
      throw RuntimeException("获取全部商品收藏统计失败: ${e.message}")
    }
  }

  /**
   * 按收藏数量排序的分页产品列表
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun productsByFavorites(
    @Argument page: Int? = 0,
    @Argument size: Int? = 20,
    @Argument minFavorites: Int? = null
  ): ProductFavoritePage {
    return try {
      productFavoriteService.getProductsByFavorites(page ?: 0, size ?: 20, minFavorites)
    } catch (e: Exception) {
      logger.error("Failed to get products by favorites", e)
      throw RuntimeException("获取收藏商品列表失败: ${e.message}")
    }
  }
}
