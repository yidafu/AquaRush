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
import dev.yidafu.aqua.common.domain.model.AddressModel
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 地址更新后订单搜索内容重建处理器单元测试。
 */
class UpdateOrderSearchContentHandlerTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var orderItemRepository: OrderItemRepository
  private lateinit var addressQueryApiService: AddressQueryApiService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var handler: UpdateOrderSearchContentHandler

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    orderItemRepository = mockk(relaxed = true)
    addressQueryApiService = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    handler = UpdateOrderSearchContentHandler(orderRepository, orderItemRepository, addressQueryApiService, productQueryApiService)
  }

  private fun address() =
    AddressModel(
      id = 7L,
      userId = 100L,
      receiverName = "张三",
      phone = "13800138000",
      province = "浙江省",
      city = "杭州市",
      district = "西湖区",
      detailAddress = "文一西路100号",
      floor = 4,
    )

  private fun order(id: Long) = OrderModel(id = id, orderNo = "ORD-$id", userId = 100L, addressId = 7L)

  private fun event() = DomainEvent(id = 1L, eventType = "ADDRESS_UPDATED", payload = """{"addressId": 7}""")

  @Test
  @DisplayName("getSupportedEventType 返回 ADDRESS_UPDATED")
  fun `supported event type is address updated`() {
    assertEquals(DomainEventType.ADDRESS_UPDATED, handler.getSupportedEventType())
  }

  @Test
  @DisplayName("命中订单与地址:重建搜索内容并逐单保存")
  fun `rebuilds search content for matching orders`() {
    val ord = order(1L)
    val ord2 = order(2L)
    every { orderRepository.findByAddressId(7L) } returns listOf(ord, ord2)
    every { addressQueryApiService.findById(7L) } returns address()
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
    assertTrue(ord.searchContent!!.contains("桶装水"), "搜索内容应包含商品名")
    assertTrue(ord.searchContent!!.contains("张三"), "搜索内容应包含收货人")
    assertTrue(ord2.searchContent!!.contains("张三"), "无明细订单按地址信息重建")
  }

  @Test
  @DisplayName("无订单或地址不存在:直接返回")
  fun `returns early when no orders or address`() {
    every { orderRepository.findByAddressId(7L) } returns emptyList()
    handler.handle(event())
    verify(exactly = 0) { orderRepository.save(any()) }

    every { orderRepository.findByAddressId(8L) } returns listOf(order(1L))
    every { addressQueryApiService.findById(8L) } returns null
    handler.handle(DomainEvent(id = 2L, eventType = "ADDRESS_UPDATED", payload = """{"addressId": 8}"""))
    verify(exactly = 0) { orderRepository.save(any()) }
  }

  @Test
  @DisplayName("部分商品查询失败的商品被跳过")
  fun `missing products are skipped`() {
    val ord = order(1L)
    every { orderRepository.findByAddressId(7L) } returns listOf(ord)
    every { addressQueryApiService.findById(7L) } returns address()
    every { orderItemRepository.findByOrderId(1L) } returns
      listOf(
        OrderItemModel(orderId = 1L, productId = 11L, quantity = 1, unitPriceCents = 100, totalPriceCents = 100, productSnapshot = "{}"),
        OrderItemModel(orderId = 1L, productId = 12L, quantity = 2, unitPriceCents = 100, totalPriceCents = 200, productSnapshot = "{}"),
      )
    every { productQueryApiService.findById(11L) } returns ProductModel(id = 11L, name = "桶装水", price = 1000, stock = 100)
    every { productQueryApiService.findById(12L) } returns null
    every { orderRepository.save(any()) } answers { firstArg() }

    handler.handle(event())

    verify(exactly = 1) { orderRepository.save(any()) }
    assertTrue(ord.searchContent!!.contains("桶装水"))
  }

  @Test
  @DisplayName("非法 payload 解析失败后重新抛出")
  fun `malformed payload rethrows`() {
    val event = DomainEvent(id = 9L, eventType = "ADDRESS_UPDATED", payload = "nope")

    assertFailsWith<Exception> { handler.handle(event) }
  }
}
