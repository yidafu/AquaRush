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
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertFailsWith

/**
 * 订单取消事件处理器单元测试:需退款时调用支付服务退款,退款失败不阻断。
 */
class OrderCancelledHandlerTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var paymentService: PaymentService
  private lateinit var orderOperationService: OrderOperationService
  private lateinit var handler: OrderCancelledHandler

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    paymentService = mockk(relaxed = true)
    orderOperationService = mockk(relaxed = true)
    handler = OrderCancelledHandler(orderRepository, paymentService, orderOperationService)
  }

  private fun order(amountCents: Long = 1200) =
    OrderModel(
      id = 1L,
      orderNo = "ORD-001",
      userId = 100L,
      amountCents = amountCents,
      status = dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus.CANCELLED,
    )

  @Test
  @DisplayName("需退款且有交易号:记录操作并调用退款")
  fun `refund is processed when requested`() {
    every { orderRepository.findById(1L) } returns Optional.of(order())
    every { paymentService.refund(any(), any(), any(), any()) } returns mapOf("refundId" to "REF-1")
    val event =
      DomainEvent(
        id = 1L,
        eventType = "ORDER_CANCELLED",
        payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
      )

    handler.handle(event)

    verify(exactly = 1) { orderOperationService.recordOperation(any(), any(), any(), any(), any()) }
    verify(exactly = 1) {
      paymentService.refund(
        transactionId = "WX-TXN-1",
        refundAmountCents = 1200,
        totalAmountCents = 1200,
        reason = any(),
      )
    }
  }

  @Test
  @DisplayName("需退款但无交易号:跳过退款")
  fun `refund is skipped without transaction id`() {
    every { orderRepository.findById(1L) } returns Optional.of(order())
    val event = DomainEvent(id = 2L, eventType = "ORDER_CANCELLED", payload = """{"orderId": 1, "shouldRefund": true}""")

    handler.handle(event)

    verify(exactly = 0) { paymentService.refund(any(), any(), any(), any()) }
    verify(exactly = 1) { orderOperationService.recordOperation(any(), any(), any(), any(), any()) }
  }

  @Test
  @DisplayName("无需退款:仅记录操作")
  fun `no refund records operation only`() {
    every { orderRepository.findById(1L) } returns Optional.of(order())
    val event = DomainEvent(id = 3L, eventType = "ORDER_CANCELLED", payload = """{"orderId": 1}""")

    handler.handle(event)

    verify(exactly = 0) { paymentService.refund(any(), any(), any(), any()) }
    verify(exactly = 1) { orderOperationService.recordOperation(any(), any(), any(), any(), any()) }
  }

  @Test
  @DisplayName("退款失败被吞掉,不阻断事件处理")
  fun `refund failure is swallowed`() {
    every { orderRepository.findById(1L) } returns Optional.of(order())
    every { paymentService.refund(any(), any(), any(), any()) } throws IllegalStateException("微信退款失败")
    val event =
      DomainEvent(
        id = 4L,
        eventType = "ORDER_CANCELLED",
        payload = """{"orderId": 1, "shouldRefund": true, "paymentTransactionId": "WX-TXN-1"}""",
      )

    handler.handle(event)

    verify(exactly = 1) { paymentService.refund(any(), any(), any(), any()) }
  }

  @Test
  @DisplayName("订单不存在:抛 IllegalStateException")
  fun `missing order throws`() {
    every { orderRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<IllegalStateException> {
      handler.handle(DomainEvent(id = 5L, eventType = "ORDER_CANCELLED", payload = """{"orderId": 1}"""))
    }
  }
}
