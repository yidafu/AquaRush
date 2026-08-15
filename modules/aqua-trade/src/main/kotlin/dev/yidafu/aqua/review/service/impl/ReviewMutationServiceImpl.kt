/**
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

import dev.yidafu.aqua.api.dto.CreateReviewRequest
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.review.ReviewMutationService
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import dev.yidafu.aqua.review.domain.repository.ReviewRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 订单评价写入服务实现(F-ORD-007,PRD 04-order.md §R-EVL-01):
 * - 一单一评:同一订单仅一次评价(unique order_id + 显式查重);
 * - 仅「已完成」订单可评价;评价对象=本次配送的送水工(服务端按订单解析);
 * - 评分 1-5 星、评论 ≤500 字(模型 init 兜底校验);
 * - 评价不可修改;评分实时计入送水工统计(delivery_worker_statistics)。
 */
@Service
class ReviewMutationServiceImpl(
  private val reviewRepository: ReviewRepository,
  private val deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository,
  private val orderQueryService: OrderQueryService,
  private val bizLogger: BizLogger,
) : ReviewMutationService {
  private val logger = LoggerFactory.getLogger(ReviewMutationServiceImpl::class.java)

  @Transactional
  override fun createReview(
    userId: Long,
    input: CreateReviewRequest,
  ): ReviewModel {
    // 1. 订单校验:存在、已完成、属于当前用户
    val order = orderQueryService.getOrderById(input.orderId)
    if (order.userId != userId) {
      throw BadRequestException("无权评价此订单")
    }
    if (order.status != OrderModelStatus.COMPLETED) {
      throw BadRequestException("仅已完成的订单可评价")
    }

    // 2. 评价对象=本次配送的送水工;无配送员的异常数据拒绝评价
    val deliveryWorkerId = input.deliveryWorkerId ?: order.deliveryWorkerId
    if (deliveryWorkerId == null) {
      throw BadRequestException("订单缺少配送员信息，无法评价")
    }

    // 3. 一单一评(R-EVL-01):重复评价拦截
    if (reviewRepository.existsByOrderId(input.orderId)) {
      throw BadRequestException("该订单已评价")
    }

    // 4. 保存评价(评分 1-5/评论 ≤500 由模型 init 校验兜底)
    val review =
      reviewRepository.save(
        ReviewModel(
          orderId = input.orderId,
          userId = userId,
          deliveryWorkerId = deliveryWorkerId,
          rating = input.rating,
          comment = input.comment,
          isAnonymous = input.isAnonymous,
        ),
      )

    // 5. 评分实时计入送水工统计(upsert)
    val statistics =
      deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(deliveryWorkerId)
        ?: DeliveryWorkerStatisticsModel(deliveryWorkerId = deliveryWorkerId)
    statistics.updateStatistics(input.rating)
    deliveryWorkerStatisticsRepository.save(statistics)

    // 业务日志(F-LOG-003 ORDER 域):订单评价
    bizLogger.logOrderOperation(
      orderId = input.orderId,
      operation = "ORDER_REVIEWED",
      result = "SUCCESS",
      userId = userId,
      additionalData =
        mapOf(
          "rating" to input.rating,
          "deliveryWorkerId" to deliveryWorkerId,
          "anonymous" to input.isAnonymous,
        ),
    )

    logger.info("Order {} reviewed by user {} with rating {}", input.orderId, userId, input.rating)
    return review
  }
}
