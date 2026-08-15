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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.order.domain.repository.OrderOperationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 订单操作记录服务单元测试:记录/查询/计数均委托仓储。
 */
class OrderOperationServiceImplTest {
  private lateinit var orderOperationRepository: OrderOperationRepository
  private lateinit var service: OrderOperationServiceImpl

  @BeforeEach
  fun setUp() {
    orderOperationRepository = mockk(relaxed = true)
    service = OrderOperationServiceImpl(orderOperationRepository)
  }

  @Test
  @DisplayName("recordOperation 落库并返回带完整字段的操作记录")
  fun `recordOperation saves and returns the operation`() {
    every { orderOperationRepository.save(any()) } answers { firstArg() }

    val saved =
      service.recordOperation(
        orderId = 1L,
        operationType = OrderOperationType.ORDER_CREATED,
        operatorType = OperatorType.USER,
        operatorId = 100L,
        description = "用户创建订单",
        extraData = "{}",
      )

    assertEquals(1L, saved.orderId)
    assertEquals(OrderOperationType.ORDER_CREATED, saved.operationType)
    assertEquals(OperatorType.USER, saved.operatorType)
    assertEquals(100L, saved.operatorId)
    assertEquals("用户创建订单", saved.description)
    assertEquals("{}", saved.extraData)
    verify(exactly = 1) { orderOperationRepository.save(any()) }
  }

  @Test
  @DisplayName("recordOperation 支持空操作者与空描述(系统操作)")
  fun `recordOperation supports null operator and description`() {
    every { orderOperationRepository.save(any()) } answers { firstArg() }

    val saved =
      service.recordOperation(
        orderId = 2L,
        operationType = OrderOperationType.PAYMENT_TIMEOUT,
        operatorType = OperatorType.SYSTEM,
        operatorId = null,
        description = null,
        extraData = null,
      )

    assertEquals(2L, saved.orderId)
    assertEquals(OperatorType.SYSTEM, saved.operatorType)
    assertEquals(null, saved.operatorId)
    assertEquals(null, saved.description)
  }

  @Test
  @DisplayName("getOrderOperations 按创建时间倒序返回操作记录")
  fun `getOrderOperations delegates to repository`() {
    val ops =
      listOf(
        OrderOperationModel(orderId = 1L, operationType = OrderOperationType.ORDER_PAID, operatorType = OperatorType.USER),
        OrderOperationModel(orderId = 1L, operationType = OrderOperationType.ORDER_CREATED, operatorType = OperatorType.USER),
      )
    every { orderOperationRepository.findByOrderIdOrderByCreatedAtDesc(1L) } returns ops

    val result = service.getOrderOperations(1L)

    assertEquals(ops, result)
    verify(exactly = 1) { orderOperationRepository.findByOrderIdOrderByCreatedAtDesc(1L) }
  }

  @Test
  @DisplayName("countOrderOperations 返回操作记录数")
  fun `countOrderOperations delegates to repository`() {
    every { orderOperationRepository.countByOrderId(1L) } returns 5L

    assertEquals(5L, service.countOrderOperations(1L))
  }
}
