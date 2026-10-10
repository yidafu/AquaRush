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

package dev.yidafu.aqua.payment.service

import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 管理员退款审核服务单元测试(R-PAY-02):
 * 通过 → 订单退款(ORDER_REFUND)+ 押金退还(DEPOSIT_REFUND)+ 订单流转「已退款」;
 * 失败 → FAILED + failureReason;驳回 → REJECTED 订单保持「已取消」;非法状态拦截。
 */
class RefundReviewServiceTest :
  FunSpec({
    var paymentRefundRepository: PaymentRefundRepository = mockk()
    var paymentRepository: PaymentRepository = mockk()
    var paymentService: PaymentService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var orderOperationService: OrderOperationService = mockk()
    var depositMutationApiService: DepositMutationApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: RefundReviewService = mockk()

    fun request(
      status: RefundStatus = RefundStatus.PENDING,
      refundAmount: Long = 1900,
    ) = PaymentRefundModel(
      id = 5L,
      paymentId = 9L,
      orderId = 1L,
      userId = 100L,
      refundAmount = refundAmount,
      refundReason = "订单取消退款",
      status = status,
    )

    fun payment() = PaymentModel(id = 9L, orderId = 1L, userId = 100L, amount = 1900L, transactionId = "WX-TXN-1")

    fun order(status: OrderModelStatus = OrderModelStatus.CANCELLED) =
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 100L,
        amountCents = 1200,
        deliveryFeeCents = 200,
        depositCents = 500,
        status = status,
      )

    beforeTest {
      paymentRefundRepository = mockk(relaxed = true)
      paymentRepository = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      orderMutationService = mockk(relaxed = true)
      orderOperationService = mockk(relaxed = true)
      depositMutationApiService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service =
        RefundReviewService(
          paymentRefundRepository,
          paymentRepository,
          paymentService,
          orderQueryService,
          orderMutationService,
          orderOperationService,
          depositMutationApiService,
          bizLogger,
        )
      every { paymentRefundRepository.save(any()) } answers { firstArg() }
    }

    test("approve executes order refund and deposit refund then completes") {
      every { paymentRefundRepository.findById(5L) } returns Optional.of(request())
      every { paymentRepository.findById(9L) } returns Optional.of(payment())
      every { orderQueryService.getOrderById(1L) } returns order()

      val saved = service.approveRefund(5L, adminId = 7L, adminNote = "核实无误", username = "admin1")

      saved.status shouldBe RefundStatus.COMPLETED
      saved.processedBy shouldBe 7L
      saved.refundedAt.shouldNotBeNull()
      // 订单退款(商品+配送费)ORDER_REFUND
      verify {
        paymentService.refund("WX-TXN-1", 1400, 1900, any(), RefundType.ORDER_REFUND, 5L)
      }
      // 押金退还 DEPOSIT_REFUND + 押金记录联动
      verify {
        paymentService.refund("WX-TXN-1", 500, 1900, any(), RefundType.DEPOSIT_REFUND)
      }
      verify { depositMutationApiService.markRefundedByOrder(1L, 7L, "订单取消自动退还") }
      // 订单流转「已退款」
      verify { orderMutationService.updateOrderStatus(1L, OrderModelStatus.REFUNDED) }
      verify(exactly = 1) { orderOperationService.recordOperation(any(), any(), any(), any(), any(), any()) }
    }

    test("approve without deposit only executes order refund") {
      every { paymentRefundRepository.findById(5L) } returns Optional.of(request(refundAmount = 1400))
      every { paymentRepository.findById(9L) } returns Optional.of(payment())
      every { orderQueryService.getOrderById(1L) } returns order().apply { depositCents = 0 }

      service.approveRefund(5L, adminId = 7L, adminNote = null, username = "admin1")

      verify(exactly = 1) { paymentService.refund("WX-TXN-1", 1400, 1400, any(), RefundType.ORDER_REFUND, 5L) }
      verify(exactly = 0) { depositMutationApiService.markRefundedByOrder(any(), any(), any()) }
    }

    test("refund failure marks FAILED with failureReason") {
      every { paymentRefundRepository.findById(5L) } returns Optional.of(request())
      every { paymentRepository.findById(9L) } returns Optional.of(payment())
      every { orderQueryService.getOrderById(1L) } returns order()
      every { paymentService.refund(any(), any(), any(), any(), any(), any()) } throws IllegalStateException("微信退款失败")

      val saved = service.approveRefund(5L, adminId = 7L, adminNote = null, username = "admin1")

      saved.status shouldBe RefundStatus.FAILED
      saved.failureReason shouldBe "微信退款失败"
      // 退款失败订单不流转「已退款」
      verify(exactly = 0) { orderMutationService.updateOrderStatus(any(), any()) }
    }

    test("approve rejected when status not pending/failed") {
      every { paymentRefundRepository.findById(5L) } returns Optional.of(request(status = RefundStatus.REJECTED))

      shouldThrow<BadRequestException> { service.approveRefund(5L, 7L, null, "admin1") }
    }

    test("reject marks REJECTED and keeps order cancelled") {
      every { paymentRefundRepository.findById(5L) } returns Optional.of(request())

      val saved = service.rejectRefund(5L, adminId = 7L, adminNote = "凭证不足", username = "admin1")

      saved.status shouldBe RefundStatus.REJECTED
      saved.processedBy shouldBe 7L
      verify(exactly = 0) { paymentService.refund(any(), any(), any(), any(), any(), any()) }
      verify(exactly = 0) { orderMutationService.updateOrderStatus(any(), any()) }
    }

    test("reject rejected when not pending") {
      every { paymentRefundRepository.findById(5L) } returns Optional.of(request(status = RefundStatus.COMPLETED))

      shouldThrow<BadRequestException> { service.rejectRefund(5L, 7L, null, "admin1") }
    }

    test("missing request throws") {
      every { paymentRefundRepository.findById(5L) } returns Optional.empty()

      shouldThrow<BadRequestException> { service.approveRefund(5L, 7L, null, "admin1") }
    }
  })
