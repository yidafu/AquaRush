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

package dev.yidafu.aqua.admin.review.resolvers

import dev.yidafu.aqua.api.service.ReviewService
import dev.yidafu.aqua.common.dto.ReviewResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerRankingResponse as GraphQLDeliveryWorkerRankingResponse
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatisticsResponse as GraphQLDeliveryWorkerStatisticsResponse

/**
 * 管理端评价查询解析器
 * 提供评价系统的管理功能，包括查询所有评价、配送员统计等
 */
// @AdminService
@Controller
class AdminReviewQueryResolver(
  private val reviewService: ReviewService,
) {
  val logger = LoggerFactory.getLogger(AdminReviewQueryResolver::class.java)

  /**
   * 查询所有评价（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  fun reviews(
    deliveryWorkerId: Long? = null,
    minRating: Int? = null,
    maxRating: Int? = null,
    dateFrom: LocalDateTime? = null,
    dateTo: LocalDateTime? = null,
    userId: Long? = null,
    page: Int = 0,
    size: Int = 20,
  ): Page<ReviewResponse> {
    return reviewService.getReviewsWithFilters(
      deliveryWorkerId = deliveryWorkerId,
      minRating = minRating,
      maxRating = maxRating,
      dateFrom = dateFrom,
      dateTo = dateTo,
      userId = userId,
      page = page,
      size = size,
    )
  }

  /**
   * 获取配送员统计数据（管理员功能）
   */
//  @PreAuthorize("hasRole('ADMIN')")
  @QueryMapping("deliveryWorkerStatistics")
  fun deliveryWorkerStatistics(
    @Argument deliveryWorkerId: Long,
  ): GraphQLDeliveryWorkerStatisticsResponse {
    logger.info("deliveryWorkerStatistics($deliveryWorkerId)")
    // For now, return empty statistics since we need to implement the actual data retrieval
    return GraphQLDeliveryWorkerStatisticsResponse(
      averageRating = java.math.BigDecimal.ZERO,
      deliveryWorkerId = deliveryWorkerId,
      fiveStarReviews = 0L,
      fourStarReviews = 0L,
      lastUpdated = java.time.LocalDateTime.now(),
      oneStarReviews = 0L,
      ratingDistribution = emptyMap(),
      threeStarReviews = 0L,
      totalReviews = 0L,
      twoStarReviews = 0L,
      workerName = "配送员$deliveryWorkerId",
    )
  }

  /**
   * 获取配送员排行榜（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  fun deliveryWorkerRanking(
    sortBy: String = "rating",
    minReviews: Int = 1,
    page: Int = 0,
    size: Int = 20,
  ): Page<GraphQLDeliveryWorkerRankingResponse> {
    return reviewService.getDeliveryWorkerRanking(
      sortBy = sortBy,
      minReviews = minReviews,
      page = page,
      size = size,
    )
  }

  /**
   * 获取配送员的评价列表（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  fun deliveryWorkerReviews(
    deliveryWorkerId: Long,
    page: Int = 0,
    size: Int = 10,
  ): Page<ReviewResponse> {
    return reviewService.getDeliveryWorkerReviews(
      deliveryWorkerId = deliveryWorkerId,
      page = page,
      size = size,
    )
  }
}
