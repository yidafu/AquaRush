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

import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 跨模块订单查询服务单元测试:日期区间过滤、状态/配送员过滤、金额合计。
 */
class OrderQueryApiServiceImplTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var orderItemRepository: OrderItemRepository
  private lateinit var service: OrderQueryApiServiceImpl

  private val start = LocalDateTime.of(2025, 1, 1, 0, 0)
  private val end = LocalDateTime.of(2025, 1, 2, 0, 0)

  private fun order(
    id: Long,
    createdAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 12, 0),
    status: OrderModelStatus = OrderModelStatus.COMPLETED,
    amountCents: Long = 1000,
    deliveryWorkerId: Long? = null,
  ) = OrderModel(
    id = id,
    userId = 100L,
    amountCents = amountCents,
    status = status,
    deliveryWorkerId = deliveryWorkerId,
    createdAt = createdAt,
  )

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    orderItemRepository = mockk(relaxed = true)
    service = OrderQueryApiServiceImpl(orderRepository, orderItemRepository)
  }

  @Test
  @DisplayName("findById/findByOrderNumber 未命中返回 null")
  fun `findById and findByOrderNumber handle absence`() {
    val order = order(1L)
    every { orderRepository.findById(1L) } returns Optional.of(order)
    every { orderRepository.findById(9L) } returns Optional.empty()
    every { orderRepository.findByOrderNo("ORD-1") } returns order
    every { orderRepository.findByOrderNo("NOPE") } returns null

    assertEquals(order, service.findById(1L))
    assertNull(service.findById(9L))
    assertEquals(order, service.findByOrderNumber("ORD-1"))
    assertNull(service.findByOrderNumber("NOPE"))
  }

  @Test
  @DisplayName("明细/批量/用户/状态查询透传仓储")
  fun `delegating queries pass through`() {
    val items =
      listOf(
        OrderItemModel(orderId = 1L, productId = 1L, quantity = 1, unitPriceCents = 100, totalPriceCents = 100, productSnapshot = "{}"),
      )
    val orders = listOf(order(1L), order(2L))
    val page = mockk<Page<OrderModel>>(relaxed = true)
    every { orderItemRepository.findByOrderId(1L) } returns items
    every { orderRepository.findAllById(listOf(1L, 2L)) } returns orders
    every { orderRepository.findByUserId(100L) } returns orders
    every { orderRepository.findByUserIdAndStatus(100L, OrderModelStatus.PENDING_PAYMENT) } returns orders
    every { orderRepository.findUserOrdersPaginated(100L, null, 0, 10) } returns page
    every { orderRepository.existsByUserIdAndStatus(100L, OrderModelStatus.COMPLETED) } returns true
    every { orderRepository.findByStatus(OrderModelStatus.COMPLETED) } returns orders
    every { orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(5L) } returns orders

    assertEquals(items, service.findItemsByOrderId(1L))
    assertEquals(orders, service.findByIds(listOf(1L, 2L)))
    assertEquals(orders, service.findByUserId(100L))
    assertEquals(orders, service.findByUserIdAndStatus(100L, OrderModelStatus.PENDING_PAYMENT))
    assertEquals(page, service.findByUserIdPaginated(100L, null, 0, 10))
    assertTrue(service.hasCompletedOrders(100L))
    assertEquals(orders, service.findByStatus(OrderModelStatus.COMPLETED))
    assertEquals(orders, service.findByDeliveryWorkerId(5L))
  }

  @Test
  @DisplayName("findByCreatedAtBetween 仅返回区间内订单")
  fun `findByCreatedAtBetween filters by range`() {
    val before = order(1L, createdAt = start.minusHours(1))
    val inside = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0))
    val after = order(3L, createdAt = end.plusHours(1))
    every { orderRepository.findAll() } returns listOf(before, inside, after)

    val result = service.findByCreatedAtBetween(start, end)

    assertEquals(listOf(inside), result)
  }

  @Test
  @DisplayName("findByStatusAndCreatedAtBetween 按状态与区间过滤")
  fun `findByStatusAndCreatedAtBetween filters by status and range`() {
    val insideCompleted = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), status = OrderModelStatus.COMPLETED)
    val insideCancelled = order(4L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), status = OrderModelStatus.CANCELLED)
    val outsideCompleted = order(3L, createdAt = end.plusHours(1), status = OrderModelStatus.COMPLETED)
    every { orderRepository.findAll() } returns listOf(insideCompleted, insideCancelled, outsideCompleted)

    val result = service.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end)

    assertEquals(listOf(insideCompleted), result)
  }

  @Test
  @DisplayName("countByStatusAndCreatedAtBetween 支持状态/配送员为空的全量计数")
  fun `countByStatusAndCreatedAtBetween counts with nullable filters`() {
    val inside = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), deliveryWorkerId = 5L)
    val insideOtherWorker = order(3L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), deliveryWorkerId = 6L)
    val outside = order(4L, createdAt = end.plusHours(1))
    every { orderRepository.findAll() } returns listOf(inside, insideOtherWorker, outside)

    assertEquals(2L, service.countByStatusAndCreatedAtBetween(start, end, null, null))
    assertEquals(1L, service.countByStatusAndCreatedAtBetween(start, end, 5L, null))
    assertEquals(
      2L,
      service.countByStatusAndCreatedAtBetween(start, end, null, listOf(OrderModelStatus.COMPLETED)),
    )
    assertEquals(
      0L,
      service.countByStatusAndCreatedAtBetween(start, end, 5L, listOf(OrderModelStatus.CANCELLED)),
    )
  }

  @Test
  @DisplayName("sumAmountCentsByStatusAndCreatedAtBetween 汇总区间内金额")
  fun `sumAmountCentsByStatusAndCreatedAtBetween sums amounts`() {
    val a = order(1L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), amountCents = 1000, deliveryWorkerId = 5L)
    val b = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 7, 0), amountCents = 2500, deliveryWorkerId = 5L)
    val c = order(3L, createdAt = LocalDateTime.of(2025, 1, 1, 8, 0), amountCents = 999, deliveryWorkerId = 6L)
    val cancelled = order(4L, createdAt = LocalDateTime.of(2025, 1, 1, 8, 0), amountCents = 5000, status = OrderModelStatus.CANCELLED)
    every { orderRepository.findAll() } returns listOf(a, b, c, cancelled)

    // 全部状态,不限配送员:1000 + 2500 + 999 + 5000
    assertEquals(
      9499L,
      service.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED), start, end, null),
    )
    // 仅已完成 + 指定配送员
    assertEquals(3500L, service.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, 5L))
  }

  @Test
  @DisplayName("空列表与空金额安全返回")
  fun `empty results are safe`() {
    every { orderRepository.findAll() } returns emptyList()

    assertTrue(service.findByCreatedAtBetween(start, end).isEmpty())
    assertTrue(service.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end).isEmpty())
    assertEquals(0L, service.countByStatusAndCreatedAtBetween(start, end, null, null))
    assertEquals(0L, service.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, null))
    assertFalse(service.hasCompletedOrders(100L))
    verify { orderRepository.existsByUserIdAndStatus(100L, OrderModelStatus.COMPLETED) }
  }
}
