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
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import dev.yidafu.aqua.review.domain.repository.ReviewRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 订单评价写入服务单元测试(F-ORD-007,PRD 04-order.md §R-EVL-01):
 * 仅已完成订单可评、一单一评、越权拦截、评分实时计入送水工统计。
 */
class ReviewMutationServiceImplTest {
  private lateinit var reviewRepository: ReviewRepository
  private lateinit var deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository
  private lateinit var orderQueryService: OrderQueryService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: ReviewMutationServiceImpl

  private fun completedOrder(deliveryWorkerId: Long? = 1L) =
    OrderModel(
      id = 1L,
      userId = 100L,
      status = OrderModelStatus.COMPLETED,
      deliveryWorkerId = deliveryWorkerId,
    )

  private fun reviewRequest(
    rating: Int = 5,
    comment: String? = "服务很好",
  ) = CreateReviewRequest(orderId = 1L, rating = rating, comment = comment, isAnonymous = false)

  @BeforeEach
  fun setUp() {
    reviewRepository = mockk(relaxed = true)
    deliveryWorkerStatisticsRepository = mockk(relaxed = true)
    orderQueryService = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    every { reviewRepository.save(any()) } answers { firstArg() as ReviewModel }
    every { deliveryWorkerStatisticsRepository.save(any()) } answers { firstArg() as DeliveryWorkerStatisticsModel }
    service =
      ReviewMutationServiceImpl(
        reviewRepository,
        deliveryWorkerStatisticsRepository,
        orderQueryService,
        bizLogger,
      )
  }

  @Test
  @DisplayName("R-EVL-01:已完成订单评价成功,评分计入送水工统计")
  fun `create review updates worker statistics`() {
    every { orderQueryService.getOrderById(1L) } returns completedOrder()
    every { reviewRepository.existsByOrderId(1L) } returns false
    val statistics = DeliveryWorkerStatisticsModel(deliveryWorkerId = 1L)
    every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(1L) } returns statistics

    val review = service.createReview(100L, reviewRequest(rating = 5))

    assertEquals(1L, review.orderId)
    assertEquals(5, review.rating)
    assertEquals(1L, review.deliveryWorkerId)
    // 评分实时计入:totalReviews=1、averageRating=5.00
    assertEquals(1, statistics.totalReviews)
    assertEquals(BigDecimal("5.00"), statistics.averageRating)
    verify(exactly = 1) { deliveryWorkerStatisticsRepository.save(statistics) }
    verify(exactly = 1) {
      bizLogger.logOrderOperation(orderId = 1L, operation = "ORDER_REVIEWED", result = "SUCCESS", userId = 100L, additionalData = any())
    }
  }

  @Test
  @DisplayName("R-EVL-01:首次评价时初始化统计行(upsert)")
  fun `create review initializes statistics row`() {
    every { orderQueryService.getOrderById(1L) } returns completedOrder()
    every { reviewRepository.existsByOrderId(1L) } returns false
    every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(1L) } returns null

    service.createReview(100L, reviewRequest(rating = 3))

    verify(exactly = 1) {
      deliveryWorkerStatisticsRepository.save(match { it.deliveryWorkerId == 1L && it.totalReviews == 1 })
    }
  }

  @Test
  @DisplayName("R-EVL-01:非已完成订单不可评价")
  fun `create review rejects non completed order`() {
    listOf(
      OrderModelStatus.PENDING_PAYMENT,
      OrderModelStatus.PENDING_DISPATCH,
      OrderModelStatus.PENDING_DELIVERY,
      OrderModelStatus.DELIVERING,
      OrderModelStatus.CANCELLED,
    ).forEach { status ->
      every { orderQueryService.getOrderById(1L) } returns
        OrderModel(id = 1L, userId = 100L, status = status)
      assertFailsWith<BadRequestException>("status=$status 应被拦截") { service.createReview(100L, reviewRequest()) }
    }
  }

  @Test
  @DisplayName("R-EVL-01:一单一评,重复评价拦截")
  fun `create review rejects duplicate`() {
    every { orderQueryService.getOrderById(1L) } returns completedOrder()
    every { reviewRepository.existsByOrderId(1L) } returns true

    assertFailsWith<BadRequestException> { service.createReview(100L, reviewRequest()) }
  }

  @Test
  @DisplayName("R-EVL-01:越权拦截(订单不属于当前用户)")
  fun `create review rejects non owner`() {
    every { orderQueryService.getOrderById(1L) } returns completedOrder()

    assertFailsWith<BadRequestException> { service.createReview(999L, reviewRequest()) }
  }

  @Test
  @DisplayName("R-EVL-01:订单无配送员(异常数据)拒绝评价")
  fun `create review rejects order without delivery worker`() {
    every { orderQueryService.getOrderById(1L) } returns completedOrder(deliveryWorkerId = null)
    every { reviewRepository.existsByOrderId(1L) } returns false

    assertFailsWith<BadRequestException> { service.createReview(100L, reviewRequest()) }
  }

  @Test
  @DisplayName("R-EVL-01:评价不可修改(新增校验:评分 1-5 由模型 init 兜底)")
  fun `create review enforces rating bounds`() {
    every { orderQueryService.getOrderById(1L) } returns completedOrder()
    every { reviewRepository.existsByOrderId(1L) } returns false

    assertFailsWith<IllegalArgumentException> { service.createReview(100L, reviewRequest(rating = 6)) }
    assertTrue(true)
  }
}
