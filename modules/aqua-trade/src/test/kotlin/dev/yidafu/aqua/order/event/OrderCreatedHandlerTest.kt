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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

/**
 * 订单创建事件处理器单元测试:用户下单/管理员代客下单分别记录操作。
 */
class OrderCreatedHandlerTest :
  FunSpec({
    var orderOperationService: OrderOperationService = mockk()
    var handler: OrderCreatedHandler = mockk()

    beforeTest {
      orderOperationService = mockk(relaxed = true)
      handler = OrderCreatedHandler(orderOperationService)
    }
    test("supported event type is order created") {
      handler.getSupportedEventType() shouldBe DomainEventType.ORDER_CREATED
    }
    test("user created order records user operation") {
      val event = DomainEvent(id = 1L, eventType = "ORDER_CREATED", payload = """{"orderId": 1001, "userId": 2001}""")

      handler.handle(event)

      verify(exactly = 1) {
        orderOperationService.recordOperation(
          orderId = 1001,
          operationType = OrderOperationType.ORDER_CREATED,
          operatorType = OperatorType.USER,
          operatorId = 2001,
          description = "用户创建订单",
        )
      }
    }
    test("admin created order records admin operation") {
      // 大于 Int 范围的 JSON 数值由 Jackson 解析为 Long,命中 as? Long 分支
      val event =
        DomainEvent(
          id = 2L,
          eventType = "ORDER_CREATED",
          payload = """{"orderId": 1002, "userId": 2002, "adminId": 3000000000}""",
        )

      handler.handle(event)

      verify(exactly = 1) {
        orderOperationService.recordOperation(
          orderId = 1002,
          operationType = OrderOperationType.ORDER_CREATED,
          operatorType = OperatorType.ADMIN,
          operatorId = 3000000000,
          description = "管理员/配送员创建订单",
        )
      }
    }
    test("malformed payload rethrows") {
      val event = DomainEvent(id = 3L, eventType = "ORDER_CREATED", payload = "not-json")

      shouldThrow<Exception> { handler.handle(event) }
    }
  })
