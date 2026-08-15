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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.utils.JsonHelper
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.logging.util.BizLogger
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
 * 配送任务变更服务单元测试:
 * - R-ORD-01/G09 状态机:派单仅接受「待派单」(待派单→配送中),二次派单/已接单订单被拦截;
 * - R-ORD-07 完成配送服务端硬校验:照片非空、收款方式必选(自提/水票核销豁免);
 * - R-ORD-08 派单约束:离线送水工不可派。
 */
class DeliveryTaskMutationServiceImplTest {
  private lateinit var workerRepository: DeliveryWorkerRepository
  private lateinit var orderQueryService: OrderQueryService
  private lateinit var orderMutationService: OrderMutationService
  private lateinit var orderOperationService: OrderOperationService
  private lateinit var eventPublishService: SimplifiedEventPublishService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: DeliveryTaskMutationServiceImpl

  private fun order(status: OrderModelStatus) = OrderModel(id = 1L, userId = 100L, status = status)

  private val onlineWorker =
    DeliveryWorkerModel(
      id = 1L,
      userId = 200L,
      name = "送水工甲",
      phone = "13800138000",
      onlineStatus = DeliverWorkerModelStatus.ONLINE,
    )

  @BeforeEach
  fun setUp() {
    workerRepository = mockk(relaxed = true)
    orderQueryService = mockk(relaxed = true)
    orderMutationService = mockk(relaxed = true)
    orderOperationService = mockk(relaxed = true)
    eventPublishService = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    every { workerRepository.findById(1L) } returns java.util.Optional.of(onlineWorker)
    every { orderMutationService.updateOrder(any(), any()) } answers { secondArg() as OrderModel }
    service =
      DeliveryTaskMutationServiceImpl(
        workerRepository,
        orderQueryService,
        orderMutationService,
        orderOperationService,
        eventPublishService,
        JsonHelper(),
        bizLogger,
      )
  }

  @Test
  @DisplayName("R-ORD-01/G09:派单仅接受「待派单」订单(待派单→配送中)")
  fun `assign accepts pending dispatch only`() {
    every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DISPATCH)

    val assigned = service.assignDeliveryWorker(1L, 1L, 1L, false)

    assertEquals(OrderModelStatus.DELIVERING, assigned.status)
    assertEquals(1L, assigned.deliveryWorkerId)
  }

  @Test
  @DisplayName("R-ORD-01:已接单(待配送)/配送中/已完成订单不可再派单(二次派单拦截,R-ORD-08)")
  fun `assign rejects non pending dispatch orders`() {
    listOf(
      OrderModelStatus.PENDING_DELIVERY,
      OrderModelStatus.DELIVERING,
      OrderModelStatus.COMPLETED,
      OrderModelStatus.PENDING_PAYMENT,
    ).forEach { status ->
      every { orderQueryService.getOrderById(1L) } returns order(status)
      assertFailsWith<BadRequestException>("status=$status 应被拦截") {
        service.assignDeliveryWorker(1L, 1L, 1L, false)
      }
    }
  }

  @Test
  @DisplayName("R-ORD-08:离线送水工不可派单")
  fun `assign rejects offline worker`() {
    every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DISPATCH)
    every { workerRepository.findById(1L) } returns
      java.util.Optional.of(
        DeliveryWorkerModel(
          id = 1L,
          userId = 200L,
          name = "送水工甲",
          phone = "13800138000",
          onlineStatus = DeliverWorkerModelStatus.OFFLINE,
        ),
      )

    assertFailsWith<BadRequestException> { service.assignDeliveryWorker(1L, 1L, 1L, false) }
  }

  @Test
  @DisplayName("R-ORD-07:完成配送照片为空被服务端拦截(防止绕过客户端)")
  fun `complete delivery rejects empty photos`() {
    every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING)

    assertFailsWith<BadRequestException> { service.completeDelivery(1L, emptyList(), PaymentType.CASH, null) }
  }

  @Test
  @DisplayName("R-ORD-07:完成配送收款方式必选(非自提、无水票核销时拦截)")
  fun `complete delivery rejects missing payment type`() {
    every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.DELIVERING)

    assertFailsWith<BadRequestException> { service.completeDelivery(1L, listOf("https://x/1.jpg"), null, null) }
  }

  @Test
  @DisplayName("R-ORD-07/R-COL-04:电子票核销订单未选收款方式自动记「水票」")
  fun `complete delivery defaults to water ticket for ticket orders`() {
    val ticketOrder =
      order(OrderModelStatus.DELIVERING).also {
        it.ticketRedeemCount = 1
        it.deliveryWorkerId = 1L
      }
    every { orderQueryService.getOrderById(1L) } returns ticketOrder

    val completed = service.completeDelivery(1L, listOf("https://x/1.jpg"), null, null)

    assertEquals(OrderModelStatus.COMPLETED, completed.status)
    assertEquals(PaymentType.WATER_TICKET, completed.paymentType)
    assertTrue(completed.deliveryPhotos != null)
    verify(exactly = 1) { eventPublishService.publishDeliveryCompleted(1L, 1L) }
  }

  @Test
  @DisplayName("R-ORD-09:自提订单免收款方式(已收款,不强制选择)")
  fun `complete delivery allows self collect without payment type`() {
    val selfCollectOrder =
      order(OrderModelStatus.DELIVERING).also {
        it.isSelfCollect = true
        it.deliveryWorkerId = 1L
      }
    every { orderQueryService.getOrderById(1L) } returns selfCollectOrder

    val completed = service.completeDelivery(1L, listOf("https://x/1.jpg"), null, null)

    assertEquals(OrderModelStatus.COMPLETED, completed.status)
  }

  @Test
  @DisplayName("R-ORD-01:完成配送仅接受配送中订单")
  fun `complete delivery rejects non delivering order`() {
    every { orderQueryService.getOrderById(1L) } returns order(OrderModelStatus.PENDING_DELIVERY)

    assertFailsWith<BadRequestException> { service.completeDelivery(1L, listOf("https://x/1.jpg"), PaymentType.CASH, null) }
  }
}
