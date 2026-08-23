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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * 配送超时事件处理器单元测试:当前仅记录日志,payload 含 Long 订单号(与全系统 Long Snowflake 主键一致)。
 */
class DeliveryTimeoutHandlerTest :
  FunSpec({
    val handler = DeliveryTimeoutHandler()

    test("supported event type is delivery timeout") {
      handler.getSupportedEventType() shouldBe DomainEventType.DELIVERY_TIMEOUT
    }
    test("valid payload is processed") {
      val event =
        DomainEvent(
          id = 1L,
          eventType = "DELIVERY_TIMEOUT",
          payload = """{"orderId": 1001, "orderNumber": "ORD-001", "deliveryWorkerId": 3}""",
        )

      handler.handle(event)
    }
    test("invalid order id rethrows") {
      val event =
        DomainEvent(id = 2L, eventType = "DELIVERY_TIMEOUT", payload = """{"orderId": "not-a-number", "orderNumber": "ORD-001"}""")

      shouldThrow<Exception> { handler.handle(event) }
    }
  })
