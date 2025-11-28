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
import dev.yidafu.aqua.review.dto.DeliveryWorkerRankingResponse
import dev.yidafu.aqua.review.dto.ReviewResponse
import dev.yidafu.aqua.review.service.ReviewService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
class AdminReviewController(
    private val reviewService: ReviewService
) {

    /**
     * 查询评价列表（支持多条件筛选）
     */
    @GetMapping
    fun getReviewsWithFilters(
        @RequestParam(required = false) deliveryWorkerId: Long?,
        @RequestParam(required = false) minRating: Int?,
        @RequestParam(required = false) maxRating: Int?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateFrom: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateTo: LocalDateTime?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> {
        val reviews = reviewService.getReviewsWithFilters(
            deliveryWorkerId = deliveryWorkerId,
            minRating = minRating,
            maxRating = maxRating,
            dateFrom = dateFrom,
            dateTo = dateTo,
            userId = userId,
            page = page,
            size = size
        )

        val pagedResponse = PagedResponse(
            content = reviews.content,
            page = reviews.number,
            size = reviews.size,
            totalElements = reviews.totalElements,
            totalPages = reviews.totalPages,
            first = reviews.isFirst,
            last = reviews.isLast
        )

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "查询成功",
                data = pagedResponse,
                code = 200
            )
        )
    }

    /**
     * 获取配送员评分排行榜
     */
    @GetMapping("/delivery-workers/ranking")
    fun getDeliveryWorkerRanking(
        @RequestParam(defaultValue = "rating") sortBy: String, // rating, reviews
        @RequestParam(defaultValue = "1") minReviews: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<DeliveryWorkerRankingResponse>>> {
        // 验证排序参数
        if (sortBy !in listOf("rating", "reviews")) {
            return ResponseEntity.badRequest().body(
                ApiResponse(
                    success = false,
                    message = "排序参数只支持 'rating' 或 'reviews'",
                    code = 400
                )
            )
        }

        val ranking = reviewService.getDeliveryWorkerRanking(
            sortBy = sortBy,
            minReviews = minReviews,
            page = page,
            size = size
        )

        val pagedResponse = PagedResponse(
            content = ranking.content,
            page = ranking.number,
            size = ranking.size,
            totalElements = ranking.totalElements,
            totalPages = ranking.totalPages,
            first = ranking.isFirst,
            last = ranking.isLast
        )

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "查询成功",
                data = pagedResponse,
                code = 200
            )
        )
    }

    /**
     * 获取指定配送员的评价统计
     */
    @GetMapping("/delivery-workers/{workerId}/statistics")
    fun getDeliveryWorkerStatistics(
        @PathVariable workerId: Long
    ): ResponseEntity<ApiResponse<Any>> {
        return try {
            val statistics = reviewService.getDeliveryWorkerStatistics(workerId)
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "查询成功",
                    data = statistics,
                    code = 200
                )
            )
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 获取评价统计概览
     */
    @GetMapping("/statistics/overview")
    fun getReviewStatisticsOverview(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        // 这里可以扩展更多的统计信息
        val overview = mutableMapOf<String, Any>()

        // 总体评价数量（可以后续扩展）
        overview["totalReviews"] = 0
        overview["averageRating"] = 0.0
        overview["totalDeliveryWorkers"] = 0

        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "查询成功",
                data = overview,
                code = 200
            )
        )
    }
}
