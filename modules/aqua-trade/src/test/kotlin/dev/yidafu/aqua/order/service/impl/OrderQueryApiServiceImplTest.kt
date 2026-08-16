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
 * 跨模块订单查询服务单元测试:日期区间过滤、状态/配送员过滤、金额合计。
 */
class OrderQueryApiServiceImplTest :
  FunSpec({
    var orderRepository: OrderRepository = mockk()
    var orderItemRepository: OrderItemRepository = mockk()
    var service: OrderQueryApiServiceImpl = mockk()
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)

    fun order(
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

    beforeTest {
      orderRepository = mockk(relaxed = true)
      orderItemRepository = mockk(relaxed = true)
      service = OrderQueryApiServiceImpl(orderRepository, orderItemRepository)
    }
    test("findById and findByOrderNumber handle absence") {
      val order = order(1L)
      every { orderRepository.findById(1L) } returns Optional.of(order)
      every { orderRepository.findById(9L) } returns Optional.empty()
      every { orderRepository.findByOrderNo("ORD-1") } returns order
      every { orderRepository.findByOrderNo("NOPE") } returns null

      service.findById(1L) shouldBe order
      service.findById(9L).shouldBeNull()
      service.findByOrderNumber("ORD-1") shouldBe order
      service.findByOrderNumber("NOPE").shouldBeNull()
    }
    test("delegating queries pass through") {
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

      service.findItemsByOrderId(1L) shouldBe items
      service.findByIds(listOf(1L, 2L)) shouldBe orders
      service.findByUserId(100L) shouldBe orders
      service.findByUserIdAndStatus(100L, OrderModelStatus.PENDING_PAYMENT) shouldBe orders
      service.findByUserIdPaginated(100L, null, 0, 10) shouldBe page
      service.hasCompletedOrders(100L) shouldBe true
      service.findByStatus(OrderModelStatus.COMPLETED) shouldBe orders
      service.findByDeliveryWorkerId(5L) shouldBe orders
    }
    test("findByCreatedAtBetween filters by range") {
      val before = order(1L, createdAt = start.minusHours(1))
      val inside = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0))
      val after = order(3L, createdAt = end.plusHours(1))
      every { orderRepository.findAll() } returns listOf(before, inside, after)

      val result = service.findByCreatedAtBetween(start, end)

      result shouldBe listOf(inside)
    }
    test("findByStatusAndCreatedAtBetween filters by status and range") {
      val insideCompleted = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), status = OrderModelStatus.COMPLETED)
      val insideCancelled = order(4L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), status = OrderModelStatus.CANCELLED)
      val outsideCompleted = order(3L, createdAt = end.plusHours(1), status = OrderModelStatus.COMPLETED)
      every { orderRepository.findAll() } returns listOf(insideCompleted, insideCancelled, outsideCompleted)

      val result = service.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end)

      result shouldBe listOf(insideCompleted)
    }
    test("countByStatusAndCreatedAtBetween counts with nullable filters") {
      val inside = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), deliveryWorkerId = 5L)
      val insideOtherWorker = order(3L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), deliveryWorkerId = 6L)
      val outside = order(4L, createdAt = end.plusHours(1))
      every { orderRepository.findAll() } returns listOf(inside, insideOtherWorker, outside)

      service.countByStatusAndCreatedAtBetween(start, end, null, null) shouldBe 2L
      service.countByStatusAndCreatedAtBetween(start, end, 5L, null) shouldBe 1L
      service.countByStatusAndCreatedAtBetween(start, end, null, listOf(OrderModelStatus.COMPLETED)) shouldBe 2L
      service.countByStatusAndCreatedAtBetween(start, end, 5L, listOf(OrderModelStatus.CANCELLED)) shouldBe 0L
    }
    test("sumAmountCentsByStatusAndCreatedAtBetween sums amounts") {
      val a = order(1L, createdAt = LocalDateTime.of(2025, 1, 1, 6, 0), amountCents = 1000, deliveryWorkerId = 5L)
      val b = order(2L, createdAt = LocalDateTime.of(2025, 1, 1, 7, 0), amountCents = 2500, deliveryWorkerId = 5L)
      val c = order(3L, createdAt = LocalDateTime.of(2025, 1, 1, 8, 0), amountCents = 999, deliveryWorkerId = 6L)
      val cancelled = order(4L, createdAt = LocalDateTime.of(2025, 1, 1, 8, 0), amountCents = 5000, status = OrderModelStatus.CANCELLED)
      every { orderRepository.findAll() } returns listOf(a, b, c, cancelled)

      // 全部状态,不限配送员:1000 + 2500 + 999 + 5000
      service.sumAmountCentsByStatusAndCreatedAtBetween(
        listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED),
        start,
        end,
        null,
      ) shouldBe
        9499L
      // 仅已完成 + 指定配送员
      service.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 3500L
    }
    test("empty results are safe") {
      every { orderRepository.findAll() } returns emptyList()

      service.findByCreatedAtBetween(start, end).isEmpty() shouldBe true
      service.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end).isEmpty() shouldBe true
      service.countByStatusAndCreatedAtBetween(start, end, null, null) shouldBe 0L
      service.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, null) shouldBe 0L
      service.hasCompletedOrders(100L) shouldBe false
      verify { orderRepository.existsByUserIdAndStatus(100L, OrderModelStatus.COMPLETED) }
    }
  })
