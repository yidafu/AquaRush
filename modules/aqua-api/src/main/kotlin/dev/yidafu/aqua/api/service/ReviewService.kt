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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.dto.*
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerRankingResponse
import dev.yidafu.aqua.common.graphql.generated.OrderReviewCheckResponse
import org.springframework.data.domain.Page
import java.time.LocalDateTime

/**
 * 评价服务接口
 */
interface ReviewService {
  /**
   * 用户创建评价
   */
  fun createReview(
    userId: Long,
    request: CreateReviewRequest,
  ): ReviewResponse

  /**
   * 检查订单是否已评价
   */
  fun checkOrderReview(
    userId: Long,
    orderId: Long,
  ): OrderReviewCheckResponse

  /**
   * 获取用户的评价历史
   */
  fun getUserReviews(
    userId: Long,
    page: Int = 0,
    size: Int = 20,
  ): Page<ReviewResponse>

  /**
   * 获取配送员统计数据
   */
  fun getDeliveryWorkerStatistics(deliveryWorkerId: Long): DeliveryWorkerStatisticsResponse

  /**
   * 获取配送员的评价列表
   */
  fun getDeliveryWorkerReviews(
    deliveryWorkerId: Long,
    page: Int = 0,
    size: Int = 10,
  ): Page<ReviewResponse>

  /**
   * 管理员查询评价列表
   */
  fun getReviewsWithFilters(
    deliveryWorkerId: Long? = null,
    minRating: Int? = null,
    maxRating: Int? = null,
    dateFrom: LocalDateTime? = null,
    dateTo: LocalDateTime? = null,
    userId: Long? = null,
    page: Int = 0,
    size: Int = 20,
  ): Page<ReviewResponse>

  /**
   * 获取配送员排行榜
   */
  fun getDeliveryWorkerRanking(
    sortBy: String = "rating",
    minReviews: Int = 1,
    page: Int = 0,
    size: Int = 20,
  ): Page<DeliveryWorkerRankingResponse>
}
