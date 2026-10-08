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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Page
import java.time.LocalDateTime
import java.util.Optional

/**
 * 订单查询服务单元测试:单条查询的异常路径、用户归属校验、状态字符串解析兜底、分页查询参数透传。
 */
class OrderQueryServiceImplTest :
  FunSpec({
    var orderRepository: OrderRepository = mockk()
    var service: OrderQueryServiceImpl = mockk()

    fun order(
      id: Long,
      userId: Long = 100L,
      status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
      orderNo: String = "ORD-$id",
    ) = OrderModel(id = id, orderNo = orderNo, userId = userId, status = status)

    beforeTest {
      orderRepository = mockk(relaxed = true)
      service = OrderQueryServiceImpl(orderRepository)
    }
    test("getOrderById returns order or throws not found") {
      val order = order(1L)
      every { orderRepository.findById(1L) } returns Optional.of(order)
      every { orderRepository.findById(99L) } returns Optional.empty()

      service.getOrderById(1L) shouldBe order
      shouldThrow<NotFoundException> { service.getOrderById(99L) }
    }
    test("getOrderByNo returns order or throws not found") {
      val order = order(1L, orderNo = "ORD-ABC")
      every { orderRepository.findByOrderNo("ORD-ABC") } returns order
      every { orderRepository.findByOrderNo("MISSING") } returns null

      service.getOrderByNo("ORD-ABC") shouldBe order
      shouldThrow<NotFoundException> { service.getOrderByNo("MISSING") }
    }
    test("user and status queries delegate to repository") {
      val orders = listOf(order(1L), order(2L))
      every { orderRepository.findByUserId(100L) } returns orders
      every { orderRepository.findByUserIdAndStatus(100L, OrderModelStatus.PENDING_PAYMENT) } returns orders
      every { orderRepository.findByStatus(OrderModelStatus.COMPLETED) } returns emptyList()
      every { orderRepository.findAll() } returns orders

      service.getUserOrders(100L) shouldBe orders
      service.getUserOrdersByStatus(100L, OrderModelStatus.PENDING_PAYMENT) shouldBe orders
      service.getOrdersByStatus(OrderModelStatus.COMPLETED) shouldBe emptyList()
      service.findAllOrders() shouldBe orders
      service.findOrdersByUserId(100L) shouldBe orders
    }
    test("findOrderByIdAndUserId validates ownership") {
      every { orderRepository.findById(1L) } returns Optional.of(order(1L, userId = 100L))
      every { orderRepository.findById(2L) } returns Optional.of(order(2L, userId = 200L))
      every { orderRepository.findById(3L) } returns Optional.empty()

      service.findOrderByIdAndUserId(1L, 100L)?.id shouldBe 1L
      withClue("他人订单不可见") { service.findOrderByIdAndUserId(2L, 100L).shouldBeNull() }
      withClue("订单不存在返回 null") { service.findOrderByIdAndUserId(3L, 100L).shouldBeNull() }
    }
    test("findOrderByNumberAndUserId validates ownership") {
      every { orderRepository.findByOrderNo("ORD-1") } returns order(1L, userId = 100L)
      every { orderRepository.findByOrderNo("ORD-2") } returns order(2L, userId = 200L)
      every { orderRepository.findByOrderNo("ORD-3") } returns null

      service.findOrderByNumberAndUserId("ORD-1", 100L)?.id shouldBe 1L
      service.findOrderByNumberAndUserId("ORD-2", 100L).shouldBeNull()
      service.findOrderByNumberAndUserId("ORD-3", 100L).shouldBeNull()
    }
    test("findOrdersByStatus handles invalid status string") {
      val orders = listOf(order(1L, status = OrderModelStatus.CANCELLED))
      every { orderRepository.findByStatus(OrderModelStatus.CANCELLED) } returns orders

      // 大小写不敏感
      service.findOrdersByStatus("cancelled") shouldBe orders
      service.findOrdersByStatus("NOT_A_STATUS").isEmpty() shouldBe true
    }
    test("findOrdersByUserIdAndStatus handles invalid status string") {
      val orders = listOf(order(1L, status = OrderModelStatus.DELIVERING))
      every { orderRepository.findByUserIdAndStatus(100L, OrderModelStatus.DELIVERING) } returns orders

      service.findOrdersByUserIdAndStatus(100L, "DELIVERING") shouldBe orders
      service.findOrdersByUserIdAndStatus(100L, "bogus").isEmpty() shouldBe true
    }
    test("searchOrders delegates with parsed filters") {
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

      result shouldBe page
    }
    test("searchOrders falls back on invalid date status and sort") {
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

      result shouldBe page
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
    test("history orders defaults to terminal statuses") {
      val page = mockk<Page<OrderModel>>(relaxed = true)
      every {
        orderRepository.findByDeliveryWorkerIdAndStatusIn(
          deliveryWorkerId = 3L,
          // R-ORD-11:历史订单含 待确认收货(已送达未确认),与已完成/已取消/已退款并列
          statuses =
            listOf(
              OrderModelStatus.AWAITING_CONFIRMATION,
              OrderModelStatus.COMPLETED,
              OrderModelStatus.CANCELLED,
              OrderModelStatus.REFUNDED,
            ),
          keyword = "kw",
          page = 0,
          size = 10,
        )
      } returns page

      service.getDeliveryWorkerHistoryOrders(3L, null, "kw", 0, 10) shouldBe page
    }
    test("history orders uses explicit status") {
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

      service.getDeliveryWorkerHistoryOrders(3L, OrderModelStatus.CANCELLED, null, 1, 5) shouldBe page
    }
    test("statistics queries delegate to repository") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      every { orderRepository.countOrdersByDateRange(start, end, 3L, null) } returns 7L
      every {
        orderRepository.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 3L)
      } returns 12345L

      service.countOrdersByDateRange(start, end, 3L, null) shouldBe 7L
      service.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 3L) shouldBe 12345L
    }
  })
