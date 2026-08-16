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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

/**
 * 订单评价写入服务单元测试(F-ORD-007,PRD 04-order.md §R-EVL-01):
 * 仅已完成订单可评、一单一评、越权拦截、评分实时计入送水工统计。
 */
class ReviewMutationServiceImplTest :
  FunSpec({
    var reviewRepository: ReviewRepository = mockk()
    var deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: ReviewMutationServiceImpl = mockk()

    fun completedOrder(deliveryWorkerId: Long? = 1L) =
      OrderModel(
        id = 1L,
        userId = 100L,
        status = OrderModelStatus.COMPLETED,
        deliveryWorkerId = deliveryWorkerId,
      )

    fun reviewRequest(
      rating: Int = 5,
      comment: String? = "服务很好",
    ) = CreateReviewRequest(orderId = 1L, rating = rating, comment = comment, isAnonymous = false)

    beforeTest {
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
    test("create review updates worker statistics") {
      every { orderQueryService.getOrderById(1L) } returns completedOrder()
      every { reviewRepository.existsByOrderId(1L) } returns false
      val statistics = DeliveryWorkerStatisticsModel(deliveryWorkerId = 1L)
      every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(1L) } returns statistics

      val review = service.createReview(100L, reviewRequest(rating = 5))

      review.orderId shouldBe 1L
      review.rating shouldBe 5
      review.deliveryWorkerId shouldBe 1L
      // 评分实时计入:totalReviews=1、averageRating=5.00
      statistics.totalReviews shouldBe 1
      statistics.averageRating shouldBe BigDecimal("5.00")
      verify(exactly = 1) { deliveryWorkerStatisticsRepository.save(statistics) }
      verify(exactly = 1) {
        bizLogger.logOrderOperation(orderId = 1L, operation = "ORDER_REVIEWED", result = "SUCCESS", userId = 100L, additionalData = any())
      }
    }
    test("create review initializes statistics row") {
      every { orderQueryService.getOrderById(1L) } returns completedOrder()
      every { reviewRepository.existsByOrderId(1L) } returns false
      every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(1L) } returns null

      service.createReview(100L, reviewRequest(rating = 3))

      verify(exactly = 1) {
        deliveryWorkerStatisticsRepository.save(match { it.deliveryWorkerId == 1L && it.totalReviews == 1 })
      }
    }
    test("create review rejects non completed order") {
      listOf(
        OrderModelStatus.PENDING_PAYMENT,
        OrderModelStatus.PENDING_DISPATCH,
        OrderModelStatus.PENDING_DELIVERY,
        OrderModelStatus.DELIVERING,
        OrderModelStatus.CANCELLED,
      ).forEach { status ->
        every { orderQueryService.getOrderById(1L) } returns
          OrderModel(id = 1L, userId = 100L, status = status)
        withClue("status=$status 应被拦截") { shouldThrow<BadRequestException> { service.createReview(100L, reviewRequest()) } }
      }
    }
    test("create review rejects duplicate") {
      every { orderQueryService.getOrderById(1L) } returns completedOrder()
      every { reviewRepository.existsByOrderId(1L) } returns true

      shouldThrow<BadRequestException> { service.createReview(100L, reviewRequest()) }
    }
    test("create review rejects non owner") {
      every { orderQueryService.getOrderById(1L) } returns completedOrder()

      shouldThrow<BadRequestException> { service.createReview(999L, reviewRequest()) }
    }
    test("create review rejects order without delivery worker") {
      every { orderQueryService.getOrderById(1L) } returns completedOrder(deliveryWorkerId = null)
      every { reviewRepository.existsByOrderId(1L) } returns false

      shouldThrow<BadRequestException> { service.createReview(100L, reviewRequest()) }
    }
    test("create review enforces rating bounds") {
      every { orderQueryService.getOrderById(1L) } returns completedOrder()
      every { reviewRepository.existsByOrderId(1L) } returns false

      shouldThrow<IllegalArgumentException> { service.createReview(100L, reviewRequest(rating = 6)) }
      true shouldBe true
    }
  })
