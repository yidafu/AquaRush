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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AdminOrderQueryResolverTest {
  private lateinit var orderQueryService: OrderQueryService
  private lateinit var orderGiftRepository: OrderGiftRepository
  private lateinit var adminOrderMapper: AdminOrderMapper
  private lateinit var resolver: AdminOrderQueryResolver

  private val order: Order = mockk(relaxed = true)

  private fun createSampleOrder(): OrderModel =
    OrderModel(
      id = 1L,
      orderNo = "ORD-20250101-001",
      userId = 1L,
      status = OrderModelStatus.PENDING_DISPATCH,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    orderQueryService = mockk(relaxed = true)
    orderGiftRepository = mockk(relaxed = true)
    adminOrderMapper = mockk(relaxed = true)
    resolver = AdminOrderQueryResolver(orderQueryService, orderGiftRepository, adminOrderMapper)
    every { adminOrderMapper.map(any()) } returns order
  }

  @Test
  fun `giftItems should map order gift rows`() {
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
    assertEquals(1, result.size)
    assertEquals("桶装水19L", result[0].productName)
    assertEquals(2, result[0].quantity)
    verify { orderGiftRepository.findByOrderId(1L) }
  }

  @Test
  fun `orders should use defaults when input is null`() {
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
    assertEquals(1, result.content.size)
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

  @Test
  fun `orders should pass through input values`() {
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
    assertEquals(0, result.content.size)
    assertEquals(1, result.number)
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

  @Test
  fun `order should map order by id`() {
    // Given
    every { orderQueryService.getOrderById(1L) } returns createSampleOrder()

    // When
    val result = resolver.order(1L)

    // Then
    assertNotNull(result)
    verify { orderQueryService.getOrderById(1L) }
  }

  @Test
  fun `orderByNo should map order by order number`() {
    // Given
    every { orderQueryService.getOrderByNo("ORD-20250101-001") } returns createSampleOrder()

    // When
    val result = resolver.orderByNo("ORD-20250101-001")

    // Then
    assertNotNull(result)
    verify { orderQueryService.getOrderByNo("ORD-20250101-001") }
  }
}
