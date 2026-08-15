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

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.NotFoundException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 订单查询服务单元测试:单条查询的异常路径、用户归属校验、状态字符串解析兜底、分页查询参数透传。
 */
class OrderQueryServiceImplTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var service: OrderQueryServiceImpl

  private fun order(
    id: Long,
    userId: Long = 100L,
    status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
    orderNo: String = "ORD-$id",
  ) = OrderModel(id = id, orderNo = orderNo, userId = userId, status = status)

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    service = OrderQueryServiceImpl(orderRepository)
  }

  @Test
  @DisplayName("getOrderById 命中返回订单,未命中抛 NotFoundException")
  fun `getOrderById returns order or throws not found`() {
    val order = order(1L)
    every { orderRepository.findById(1L) } returns Optional.of(order)
    every { orderRepository.findById(99L) } returns Optional.empty()

    assertEquals(order, service.getOrderById(1L))
    assertFailsWith<NotFoundException> { service.getOrderById(99L) }
  }

  @Test
  @DisplayName("getOrderByNo 命中返回订单,未命中抛 NotFoundException")
  fun `getOrderByNo returns order or throws not found`() {
    val order = order(1L, orderNo = "ORD-ABC")
    every { orderRepository.findByOrderNo("ORD-ABC") } returns order
    every { orderRepository.findByOrderNo("MISSING") } returns null

    assertEquals(order, service.getOrderByNo("ORD-ABC"))
    assertFailsWith<NotFoundException> { service.getOrderByNo("MISSING") }
  }

  @Test
  @DisplayName("按用户/状态查询直接透传仓储")
  fun `user and status queries delegate to repository`() {
    val orders = listOf(order(1L), order(2L))
    every { orderRepository.findByUserId(100L) } returns orders
    every { orderRepository.findByUserIdAndStatus(100L, OrderModelStatus.PENDING_PAYMENT) } returns orders
    every { orderRepository.findByStatus(OrderModelStatus.COMPLETED) } returns emptyList()
    every { orderRepository.findAll() } returns orders

    assertEquals(orders, service.getUserOrders(100L))
    assertEquals(orders, service.getUserOrdersByStatus(100L, OrderModelStatus.PENDING_PAYMENT))
    assertEquals(emptyList(), service.getOrdersByStatus(OrderModelStatus.COMPLETED))
    assertEquals(orders, service.findAllOrders())
    assertEquals(orders, service.findOrdersByUserId(100L))
  }

  @Test
  @DisplayName("findOrderByIdAndUserId:订单不存在或不属于该用户返回 null")
  fun `findOrderByIdAndUserId validates ownership`() {
    every { orderRepository.findById(1L) } returns Optional.of(order(1L, userId = 100L))
    every { orderRepository.findById(2L) } returns Optional.of(order(2L, userId = 200L))
    every { orderRepository.findById(3L) } returns Optional.empty()

    assertEquals(1L, service.findOrderByIdAndUserId(1L, 100L)?.id)
    assertNull(service.findOrderByIdAndUserId(2L, 100L), "他人订单不可见")
    assertNull(service.findOrderByIdAndUserId(3L, 100L), "订单不存在返回 null")
  }

  @Test
  @DisplayName("findOrderByNumberAndUserId:订单不存在或不属于该用户返回 null")
  fun `findOrderByNumberAndUserId validates ownership`() {
    every { orderRepository.findByOrderNo("ORD-1") } returns order(1L, userId = 100L)
    every { orderRepository.findByOrderNo("ORD-2") } returns order(2L, userId = 200L)
    every { orderRepository.findByOrderNo("ORD-3") } returns null

    assertEquals(1L, service.findOrderByNumberAndUserId("ORD-1", 100L)?.id)
    assertNull(service.findOrderByNumberAndUserId("ORD-2", 100L))
    assertNull(service.findOrderByNumberAndUserId("ORD-3", 100L))
  }

  @Test
  @DisplayName("findOrdersByStatus:合法状态查询,非法状态兜底为空列表")
  fun `findOrdersByStatus handles invalid status string`() {
    val orders = listOf(order(1L, status = OrderModelStatus.CANCELLED))
    every { orderRepository.findByStatus(OrderModelStatus.CANCELLED) } returns orders

    // 大小写不敏感
    assertEquals(orders, service.findOrdersByStatus("cancelled"))
    assertTrue(service.findOrdersByStatus("NOT_A_STATUS").isEmpty())
  }

  @Test
  @DisplayName("findOrdersByUserIdAndStatus:合法状态查询,非法状态兜底为空列表")
  fun `findOrdersByUserIdAndStatus handles invalid status string`() {
    val orders = listOf(order(1L, status = OrderModelStatus.DELIVERING))
    every { orderRepository.findByUserIdAndStatus(100L, OrderModelStatus.DELIVERING) } returns orders

    assertEquals(orders, service.findOrdersByUserIdAndStatus(100L, "DELIVERING"))
    assertTrue(service.findOrdersByUserIdAndStatus(100L, "bogus").isEmpty())
  }

  @Test
  @DisplayName("searchOrders 全参数透传(日期/状态/排序解析)")
  fun `searchOrders delegates with parsed filters`() {
    val page = mockk<Page<OrderModel>>(relaxed = true)
    val from = "2025-01-01T00:00:00"
    val to = "2025-01-02T00:00:00"
    every {
      orderRepository.findOrdersPaginated(
        keyword = any(),
        status = OrderModelStatus.COMPLETED,
        userId = 100L,
        deliveryWorkerId = 5L,
        startDate = LocalDateTime.parse(from),
        endDate = LocalDateTime.parse(to),
        minAmount = 100L,
        maxAmount = 10000L,
        page = 0,
        size = 10,
        sortField = "createdAt",
        sortDirection = "desc",
      )
    } returns page

    val result =
      service.searchOrders(
        keyword = "桶装水",
        status = "completed",
        userId = 100L,
        dateFrom = from,
        dateTo = to,
        minAmount = 100L,
        maxAmount = 10000L,
        deliveryWorkerId = 5L,
        page = 0,
        size = 10,
        sort = "createdAt,desc",
      )

    assertEquals(page, result)
  }

  @Test
  @DisplayName("searchOrders 非法日期/状态/空排序使用兜底值")
  fun `searchOrders falls back on invalid date status and sort`() {
    val page = mockk<Page<OrderModel>>(relaxed = true)
    every {
      orderRepository.findOrdersPaginated(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns
      page

    val result =
      service.searchOrders(
        keyword = null,
        status = "not-a-status",
        userId = null,
        dateFrom = "bad-date",
        dateTo = "also-bad",
        minAmount = null,
        maxAmount = null,
        deliveryWorkerId = null,
        page = 2,
        size = 20,
        sort = "",
      )

    assertEquals(page, result)
    verify {
      orderRepository.findOrdersPaginated(
        keyword = null,
        status = null,
        userId = null,
        deliveryWorkerId = null,
        startDate = null,
        endDate = null,
        minAmount = null,
        maxAmount = null,
        page = 2,
        size = 20,
        sortField = "",
        sortDirection = "desc",
      )
    }
  }

  @Test
  @DisplayName("getDeliveryWorkerHistoryOrders:未传状态使用历史状态集合")
  fun `history orders defaults to terminal statuses`() {
    val page = mockk<Page<OrderModel>>(relaxed = true)
    every {
      orderRepository.findByDeliveryWorkerIdAndStatusIn(
        deliveryWorkerId = 3L,
        statuses = listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED, OrderModelStatus.REFUNDED),
        keyword = "kw",
        page = 0,
        size = 10,
      )
    } returns page

    assertEquals(page, service.getDeliveryWorkerHistoryOrders(3L, null, "kw", 0, 10))
  }

  @Test
  @DisplayName("getDeliveryWorkerHistoryOrders:显式状态仅查询该状态")
  fun `history orders uses explicit status`() {
    val page = mockk<Page<OrderModel>>(relaxed = true)
    every {
      orderRepository.findByDeliveryWorkerIdAndStatusIn(
        deliveryWorkerId = 3L,
        statuses = listOf(OrderModelStatus.CANCELLED),
        keyword = null,
        page = 1,
        size = 5,
      )
    } returns page

    assertEquals(page, service.getDeliveryWorkerHistoryOrders(3L, OrderModelStatus.CANCELLED, null, 1, 5))
  }

  @Test
  @DisplayName("统计查询委托仓储")
  fun `statistics queries delegate to repository`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)
    every { orderRepository.countOrdersByDateRange(start, end, 3L, null) } returns 7L
    every {
      orderRepository.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 3L)
    } returns 12345L

    assertEquals(7L, service.countOrdersByDateRange(start, end, 3L, null))
    assertEquals(12345L, service.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 3L))
  }
}
