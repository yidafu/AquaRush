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

package dev.yidafu.aqua.review.service.impl

import dev.yidafu.aqua.api.service.OrderService
import dev.yidafu.aqua.api.service.ReviewService
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.dto.CreateReviewRequest
import dev.yidafu.aqua.common.dto.DeliveryWorkerStatisticsResponse
import dev.yidafu.aqua.common.dto.ReviewResponse
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.OrderReviewCheckResponse
import dev.yidafu.aqua.common.graphql.generated.Review
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import dev.yidafu.aqua.review.domain.repository.ReviewRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerRankingResponse as GraphQLDeliveryWorkerRankingResponse

@Service
class ReviewServiceImpl(
  private val reviewRepository: ReviewRepository,
  private val statisticsRepository: DeliveryWorkerStatisticsRepository,
  private val orderRepository: OrderRepository,
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val orderService: OrderService,
) : ReviewService {
  private val logger = LoggerFactory.getLogger(ReviewService::class.java)

  /**
   * 用户创建评价
   */
  @Transactional
  override fun createReview(
    userId: Long,
    request: CreateReviewRequest,
  ): ReviewResponse {
    // 1. 验证订单存在且属于当前用户
    val order =
      orderRepository.findById(request.orderId)
        .orElseThrow { NotFoundException("订单不存在: ${request.orderId}") }

    if (order.userId != userId) {
      throw BadRequestException("无权评价此订单")
    }

    // 2. 验证订单状态为已完成
    if (order.status != OrderStatus.COMPLETED) {
      throw BadRequestException("只能评价已完成的订单")
    }

    // 3. 验证配送员信息
    if (order.deliveryWorkerId == null || order.deliveryWorkerId != request.deliveryWorkerId) {
      throw BadRequestException("配送员信息不匹配")
    }

    // 4. 检查是否已经评价过
    if (reviewRepository.existsByOrderId(request.orderId)) {
      throw BadRequestException("该订单已经评价过了")
    }

    // 5. 验证配送员存在
    val deliveryWorker =
      deliveryWorkerRepository.findById(request.deliveryWorkerId)
        .orElseThrow { NotFoundException("配送员不存在: ${request.deliveryWorkerId}") }

    // 6. 创建评价
    val review =
      ReviewModel(
        orderId = request.orderId,
        userId = userId,
        deliveryWorkerId = request.deliveryWorkerId,
        rating = request.rating,
        comment = request.comment,
        isAnonymous = request.isAnonymous,
      )

    val savedReview = reviewRepository.save(review)

    // 7. 更新配送员统计数据
    updateDeliveryWorkerStatistics(request.deliveryWorkerId, request.rating)

    return ReviewResponse(
      reviewId = savedReview.id!!,
      orderId = savedReview.orderId,
      userId = if (savedReview.isAnonymous) null else savedReview.userId,
      deliveryWorkerId = savedReview.deliveryWorkerId,
      deliveryWorkerName = deliveryWorker.name,
      rating = savedReview.rating,
      comment = savedReview.comment,
      isAnonymous = savedReview.isAnonymous,
      createdAt = savedReview.createdAt,
    )
  }

  /**
   * 检查订单是否已评价
   */
  override fun checkOrderReview(
    userId: Long,
    orderId: Long,
  ): OrderReviewCheckResponse {
    // 验证订单存在且属于当前用户
    val order =
      orderRepository.findById(orderId)
        .orElseThrow { NotFoundException("订单不存在: $orderId") }

    if (order.userId != userId) {
      throw BadRequestException("无权查看此订单评价状态")
    }

    val review = reviewRepository.findByOrderId(orderId)
    return if (review != null) {
      OrderReviewCheckResponse(
        hasReviewed = true,
        review =
          Review(
            reviewId = review.id!!,
            orderId = review.orderId,
            userId = if (review.isAnonymous) null else review.userId,
            deliveryWorkerId = review.deliveryWorkerId,
            rating = review.rating,
            comment = review.comment,
            isAnonymous = review.isAnonymous,
            createdAt = review.createdAt,
            deliveryWorkerName = TODO(),
          ),
        canReview = false,
      )
    } else {
      OrderReviewCheckResponse(
        hasReviewed = false,
        canReview = order.status == OrderStatus.COMPLETED,
        review = null,
      )
    }
  }

  /**
   * 获取用户的评价历史
   */
  override fun getUserReviews(
    userId: Long,
    page: Int,
    size: Int,
  ): Page<ReviewResponse> {
    val pageable = PageRequest.of(page, size)
    val reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)

    return reviews.map { review ->
      val deliveryWorker = deliveryWorkerRepository.findById(review.deliveryWorkerId).orElse(null)
      ReviewResponse(
        reviewId = review.id!!,
        orderId = review.orderId,
        userId = if (review.isAnonymous) null else review.userId,
        deliveryWorkerId = review.deliveryWorkerId,
        deliveryWorkerName = deliveryWorker?.name,
        rating = review.rating,
        comment = review.comment,
        isAnonymous = review.isAnonymous,
        createdAt = review.createdAt,
      )
    }
  }

  /**
   * 获取配送员统计数据
   */
  override fun getDeliveryWorkerStatistics(deliveryWorkerId: Long): DeliveryWorkerStatisticsResponse {
    logger.info("getDeliveryWorkerStatistics($deliveryWorkerId)")
    val statistics =
      statisticsRepository.findByDeliveryWorkerId(deliveryWorkerId)
        ?: DeliveryWorkerStatisticsModel(
          deliveryWorkerId = deliveryWorkerId,
          averageRating = BigDecimal.ZERO,
          totalReviews = 0,
          oneStarReviews = 0,
          twoStarReviews = 0,
          threeStarReviews = 0,
          fourStarReviews = 0,
          fiveStarReviews = 0,
          lastUpdated = LocalDateTime.now(),
        ).let {
          statisticsRepository.save(it)
        }

    val deliveryWorker = deliveryWorkerRepository.findById(deliveryWorkerId).orElse(null)

    return DeliveryWorkerStatisticsResponse(
      workerId = statistics.deliveryWorkerId,
      totalDeliveries = statistics.totalReviews, // 假设总配送量等于总评价数
      averageRating = statistics.averageRating,
      totalReviews = statistics.totalReviews,
      onTimeRate = 0.95, // TODO: 需要实现准时率计算逻辑
      positiveRatingCount = statistics.fourStarReviews + statistics.fiveStarReviews,
      negativeRatingCount = statistics.oneStarReviews + statistics.twoStarReviews,
    )
  }

  /**
   * 获取配送员的评价列表
   */
  override fun getDeliveryWorkerReviews(
    deliveryWorkerId: Long,
    page: Int,
    size: Int,
  ): Page<ReviewResponse> {
    val pageable = PageRequest.of(page, size)
    val reviews = reviewRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId, pageable)

    return reviews.map { review ->
      val deliveryWorker = deliveryWorkerRepository.findById(review.deliveryWorkerId).orElse(null)
      ReviewResponse(
        reviewId = review.id!!,
        orderId = review.orderId,
        userId = if (review.isAnonymous) null else review.userId,
        deliveryWorkerId = review.deliveryWorkerId,
        deliveryWorkerName = deliveryWorker?.name,
        rating = review.rating,
        comment = review.comment,
        isAnonymous = review.isAnonymous,
        createdAt = review.createdAt,
      )
    }
  }

  /**
   * 管理员查询评价列表
   */
  override fun getReviewsWithFilters(
    deliveryWorkerId: Long?,
    minRating: Int?,
    maxRating: Int?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    userId: Long?,
    page: Int,
    size: Int,
  ): Page<ReviewResponse> {
    val pageable = PageRequest.of(page, size)
    val reviews =
      reviewRepository.findReviewsWithFilters(
        deliveryWorkerId = deliveryWorkerId,
        minRating = minRating,
        maxRating = maxRating,
        dateFrom = dateFrom,
        dateTo = dateTo,
        userId = userId,
        pageable = pageable,
      )

    return reviews.map { review ->
      val deliveryWorker = deliveryWorkerRepository.findById(review.deliveryWorkerId).orElse(null)
      ReviewResponse(
        reviewId = review.id!!,
        orderId = review.orderId,
        userId = if (review.isAnonymous) null else review.userId,
        deliveryWorkerId = review.deliveryWorkerId,
        deliveryWorkerName = deliveryWorker?.name,
        rating = review.rating,
        comment = review.comment,
        isAnonymous = review.isAnonymous,
        createdAt = review.createdAt,
      )
    }
  }

  /**
   * 获取配送员排行榜
   */
  override fun getDeliveryWorkerRanking(
    sortBy: String,
    minReviews: Int,
    page: Int,
    size: Int,
  ): Page<GraphQLDeliveryWorkerRankingResponse> {
    val pageable = PageRequest.of(page, size)
    val statistics = statisticsRepository.findDeliveryWorkersRanking(sortBy, minReviews, pageable)

    return statistics.map<GraphQLDeliveryWorkerRankingResponse> { stat ->
      val deliveryWorker = deliveryWorkerRepository.findById(stat.deliveryWorkerId).orElse(null)
      GraphQLDeliveryWorkerRankingResponse(
        workerId = stat.deliveryWorkerId,
        workerName = deliveryWorker?.name,
        averageRating = stat.averageRating,
        totalReviews = stat.totalReviews.toLong(),
        positiveRatingPercentage = 0.0f,
//          if (stat.totalReviews > 0) {
//            ((stat.fourStarReviews + stat.fiveStarReviews).toDouble() / stat.totalReviews * 100)
//          } else {
//            0.0
//          },
      )
    }
  }

  /**
   * 更新配送员统计数据
   */
  @Transactional
  private fun updateDeliveryWorkerStatistics(
    deliveryWorkerId: Long,
    newRating: Int,
  ) {
    val statistics =
      statisticsRepository.findByDeliveryWorkerId(deliveryWorkerId)
        ?: DeliveryWorkerStatisticsModel(
          deliveryWorkerId = deliveryWorkerId,
          averageRating = BigDecimal.ZERO,
          totalReviews = 0,
          oneStarReviews = 0,
          twoStarReviews = 0,
          threeStarReviews = 0,
          fourStarReviews = 0,
          fiveStarReviews = 0,
          lastUpdated = LocalDateTime.now(),
        )

    statistics.updateStatistics(newRating)
    statisticsRepository.save(statistics)
  }
}
