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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.review.resolvers

import dev.yidafu.aqua.client.review.resolvers.ClientReviewMutationResolver.Companion.MAX_COMMENT_LENGTH
import dev.yidafu.aqua.client.review.resolvers.ClientReviewMutationResolver.Companion.MAX_RATING
import dev.yidafu.aqua.client.review.resolvers.ClientReviewMutationResolver.Companion.MIN_RATING
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.review.dto.OrderReviewCheckResponse
import dev.yidafu.aqua.review.dto.ReviewResponse
import dev.yidafu.aqua.review.service.ReviewService
import org.springframework.data.domain.Page
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 客户端评价查询解析器
 * 提供用户评价查询功能，仅限用户查看自己的评价
 */
@ClientService
@Controller
class ClientReviewQueryResolver(
    private val reviewService: ReviewService
) {

    /**
     * 检查订单是否已评价
     */
    @PreAuthorize("isAuthenticated()")
    fun checkOrderReview(orderId: Long): OrderReviewCheckResponse {
        // 需要从认证上下文获取userId，这里暂时使用传入参数
        // 实际实现中应该从Spring Security Context获取
        return reviewService.checkOrderReview(getCurrentUserId(), orderId)
    }

    /**
     * 获取用户的评价历史
     */
    @PreAuthorize("isAuthenticated()")
    fun myReviews(page: Int = 0, size: Int = 20): Page<ReviewResponse> {
        return reviewService.getUserReviews(getCurrentUserId(), page, size)
    }

    /**
     * 获取配送员统计数据（公开信息）
     * 用户可以查看配送员的评分和统计，但不能查看敏感信息
     */
    @PreAuthorize("isAuthenticated()")
    fun deliveryWorkerPublicStatistics(deliveryWorkerId: Long): ReviewResponse {
        // 返回配送员的公开统计信息
        val statistics = reviewService.getDeliveryWorkerStatistics(deliveryWorkerId)
        return ReviewResponse(
            reviewId = 0L,
            orderId = 0L,
            userId = null, // 不返回用户信息
            deliveryWorkerId = statistics.deliveryWorkerId,
            deliveryWorkerName = statistics.workerName,
            rating = statistics.averageRating.toInt(),
            comment = null, // 不返回具体评论
            isAnonymous = true,
            createdAt = statistics.lastUpdated
        )
    }

    /**
     * 获取当前认证用户的ID
     * 在实际实现中应该从Spring Security Context获取
     */
    private fun getCurrentUserId(): Long {
        // TODO: 从Spring Security Context获取当前用户ID
        // 暂时返回占位符，实际实现需要：
        // val authentication = SecurityContextHolder.getContext().authentication
        // return (authentication.principal as UserDetails).id
        throw UnsupportedOperationException("需要从Spring Security Context获取用户ID")
    }

    /**
     * 评价相关常量
     */
    companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 5
        const val MAX_COMMENT_LENGTH = 500
    }
}