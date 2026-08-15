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
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 支付超时事件处理器单元测试:以系统身份记录超时操作。
 */
class PaymentTimeoutHandlerTest {
  private lateinit var orderOperationService: OrderOperationService
  private lateinit var handler: PaymentTimeoutHandler

  @BeforeEach
  fun setUp() {
    orderOperationService = mockk(relaxed = true)
    handler = PaymentTimeoutHandler(orderOperationService)
  }

  @Test
  @DisplayName("getSupportedEventType 返回 PAYMENT_TIMEOUT")
  fun `supported event type is payment timeout`() {
    assertEquals(DomainEventType.PAYMENT_TIMEOUT, handler.getSupportedEventType())
  }

  @Test
  @DisplayName("记录 SYSTEM 身份的超时取消操作")
  fun `records system timeout operation`() {
    val event = DomainEvent(id = 1L, eventType = "PAYMENT_TIMEOUT", payload = """{"orderId": 1001, "userId": 2001}""")

    handler.handle(event)

    verify(exactly = 1) {
      orderOperationService.recordOperation(
        orderId = 1001,
        operationType = OrderOperationType.PAYMENT_TIMEOUT,
        operatorType = OperatorType.SYSTEM,
        description = "订单支付超时，自动取消",
      )
    }
  }

  @Test
  @DisplayName("非法 payload 解析失败后重新抛出")
  fun `malformed payload rethrows`() {
    val event = DomainEvent(id = 2L, eventType = "PAYMENT_TIMEOUT", payload = "bad")

    assertFailsWith<Exception> { handler.handle(event) }
  }
}
