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

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 配送超时事件处理器单元测试:当前仅记录日志,payload 需含 UUID 订单号。
 */
class DeliveryTimeoutHandlerTest {
  private val handler = DeliveryTimeoutHandler()

  @Test
  @DisplayName("getSupportedEventType 返回 DELIVERY_TIMEOUT")
  fun `supported event type is delivery timeout`() {
    assertEquals(DomainEventType.DELIVERY_TIMEOUT, handler.getSupportedEventType())
  }

  @Test
  @DisplayName("合法 payload 处理成功(UUID 订单号)")
  fun `valid payload is processed`() {
    val uuid = UUID.randomUUID().toString()
    val event =
      DomainEvent(
        id = 1L,
        eventType = "DELIVERY_TIMEOUT",
        payload = """{"orderId": "$uuid", "orderNumber": "ORD-001"}""",
      )

    handler.handle(event)
  }

  @Test
  @DisplayName("非法 UUID 订单号解析失败后重新抛出")
  fun `invalid uuid rethrows`() {
    val event = DomainEvent(id = 2L, eventType = "DELIVERY_TIMEOUT", payload = """{"orderId": "not-a-uuid", "orderNumber": "ORD-001"}""")

    assertFailsWith<Exception> { handler.handle(event) }
  }
}
