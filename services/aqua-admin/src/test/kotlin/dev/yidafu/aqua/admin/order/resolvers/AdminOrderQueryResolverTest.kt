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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.admin.order.mapper.AdminOrderMapper
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.OrderGiftModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderListInput
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.order.domain.repository.OrderGiftRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class AdminOrderQueryResolverTest :
  FunSpec({
    var orderQueryService: OrderQueryService = mockk()
    var orderGiftRepository: OrderGiftRepository = mockk()
    var adminOrderMapper: AdminOrderMapper = mockk()
    var resolver: AdminOrderQueryResolver = mockk()
    val order: Order = mockk(relaxed = true)

    fun createSampleOrder(): OrderModel =
      OrderModel(
        id = 1L,
        orderNo = "ORD-20250101-001",
        userId = 1L,
        status = OrderModelStatus.PENDING_DISPATCH,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      orderQueryService = mockk(relaxed = true)
      orderGiftRepository = mockk(relaxed = true)
      adminOrderMapper = mockk(relaxed = true)
      resolver = AdminOrderQueryResolver(orderQueryService, orderGiftRepository, adminOrderMapper)
      every { adminOrderMapper.map(any()) } returns order
    }
    test("giftItems should map order gift rows") {
      // Given
      every { orderGiftRepository.findByOrderId(1L) } returns
        listOf(
          OrderGiftModel(
            id = 1L,
            orderId = 1L,
            productId = 10L,
            productName = "桶装水19L",
            quantity = 2,
          ),
        )
      val order = mockk<Order>()
      every { order.id } returns 1L

      // When
      val result = resolver.giftItems(order)

      // Then
      result.size shouldBe 1
      result[0].productName shouldBe "桶装水19L"
      result[0].quantity shouldBe 2
      verify { orderGiftRepository.findByOrderId(1L) }
    }
    test("orders should use defaults when input is null") {
      // Given
      every {
        orderQueryService.searchOrders(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          0,
          20,
          "createdAt,desc",
        )
      } returns PageImpl(listOf(createSampleOrder()))

      // When
      val result = resolver.orders(null)

      // Then
      result.content.size shouldBe 1
      verify {
        orderQueryService.searchOrders(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          0,
          20,
          "createdAt,desc",
        )
      }
    }
    test("orders should pass through input values") {
      // Given
      val input =
        OrderListInput(
          search = "张三",
          status = OrderStatus.COMPLETED,
          userId = 5L,
          page = 1,
          size = 10,
          sort = "amountCents,asc",
        )
      every {
        orderQueryService.searchOrders(
          "张三",
          OrderStatus.COMPLETED.name,
          5L,
          null,
          null,
          null,
          null,
          null,
          1,
          10,
          "amountCents,asc",
        )
      } returns PageImpl(emptyList(), PageRequest.of(1, 10), 0)

      // When
      val result = resolver.orders(input)

      // Then
      result.content.size shouldBe 0
      result.number shouldBe 1
      verify {
        orderQueryService.searchOrders(
          "张三",
          OrderStatus.COMPLETED.name,
          5L,
          null,
          null,
          null,
          null,
          null,
          1,
          10,
          "amountCents,asc",
        )
      }
    }
    test("order should map order by id") {
      // Given
      every { orderQueryService.getOrderById(1L) } returns createSampleOrder()

      // When
      val result = resolver.order(1L)

      // Then
      result.shouldNotBeNull()
      verify { orderQueryService.getOrderById(1L) }
    }
    test("orderByNo should map order by order number") {
      // Given
      every { orderQueryService.getOrderByNo("ORD-20250101-001") } returns createSampleOrder()

      // When
      val result = resolver.orderByNo("ORD-20250101-001")

      // Then
      result.shouldNotBeNull()
      verify { orderQueryService.getOrderByNo("ORD-20250101-001") }
    }
  })
