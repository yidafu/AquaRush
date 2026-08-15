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

package dev.yidafu.aqua.delivery.event

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 配送分配事件处理器单元测试:校验送水员存在并记录派单操作。
 */
class DeliveryAssignmentHandlerTest {
  private lateinit var orderService: OrderQueryService
  private lateinit var orderOperationService: OrderOperationService
  private lateinit var adminService: AdminService
  private lateinit var handler: DeliveryAssignmentHandler

  @BeforeEach
  fun setUp() {
    orderService = mockk(relaxed = true)
    orderOperationService = mockk(relaxed = true)
    adminService = mockk(relaxed = true)
    handler = DeliveryAssignmentHandler(orderService, orderOperationService, adminService)
  }

  private fun order() =
    OrderModel(
      id = 1L,
      orderNo = "ORD-001",
      userId = 100L,
      user = UserModel(id = 100L, nickname = "小明"),
    )

  private fun admin(
    id: Long?,
    realName: String? = "管理员A",
    username: String = "admin-a",
  ) = AdminModel(id = id, username = username, passwordHash = "x", realName = realName)

  private fun event() =
    DomainEvent(
      id = 1L,
      eventType = "DELIVERY_ASSIGNED",
      payload = """{"orderId": "1", "adminId": "2", "deliveryWorkerId": "3"}""",
    )

  @Test
  @DisplayName("getSupportedEventType 返回 DELIVERY_ASSIGNED")
  fun `supported event type is delivery assigned`() {
    assertEquals(DomainEventType.DELIVERY_ASSIGNED, handler.getSupportedEventType())
  }

  @Test
  @DisplayName("送水员存在:记录派单操作并包含双方姓名")
  fun `records assignment operation with names`() {
    every { orderService.getOrderById(1L) } returns order()
    every { adminService.findById(2L) } returns admin(id = 2L, realName = "管理员A")
    every { adminService.findById(3L) } returns admin(id = 3L, realName = "配送员B")

    handler.handle(event())

    verify(exactly = 1) {
      orderOperationService.recordOperation(
        1L,
        OrderOperationType.DELIVERY_ASSIGNED,
        dev.yidafu.aqua.common.domain.model.enums.OperatorType.ADMIN,
        operatorId = 2L,
        match { msg -> msg.contains("ORD-001") && msg.contains("管理员A") && msg.contains("配送员B") && msg.contains("小明") },
      )
    }
  }

  @Test
  @DisplayName("派单管理员不存在时使用默认文案")
  fun `falls back to default admin name`() {
    every { orderService.getOrderById(1L) } returns order()
    every { adminService.findById(2L) } returns null
    every { adminService.findById(3L) } returns admin(id = 3L, realName = null, username = "worker-c")

    handler.handle(event())

    verify(exactly = 1) {
      orderOperationService.recordOperation(
        1L,
        OrderOperationType.DELIVERY_ASSIGNED,
        dev.yidafu.aqua.common.domain.model.enums.OperatorType.ADMIN,
        operatorId = 2L,
        match { msg -> msg.contains("管理员") && msg.contains("worker-c") },
      )
    }
  }

  @Test
  @DisplayName("送水员不存在:抛 IllegalStateException 触发重试")
  fun `missing delivery worker throws`() {
    every { orderService.getOrderById(1L) } returns order()
    every { adminService.findById(2L) } returns admin(id = 2L)
    every { adminService.findById(3L) } returns null

    assertFailsWith<IllegalStateException> { handler.handle(event()) }
  }

  @Test
  @DisplayName("非法 payload 解析失败后重新抛出")
  fun `malformed payload rethrows`() {
    val event = DomainEvent(id = 9L, eventType = "DELIVERY_ASSIGNED", payload = "bad")

    assertFailsWith<Exception> { handler.handle(event) }
  }

  @Test
  @DisplayName("用户昵称缺失时使用默认文案")
  fun `falls back when user nickname missing`() {
    every { orderService.getOrderById(1L) } returns
      OrderModel(id = 1L, orderNo = "ORD-001", userId = 100L, user = UserModel(id = 100L))
    every { adminService.findById(2L) } returns admin(id = 2L)
    every { adminService.findById(3L) } returns admin(id = 3L, realName = "配送员B")

    handler.handle(event())

    verify(exactly = 1) {
      orderOperationService.recordOperation(
        1L,
        OrderOperationType.DELIVERY_ASSIGNED,
        dev.yidafu.aqua.common.domain.model.enums.OperatorType.ADMIN,
        operatorId = 2L,
        match { msg -> msg.contains("用户") },
      )
    }
    assertTrue(true)
  }
}
