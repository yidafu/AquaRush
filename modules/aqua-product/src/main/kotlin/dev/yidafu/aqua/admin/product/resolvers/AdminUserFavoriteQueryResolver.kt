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
import dev.yidafu.aqua.product.service.AdminUserFavoriteService
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
class AdminUserFavoriteQueryResolver(
  private val adminUserFavoriteService: AdminUserFavoriteService
) {
  private val logger = LoggerFactory.getLogger(AdminUserFavoriteQueryResolver::class.java)

  /**
   * 获取用户收藏综合分析数据
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun userFavoriteAnalytics(): UserFavoriteAnalytics {
    return try {
      adminUserFavoriteService.getFavoriteAnalytics()
    } catch (e: Exception) {
      logger.error("Failed to get user favorite analytics", e)
      throw RuntimeException("获取用户收藏分析数据失败: ${e.message}")
    }
  }

  /**
   * 获取最受收藏的商品列表
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun mostFavoritedProducts(@Argument limit: Int? = 10): List<ProductFavoriteCount> {
    return try {
      adminUserFavoriteService.getMostFavoritedProducts(limit ?: 10)
    } catch (e: Exception) {
      logger.error("Failed to get most favorited products with limit: $limit", e)
      throw RuntimeException("获取最受收藏商品列表失败: ${e.message}")
    }
  }

  /**
   * 获取用户收藏趋势数据
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun favoritesTrend(@Argument days: Int? = 30): List<FavoriteTrendData> {
    return try {
      adminUserFavoriteService.getFavoritesTrend(days ?: 30)
    } catch (e: Exception) {
      logger.error("Failed to get favorites trend for days: $days", e)
      throw RuntimeException("获取用户收藏趋势数据失败: ${e.message}")
    }
  }

  /**
   * 获取有收藏的用户列表（汇总信息）
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun usersWithFavorites(@Argument input: AdminFavoriteListInput): UserFavoriteSummaryPage {
    return try {
      adminUserFavoriteService.getUsersWithFavorites(input)
    } catch (e: Exception) {
      logger.error("Failed to get users with favorites", e)
      throw RuntimeException("获取用户收藏汇总列表失败: ${e.message}")
    }
  }

  /**
   * 获取用户收藏详情（包含商品信息）
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun userFavoritesWithDetails(@Argument input: AdminFavoriteListInput): UserFavoriteWithProductPage {
    return try {
      adminUserFavoriteService.getUserFavoritesWithDetails(input)
    } catch (e: Exception) {
      logger.error("Failed to get user favorites with details", e)
      throw RuntimeException("获取用户收藏详情失败: ${e.message}")
    }
  }

  /**
   * 获取特定用户的收藏洞察数据
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun userFavoriteInsights(@Argument userId: Long): UserFavoriteInsights {
    return try {
      adminUserFavoriteService.getUserFavoriteInsights(userId)
    } catch (e: Exception) {
      logger.error("Failed to get user favorite insights for userId: $userId", e)
      throw RuntimeException("获取用户收藏洞察数据失败: ${e.message}")
    }
  }

  /**
   * 获取收藏商品统计数据
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun favoriteProductStats(@Argument productId: Long): FavoriteProductStats? {
    return try {
      // This would be implemented in the service to get specific product stats
      // For now, return null as placeholder
      null
    } catch (e: Exception) {
      logger.error("Failed to get favorite product stats for productId: $productId", e)
      throw RuntimeException("获取商品收藏统计数据失败: ${e.message}")
    }
  }

  /**
   * 获取收藏时间段统计
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun favoritePeriodStats(@Argument dateRange: DateRangeInput): Map<String, Any> {
    return try {
      // This would be implemented in the service to get period-specific stats
      // For now, return empty map as placeholder
      mapOf(
        "totalFavorites" to 0,
        "activeUsers" to 0,
        "averageFavoritesPerUser" to 0.0,
        "newUsers" to 0
      )
    } catch (e: Exception) {
      logger.error("Failed to get favorite period stats", e)
      throw RuntimeException("获取收藏时间段统计失败: ${e.message}")
    }
  }
}
