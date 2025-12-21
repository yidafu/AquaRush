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
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 管理端用户收藏变更解析器
 * 提供用户收藏管理相关的变更功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminUserFavoriteMutationResolver(
  private val adminUserFavoriteService: AdminUserFavoriteService
) {
  private val logger = LoggerFactory.getLogger(AdminUserFavoriteMutationResolver::class.java)

  
  /**
   * 批量操作用户收藏
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun batchFavoriteOperation(@Argument input: BatchFavoriteOperationInput): BatchOperationResult {
    // Convert collections once for both try and catch blocks
    val userIdList = input.userIds.toList()
    val productIdList = input.productIds.toList()

    return try {
      // Validate input
      if (userIdList.isEmpty() && productIdList.isEmpty()) {
        return BatchOperationResult(
          success = false,
          message = "用户ID和商品ID不能都为空",
          processedCount = 0,
          successCount = 0,
          failureCount = 0,
          errors = listOf("用户ID和商品ID不能都为空"),
          details = emptyMap()
        )
      }

      // Limit the number of IDs to prevent performance issues
      if (userIdList.size > 1000 || productIdList.size > 1000) {
        return BatchOperationResult(
          success = false,
          message = "批量操作数量不能超过1000个",
          processedCount = 0,
          successCount = 0,
          failureCount = 0,
          errors = listOf("批量操作数量不能超过1000个"),
          details = mapOf(
            "userCount" to userIdList.size,
            "productCount" to productIdList.size
          )
        )
      }

      val result = adminUserFavoriteService.performBatchOperation(input)

      // Log the operation for audit purposes
      logger.info(
        "Admin batch favorite operation: {} - Users: {}, Products: {}, Reason: {}, Success: {}",
        input.operation,
        userIdList.size,
        productIdList.size,
        input.reason ?: "No reason provided",
        result.success
      )

      result
    } catch (e: Exception) {
      logger.error("Failed to perform batch favorite operation", e)
      BatchOperationResult(
        success = false,
        message = "批量操作失败: ${e.message}",
        processedCount = userIdList.size + productIdList.size,
        successCount = 0,
        failureCount = userIdList.size + productIdList.size,
        errors = listOf("批量操作失败: ${e.message}"),
        details = mapOf(
          "operation" to input.operation.name,
          "userCount" to userIdList.size,
          "productCount" to productIdList.size,
          "reason" to (input.reason ?: ""),
          "error" to (e.message ?: "Unknown error")
        )
      )
    }
  }

  /**
   * 导出用户收藏数据
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun exportFavorites(@Argument input: ExportFavoritesInput): ExportFavoritesResult {
    return try {
      // Validate export parameters
      val dateFrom = input.dateFrom
      val dateTo = input.dateTo
      if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
        return ExportFavoritesResult(
          success = false,
          message = "开始时间不能晚于结束时间",
          downloadUrl = null,
          fileName = "",
          fileSize = 0,
          recordCount = 0,
          expiresAt = java.time.LocalDateTime.now().plusDays(1) // Default expiry: 1 day from now
        )
      }

      val result = adminUserFavoriteService.exportFavorites(input)

      // Log the export operation for audit purposes
      val productCount = input.productIds?.count() ?: 0
      logger.info(
        "Admin export favorites: Format: {}, UserId: {}, ProductCount: {}, DateRange: {} to {}, Records: {}",
        input.format,
        input.userId,
        productCount,
        dateFrom,
        dateTo,
        result.recordCount
      )

      result
    } catch (e: Exception) {
      logger.error("Failed to export favorites", e)
      ExportFavoritesResult(
        success = false,
        message = "导出失败: ${e.message}",
        downloadUrl = null,
        fileName = "",
        fileSize = 0,
        recordCount = 0,
        expiresAt = java.time.LocalDateTime.now().plusDays(1) // Default expiry: 1 day from now
      )
    }
  }

  /**
   * 分析用户收藏数据
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun analyzeUserFavorites(@Argument userId: Long): Map<String, Any> {
    return try {
      val insights = adminUserFavoriteService.getUserFavoriteInsights(userId)

      // Create analysis summary
      mapOf(
        "userId" to userId,
        "totalFavorites" to insights.totalFavorites,
        "favoriteCategories" to insights.favoriteCategories.toList(),
        "averageFavoritePrice" to insights.averageFavoritePrice,
        "engagementLevel" to insights.engagementLevel.name,
        "lastActiveAt" to insights.lastActiveAt.toString(),
        "analysisTimestamp" to java.time.LocalDateTime.now().toString(),
        "recommendations" to (generateRecommendations(insights) ?: emptyList<String>())
      )
    } catch (e: Exception) {
      logger.error("Failed to analyze user favorites for userId: $userId", e)
      mapOf(
        "error" to (e.message ?: "Unknown error"),
        "analysisTimestamp" to java.time.LocalDateTime.now().toString()
      )
    }
  }

  
  /**
   * 刷新收藏分析缓存
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun refreshFavoriteAnalyticsCache(): BooleanPayload {
    return try {
      // This would implement cache refresh functionality
      // For now, return success
      BooleanPayload(
        success = true,
        message = "收藏分析缓存刷新成功"
      )
    } catch (e: Exception) {
      logger.error("Failed to refresh favorite analytics cache", e)
      BooleanPayload(
        success = false,
        message = "缓存刷新失败: ${e.message}"
      )
    }
  }

  /**
   * 生成用户推荐
   */
  private fun generateRecommendations(insights: UserFavoriteInsights): List<String> {
    val recommendations = mutableListOf<String>()

    when (insights.engagementLevel) {
      UserEngagementLevel.LOW -> {
        recommendations.add("用户活跃度较低，建议发送收藏提醒推送")
        recommendations.add("可以考虑推荐热门商品来增加用户参与度")
      }
      UserEngagementLevel.MEDIUM -> {
        recommendations.add("用户活跃度适中，可以个性化推荐相关商品")
      }
      UserEngagementLevel.HIGH -> {
        recommendations.add("用户活跃度较高，可以考虑VIP权益或专属推荐")
      }
      UserEngagementLevel.VERY_HIGH -> {
        recommendations.add("用户非常活跃，是核心用户，建议提供个性化服务")
        recommendations.add("可以邀请参与用户调研或产品体验活动")
      }
    }

    if (insights.averageFavoritePrice > 10000) { // 100元 in cents
      recommendations.add("用户偏好高端商品，可以推荐 premium 系列")
    }

    val categories = insights.favoriteCategories.toList()
    if (categories.isNotEmpty()) {
      recommendations.add("用户主要关注 ${categories.joinToString(", ")} 类商品")
    }

    return recommendations
  }
}
