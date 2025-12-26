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

package dev.yidafu.aqua.admin.product.controller

import dev.yidafu.aqua.api.service.ProductFavoriteService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.generated.ExportFavoritesInput
import dev.yidafu.aqua.common.graphql.generated.ExportFavoritesResult
import dev.yidafu.aqua.common.graphql.generated.ExportFormat
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 管理端用户收藏 HTTP 控制器
 * 提供用户收藏管理相关的 REST API，仅管理员可访问
 */
@AdminService
@RestController
@RequestMapping("/api/admin/product/favorites")
class AdminProductFavoriteController(
  private val productFavoriteService: ProductFavoriteService
) {
  private val logger = LoggerFactory.getLogger(AdminProductFavoriteController::class.java)

  /**
   * 导出用户收藏数据
   *
   * POST /api/admin/product/favorites/export
   *
   * @param request 导出请求参数
   * @return 导出结果，包含下载链接和文件信息
   */
  @PostMapping("/export")
  @PreAuthorize("hasRole('ADMIN')")
  fun exportFavorites(@RequestBody @Valid request: ExportFavoritesRequest): ResponseEntity<ExportFavoritesResult> {
    return try {
      // Validate export parameters
      val dateFrom = request.dateFrom
      val dateTo = request.dateTo
      if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
        return ResponseEntity.ok(
          ExportFavoritesResult(
            success = false,
            message = "开始时间不能晚于结束时间",
            downloadUrl = null,
            fileName = "",
            fileSize = 0,
            recordCount = 0,
            expiresAt = LocalDateTime.now().plusDays(1)
          )
        )
      }

      // Convert request to GraphQL input type
      val input = ExportFavoritesInput(
        dateFrom = dateFrom,
        dateTo = dateTo,
        format = request.format,
        includeProductInfo = request.includeProductInfo,
        includeUserInfo = request.includeUserInfo,
        productIds = request.productIds,
        userId = request.userId
      )

      val result = productFavoriteService.exportFavorites(input)

      // Log the export operation for audit purposes
      val productCount = request.productIds?.count() ?: 0
      logger.info(
        "Admin export favorites: Format: {}, UserId: {}, ProductCount: {}, DateRange: {} to {}, Records: {}",
        request.format,
        request.userId,
        productCount,
        dateFrom,
        dateTo,
        result.recordCount
      )

      ResponseEntity.ok(result)
    } catch (e: Exception) {
      logger.error("Failed to export favorites", e)
      ResponseEntity.ok(
        ExportFavoritesResult(
          success = false,
          message = "导出失败: ${e.message}",
          downloadUrl = null,
          fileName = "",
          fileSize = 0,
          recordCount = 0,
          expiresAt = LocalDateTime.now().plusDays(1)
        )
      )
    }
  }
}

/**
 * 导出收藏数据请求 DTO
 */
data class ExportFavoritesRequest(
  /**
   * 开始时间（可选）
   */
  val dateFrom: LocalDateTime? = null,

  /**
   * 结束时间（可选）
   */
  val dateTo: LocalDateTime? = null,

  /**
   * 导出格式（默认 CSV）
   */
  val format: ExportFormat = ExportFormat.CSV,

  /**
   * 是否包含商品信息（默认 true）
   */
  val includeProductInfo: Boolean = true,

  /**
   * 是否包含用户信息（默认 true）
   */
  val includeUserInfo: Boolean = true,

  /**
   * 指定商品 ID 列表（可选）
   */
  val productIds: List<Long>? = null,

  /**
   * 指定用户 ID（可选）
   */
  val userId: Long? = null
)
