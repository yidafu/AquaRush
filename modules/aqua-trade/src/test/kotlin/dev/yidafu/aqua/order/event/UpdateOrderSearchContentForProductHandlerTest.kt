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

package dev.yidafu.aqua.order.event

import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 商品更新后订单搜索内容重建处理器单元测试(经 order_items 反查订单)。
 */
class UpdateOrderSearchContentForProductHandlerTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var orderItemRepository: OrderItemRepository
  private lateinit var addressQueryApiService: AddressQueryApiService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var handler: UpdateOrderSearchContentForProductHandler

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    orderItemRepository = mockk(relaxed = true)
    addressQueryApiService = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    handler =
      UpdateOrderSearchContentForProductHandler(orderRepository, orderItemRepository, addressQueryApiService, productQueryApiService)
  }

  private fun order(id: Long) = OrderModel(id = id, orderNo = "ORD-$id", userId = 100L, addressId = 7L)

  private fun event() = DomainEvent(id = 1L, eventType = "PRODUCT_UPDATED", payload = """{"productId": 11}""")

  @Test
  @DisplayName("getSupportedEventType 返回 PRODUCT_UPDATED")
  fun `supported event type is product updated`() {
    assertEquals(DomainEventType.PRODUCT_UPDATED, handler.getSupportedEventType())
  }

  @Test
  @DisplayName("包含该商品的订单全部重建搜索内容(去重)")
  fun `rebuilds search content for orders containing product`() {
    val ord = order(1L)
    val ord2 = order(2L)
    every { orderItemRepository.findByProductId(11L) } returns
      listOf(
        OrderItemModel(orderId = 1L, productId = 11L, quantity = 1, unitPriceCents = 100, totalPriceCents = 100, productSnapshot = "{}"),
        OrderItemModel(orderId = 1L, productId = 11L, quantity = 2, unitPriceCents = 100, totalPriceCents = 200, productSnapshot = "{}"),
        OrderItemModel(orderId = 2L, productId = 11L, quantity = 1, unitPriceCents = 100, totalPriceCents = 100, productSnapshot = "{}"),
      )
    every { orderRepository.findById(1L) } returns Optional.of(ord)
    every { orderRepository.findById(2L) } returns Optional.of(ord2)
    every { addressQueryApiService.findById(7L) } returns null
    every { orderItemRepository.findByOrderId(1L) } returns
      listOf(
        OrderItemModel(orderId = 1L, productId = 11L, quantity = 1, unitPriceCents = 100, totalPriceCents = 100, productSnapshot = "{}"),
      )
    every { orderItemRepository.findByOrderId(2L) } returns emptyList()
    every { productQueryApiService.findById(11L) } returns ProductModel(id = 11L, name = "桶装水", price = 1000, stock = 100)
    every { orderRepository.save(any()) } answers { firstArg() }

    handler.handle(event())

    verify(exactly = 2) { orderRepository.save(any()) }
    assertNotNull(ord.searchContent)
    assertTrue(ord.searchContent!!.contains("桶装水"))
    assertNotNull(ord2.searchContent)
  }

  @Test
  @DisplayName("无订单包含该商品:直接返回")
  fun `returns early when no orders contain product`() {
    every { orderItemRepository.findByProductId(11L) } returns emptyList()

    handler.handle(event())

    verify(exactly = 0) { orderRepository.save(any()) }
  }

  @Test
  @DisplayName("订单已删除时跳过该订单")
  fun `skips missing orders`() {
    every { orderItemRepository.findByProductId(11L) } returns
      listOf(
        OrderItemModel(orderId = 1L, productId = 11L, quantity = 1, unitPriceCents = 100, totalPriceCents = 100, productSnapshot = "{}"),
      )
    every { orderRepository.findById(1L) } returns Optional.empty()

    handler.handle(event())

    verify(exactly = 0) { orderRepository.save(any()) }
  }

  @Test
  @DisplayName("非法 payload 解析失败后重新抛出")
  fun `malformed payload rethrows`() {
    val event = DomainEvent(id = 9L, eventType = "PRODUCT_UPDATED", payload = "nope")

    assertFailsWith<Exception> { handler.handle(event) }
  }
}
