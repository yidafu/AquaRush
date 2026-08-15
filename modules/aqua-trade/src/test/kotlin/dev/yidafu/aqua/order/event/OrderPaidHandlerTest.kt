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
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
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
 * 订单支付成功事件处理器单元测试:待配送订单触发派单事件,其他状态跳过。
 */
class OrderPaidHandlerTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var orderOperationService: OrderOperationService
  private lateinit var simplifiedEventPublishService: SimplifiedEventPublishService
  private lateinit var handler: OrderPaidHandler

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    orderOperationService = mockk(relaxed = true)
    simplifiedEventPublishService = mockk(relaxed = true)
    handler = OrderPaidHandler(orderRepository, orderOperationService, simplifiedEventPublishService)
  }

  private fun order(status: OrderModelStatus) = OrderModel(id = 1L, orderNo = "ORD-001", userId = 100L, addressId = 7L, status = status)

  private fun paidEvent() = DomainEvent(id = 1L, eventType = "ORDER_PAID", payload = """{"orderId": 1}""")

  @Test
  @DisplayName("待配送订单:记录 ORDER_PAID 并发布配送分配事件")
  fun `pending delivery order records and publishes assignment`() {
    every { orderRepository.findById(1L) } returns Optional.of(order(OrderModelStatus.PENDING_DELIVERY))

    handler.handle(paidEvent())

    verify(exactly = 1) {
      orderOperationService.recordOperation(
        orderId = 1L,
        operationType = OrderOperationType.ORDER_PAID,
        operatorType = OperatorType.USER,
        operatorId = 100L,
        description = "订单支付成功",
      )
    }
    verify(exactly = 1) {
      simplifiedEventPublishService.publishDomainEvent(
        eventType = "ORDER_DELIVERY_ASSIGNMENT",
        aggregateId = "1",
        eventData =
          mapOf(
            "orderId" to "1",
            "orderNumber" to "ORD-001",
            "userId" to "100",
            "addressId" to "7",
          ),
      )
    }
  }

  @Test
  @DisplayName("非待配送状态:跳过记录与派单")
  fun `non pending delivery order is skipped`() {
    every { orderRepository.findById(1L) } returns Optional.of(order(OrderModelStatus.PENDING_DISPATCH))

    handler.handle(paidEvent())

    verify(exactly = 0) { orderOperationService.recordOperation(any(), any(), any(), any(), any()) }
    verify(exactly = 0) { simplifiedEventPublishService.publishDomainEvent(any(), any(), any()) }
  }

  @Test
  @DisplayName("订单不存在:抛 IllegalStateException 触发重试")
  fun `missing order throws`() {
    every { orderRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<IllegalStateException> { handler.handle(paidEvent()) }
  }

  @Test
  @DisplayName("非法 payload 解析失败后重新抛出")
  fun `malformed payload rethrows`() {
    val event = DomainEvent(id = 2L, eventType = "ORDER_PAID", payload = "oops")

    assertFailsWith<Exception> { handler.handle(event) }
  }
}
