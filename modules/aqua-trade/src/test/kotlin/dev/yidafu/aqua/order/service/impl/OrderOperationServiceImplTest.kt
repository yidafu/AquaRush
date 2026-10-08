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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 订单操作记录服务单元测试:记录/查询/计数均委托仓储。
 */
class OrderOperationServiceImplTest :
  FunSpec({
    var orderOperationRepository: OrderOperationRepository = mockk()
    var service: OrderOperationServiceImpl = mockk()

    beforeTest {
      orderOperationRepository = mockk(relaxed = true)
      service = OrderOperationServiceImpl(orderOperationRepository)
    }
    test("recordOperation saves and returns the operation") {
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

      saved.orderId shouldBe 1L
      saved.operationType shouldBe OrderOperationType.ORDER_CREATED
      saved.operatorType shouldBe OperatorType.USER
      saved.operatorId shouldBe 100L
      saved.description shouldBe "用户创建订单"
      saved.extraData shouldBe "{}"
      verify(exactly = 1) { orderOperationRepository.save(any()) }
    }
    test("recordOperation supports null operator and description") {
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

      saved.orderId shouldBe 2L
      saved.operatorType shouldBe OperatorType.SYSTEM
      saved.operatorId shouldBe null
      saved.description shouldBe null
    }
    test("getOrderOperations delegates to repository") {
      val ops =
        listOf(
          OrderOperationModel(orderId = 1L, operationType = OrderOperationType.ORDER_PAID, operatorType = OperatorType.USER),
          OrderOperationModel(orderId = 1L, operationType = OrderOperationType.ORDER_CREATED, operatorType = OperatorType.USER),
        )
      every { orderOperationRepository.findByOrderIdOrderByCreatedAtDesc(1L) } returns ops

      val result = service.getOrderOperations(1L)

      result shouldBe ops
      verify(exactly = 1) { orderOperationRepository.findByOrderIdOrderByCreatedAtDesc(1L) }
    }
    test("countOrderOperations delegates to repository") {
      every { orderOperationRepository.countByOrderId(1L) } returns 5L

      service.countOrderOperations(1L) shouldBe 5L
    }
  })
