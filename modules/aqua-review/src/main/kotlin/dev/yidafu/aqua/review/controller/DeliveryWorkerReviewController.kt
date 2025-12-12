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

package dev.yidafu.aqua.review.controller

import dev.yidafu.aqua.api.common.ApiResponse
import dev.yidafu.aqua.api.common.PagedResponse
import dev.yidafu.aqua.review.dto.DeliveryWorkerStatisticsResponse
import dev.yidafu.aqua.review.dto.ReviewResponse
import dev.yidafu.aqua.review.service.ReviewService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/delivery-workers")
class DeliveryWorkerReviewController(
  private val reviewService: ReviewService,
) {
  /**
   * 获取配送员评分统计
   */
  @GetMapping("/{workerId}/statistics")
  @PreAuthorize("hasAnyRole('DELIVERY_WORKER', 'ADMIN')")
  fun getDeliveryWorkerStatistics(
    @PathVariable workerId: Long,
    @RequestAttribute("userId") userId: Long,
    @RequestAttribute("userRole") userRole: String,
  ): ResponseEntity<ApiResponse<DeliveryWorkerStatisticsResponse>> {
    // 配送员只能查看自己的统计数据，管理员可以查看所有
    if (userRole == "DELIVERY_WORKER" && workerId != userId) {
      return ResponseEntity.status(403).body(
        ApiResponse(
          success = false,
          message = "无权查看其他配送员的统计数据",
          code = 403,
        ),
      )
    }

    val statistics = reviewService.getDeliveryWorkerStatistics(workerId)
    return ResponseEntity.ok(
      ApiResponse(
        success = true,
        message = "查询成功",
        data = statistics,
        code = 200,
      ),
    )
  }

  /**
   * 获取配送员评价列表
   */
  @GetMapping("/{workerId}/reviews")
  @PreAuthorize("hasAnyRole('DELIVERY_WORKER', 'ADMIN')")
  fun getDeliveryWorkerReviews(
    @PathVariable workerId: Long,
    @RequestAttribute("userId") userId: Long,
    @RequestAttribute("userRole") userRole: String,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
  ): ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> {
    // 配送员只能查看自己的评价，管理员可以查看所有
    if (userRole == "DELIVERY_WORKER" && workerId != userId) {
      return ResponseEntity.status(403).body(
        ApiResponse(
          success = false,
          message = "无权查看其他配送员的评价",
          code = 403,
        ),
      )
    }

    val reviews = reviewService.getDeliveryWorkerReviews(workerId, page, size)
    val pagedResponse =
      PagedResponse(
        content = reviews.content,
        page = reviews.number,
        size = reviews.size,
        totalElements = reviews.totalElements,
        totalPages = reviews.totalPages,
        first = reviews.isFirst,
        last = reviews.isLast,
      )
    return ResponseEntity.ok(
      ApiResponse(
        success = true,
        message = "查询成功",
        data = pagedResponse,
        code = 200,
      ),
    )
  }

  /**
   * 获取当前配送员评分统计（简化接口，配送员专用于查看自己）
   */
  @GetMapping("/me/statistics")
  @PreAuthorize("hasRole('DELIVERY_WORKER')")
  fun getMyStatistics(
    @RequestAttribute("userId") workerId: Long,
  ): ResponseEntity<ApiResponse<DeliveryWorkerStatisticsResponse>> {
    val statistics = reviewService.getDeliveryWorkerStatistics(workerId)
    return ResponseEntity.ok(
      ApiResponse(
        success = true,
        message = "查询成功",
        data = statistics,
        code = 200,
      ),
    )
  }

  /**
   * 获取当前配送员评价列表（简化接口，配送员专用于查看自己）
   */
  @GetMapping("/me/reviews")
  @PreAuthorize("hasRole('DELIVERY_WORKER')")
  fun getMyReviews(
    @RequestAttribute("userId") workerId: Long,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
  ): ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> {
    val reviews = reviewService.getDeliveryWorkerReviews(workerId, page, size)
    val pagedResponse =
      PagedResponse(
        content = reviews.content,
        page = reviews.number,
        size = reviews.size,
        totalElements = reviews.totalElements,
        totalPages = reviews.totalPages,
        first = reviews.isFirst,
        last = reviews.isLast,
      )
    return ResponseEntity.ok(
      ApiResponse(
        success = true,
        message = "查询成功",
        data = pagedResponse,
        code = 200,
      ),
    )
  }
}
