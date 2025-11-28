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
import dev.yidafu.aqua.review.dto.CreateReviewRequest
import dev.yidafu.aqua.review.dto.OrderReviewCheckResponse
import dev.yidafu.aqua.review.dto.ReviewResponse
import dev.yidafu.aqua.review.service.ReviewService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reviews")
class ReviewController(
    private val reviewService: ReviewService
) {

    /**
     * 用户提交评价
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    fun createReview(
        @RequestAttribute("userId") userId: Long,
        @Valid @RequestBody request: CreateReviewRequest
    ): ResponseEntity<ApiResponse<ReviewResponse>> {
        val review = reviewService.createReview(userId, request)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "评价成功",
                data = review,
                code = 200
            )
        )
    }

    /**
     * 检查订单是否已评价
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('USER')")
    fun checkOrderReview(
        @RequestAttribute("userId") userId: Long,
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<OrderReviewCheckResponse>> {
        val checkResult = reviewService.checkOrderReview(userId, orderId)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "查询成功",
                data = checkResult,
                code = 200
            )
        )
    }

    /**
     * 获取用户评价历史
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    fun getUserReviews(
        @RequestAttribute("userId") userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> {
        val reviews = reviewService.getUserReviews(userId, page, size)
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
}
