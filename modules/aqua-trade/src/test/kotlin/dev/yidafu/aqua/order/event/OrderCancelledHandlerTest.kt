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

package dev.yidafu.aqua.order.event

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 订单取消事件处理器单元测试(R-PAY-02 审核制):
 * 已支付取消生成 payment_refunds(PENDING) 申请,不直接执行微信退款;幂等防重复生成;
 * 金额口径 = 商品+配送费(缺陷 #5);退款失败由后台审核流程处理,事件处理本身不应失败。
 */
class OrderCancelledHandlerTest :
  FunSpec({
    var orderRepository: OrderRepository = mockk()
    var paymentRepository: PaymentRepository = mockk()
    var paymentRefundRepository: PaymentRefundRepository = mockk()
    var orderOperationService: OrderOperationService = mockk()
    var handler: OrderCancelledHandler = mockk()

    fun order(
      amountCents: Long = 1200,
      deliveryFeeCents: Long = 200,
    ) = OrderModel(
      id = 1L,
      orderNo = "ORD-001",
      userId = 100L,
      amountCents = amountCents,
      deliveryFeeCents = deliveryFeeCents,
      status = dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.CANCELLED,
    )

    fun payment() = PaymentModel(id = 9L, orderId = 1L, userId = 100L, amount = 1400L, transactionId = "WX-TXN-1")

    beforeTest {
      orderRepository = mockk(relaxed = true)
      paymentRepository = mockk(relaxed = true)
      paymentRefundRepository = mockk(relaxed = true)
      orderOperationService = mockk(relaxed = true)
      handler = OrderCancelledHandler(orderRepository, paymentRepository, paymentRefundRepository, orderOperationService)
      every { paymentRefundRepository.save(any()) } answers { firstArg() }
      every { paymentRefundRepository.findByOrderIdAndRefundType(any(), any()) } returns Optional.empty()
      every { paymentRepository.findByTransactionId("WX-TXN-1") } returns Optional.of(payment())
    }

    test("refund request PENDING created with goods+delivery amount, no wechat refund") {
      every { orderRepository.findById(1L) } returns Optional.of(order())
      val event =
        DomainEvent(
          id = 1L,
          eventType = "ORDER_CANCELLED",
          payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
        )

      handler.handle(event)

      // ORDER_CANCELLED + REFUND_INITIATED 两条时间线
      verify(exactly = 2) { orderOperationService.recordOperation(any(), any(), any(), any(), any(), any()) }
      verify(exactly = 1) {
        paymentRefundRepository.save(
          match {
            it.status == RefundStatus.PENDING &&
              it.refundType == RefundType.ORDER_REFUND &&
              it.refundAmount == 1400L &&
              it.paymentId == 9L &&
              it.orderId == 1L &&
              it.userId == 100L
          },
        )
      }
      // 时间线留痕:退款申请已生成
      verify(exactly = 1) {
        orderOperationService.recordOperation(
          orderId = 1L,
          operationType = dev.yidafu.aqua.common.domain.model.enums.OrderOperationType.REFUND_INITIATED,
          operatorType = any(),
          operatorId = any(),
          description = any(),
          extraData = any(),
        )
      }
    }

    test("duplicate event skips when refund request already exists") {
      every { orderRepository.findById(1L) } returns Optional.of(order())
      every { paymentRefundRepository.findByOrderIdAndRefundType(1L, RefundType.ORDER_REFUND) } returns
        Optional.of(
          dev.yidafu.aqua.common.domain.model.PaymentRefundModel(
            id = 5L,
            paymentId = 9L,
            orderId = 1L,
            userId = 100L,
            refundAmount = 1400L,
            status = RefundStatus.PENDING,
          ),
        )
      val event =
        DomainEvent(
          id = 2L,
          eventType = "ORDER_CANCELLED",
          payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
        )

      handler.handle(event)

      verify(exactly = 0) { paymentRefundRepository.save(any()) }
    }

    test("refund is skipped without transaction id") {
      every { orderRepository.findById(1L) } returns Optional.of(order())
      val event = DomainEvent(id = 3L, eventType = "ORDER_CANCELLED", payload = """{"orderId": 1, "shouldRefund": true}""")

      handler.handle(event)

      verify(exactly = 0) { paymentRefundRepository.save(any()) }
      verify(exactly = 1) { orderOperationService.recordOperation(any(), any(), any(), any(), any(), any()) }
    }

    test("no refund records cancel operation only") {
      every { orderRepository.findById(1L) } returns Optional.of(order())
      val event = DomainEvent(id = 4L, eventType = "ORDER_CANCELLED", payload = """{"orderId": 1}""")

      handler.handle(event)

      verify(exactly = 0) { paymentRefundRepository.save(any()) }
      // 仅 ORDER_CANCELLED 一条操作,无 REFUND_INITIATED
      verify(exactly = 1) { orderOperationService.recordOperation(any(), any(), any(), any(), any(), any()) }
    }

    test("zero refundable amount skips generation") {
      every { orderRepository.findById(1L) } returns Optional.of(order(amountCents = 0, deliveryFeeCents = 0))
      val event =
        DomainEvent(
          id = 5L,
          eventType = "ORDER_CANCELLED",
          payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
        )

      handler.handle(event)

      verify(exactly = 0) { paymentRefundRepository.save(any()) }
    }

    test("missing payment record skips generation with error log") {
      every { orderRepository.findById(1L) } returns Optional.of(order())
      every { paymentRepository.findByTransactionId(any()) } returns Optional.empty()
      every { paymentRepository.findByOrderId(1L) } returns Optional.empty()
      val event =
        DomainEvent(
          id = 6L,
          eventType = "ORDER_CANCELLED",
          payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
        )

      handler.handle(event)

      verify(exactly = 0) { paymentRefundRepository.save(any()) }
    }

    test("missing order throws") {
      every { orderRepository.findById(1L) } returns Optional.empty()

      shouldThrow<IllegalStateException> {
        handler.handle(DomainEvent(id = 7L, eventType = "ORDER_CANCELLED", payload = """{"orderId": 1}"""))
      }
    }

    test("amount equals goods plus delivery fee") {
      every { orderRepository.findById(1L) } returns Optional.of(order(amountCents = 900, deliveryFeeCents = 500))
      handler.handle(
        DomainEvent(
          id = 8L,
          eventType = "ORDER_CANCELLED",
          payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
        ),
      )

      val slot = io.mockk.slot<dev.yidafu.aqua.common.domain.model.PaymentRefundModel>()
      verify(exactly = 1) { paymentRefundRepository.save(capture(slot)) }
      slot.captured.refundAmount shouldBe 1400L
      slot.captured.status shouldBe RefundStatus.PENDING
    }
  })
