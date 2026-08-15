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

package dev.yidafu.aqua.client.order.resolver

import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.review.ReviewQueryService
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.OrderStatus
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 用户端订单查询 Resolver 测试(F-ORD-003 详情/F-ORD-002 列表/F-ORD-007 评价):
 * 仅本人可见;列表 userId 强制登录用户;评价检查归属校验。
 */
@DisplayName("ClientOrderQueryResolver Tests")
class ClientOrderQueryResolverTest {
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var reviewQueryService: ReviewQueryService
  private lateinit var resolver: ClientOrderQueryResolver
  private lateinit var principal: UserPrincipal

  @BeforeEach
  fun setUp() {
    orderQueryApiService = mockk()
    reviewQueryService = mockk()
    resolver = ClientOrderQueryResolver(orderQueryApiService, reviewQueryService)
    // 商品明细(F-ORD-004):默认空列表,用例可按需 stub
    every { orderQueryApiService.findItemsByOrderId(any()) } returns emptyList()
    principal =
      UserPrincipal(
        id = 1L,
        _username = "openid-1",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  @Test
  @DisplayName("order(id):本人订单返回映射结果")
  fun orderReturnsOwnOrder() {
    every { orderQueryApiService.findById(1L) } returns
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 1L,
        amountCents = 1000,
        deliveryFeeCents = 200,
        status = OrderModelStatus.PENDING_PAYMENT,
      )

    val result = resolver.order(1L, principal)

    assertEquals("ORD-001", result.orderNo)
    assertEquals(1200L, result.payAmountCents)
  }

  @Test
  @DisplayName("order(id):订单不存在抛 NotFoundException")
  fun orderNotFoundThrows() {
    every { orderQueryApiService.findById(1L) } returns null

    assertFailsWith<NotFoundException> { resolver.order(1L, principal) }
  }

  @Test
  @DisplayName("order(id):非本人订单拒绝")
  fun orderRejectsForeignOrder() {
    every { orderQueryApiService.findById(1L) } returns
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 99L,
        amountCents = 1000,
        status = OrderModelStatus.PENDING_PAYMENT,
      )

    assertFailsWith<BadRequestException> { resolver.order(1L, principal) }
  }

  @Test
  @DisplayName("myOrders(F-ORD-002):按登录用户查询且分页映射正确")
  fun myOrdersUsesPrincipalUserId() {
    every {
      orderQueryApiService.findByUserIdPaginated(1L, null, 0, 10)
    } returns
      PageImpl(
        listOf(
          OrderModel(
            id = 1L,
            orderNo = "ORD-001",
            userId = 1L,
            amountCents = 2000,
            deliveryFeeCents = 0,
            status = OrderModelStatus.PENDING_PAYMENT,
          ),
        ),
      )

    // 商品明细映射(F-ORD-004):明细随单快照
    every { orderQueryApiService.findItemsByOrderId(1L) } returns
      listOf(
        OrderItemModel(orderId = 1L, productId = 1L, quantity = 2, unitPriceCents = 1000, totalPriceCents = 2000, productSnapshot = "{}"),
      )

    val result = resolver.myOrders(null, 0, 10, principal)

    assertEquals(1, result.totalElements)
    assertEquals(1, result.content.size)
    assertEquals("ORD-001", result.content[0].orderNo)
    assertEquals(1, result.content[0].items.size)
    assertEquals(2, result.content[0].items[0].quantity)
  }

  @Test
  @DisplayName("myOrders:状态过滤透传(待配送口径=待派单+待配送)")
  fun myOrdersPassesStatuses() {
    every {
      orderQueryApiService.findByUserIdPaginated(
        1L,
        listOf(OrderModelStatus.PENDING_DISPATCH, OrderModelStatus.PENDING_DELIVERY),
        1,
        20,
      )
    } returns PageImpl(emptyList<OrderModel>())

    val result = resolver.myOrders(listOf(OrderStatus.PENDING_DISPATCH, OrderStatus.PENDING_DELIVERY), 1, 20, principal)

    assertEquals(0, result.content.size)
    verify(exactly = 1) {
      orderQueryApiService.findByUserIdPaginated(
        1L,
        listOf(OrderModelStatus.PENDING_DISPATCH, OrderModelStatus.PENDING_DELIVERY),
        1,
        20,
      )
    }
  }

  @Test
  @DisplayName("orderReviewCheck(F-ORD-007):已完成未评价可评,已评价返回内容")
  fun reviewCheckReturnsCanReviewAndReview() {
    every { orderQueryApiService.findById(1L) } returns
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 1L,
        amountCents = 1000,
        status = OrderModelStatus.COMPLETED,
      )
    every { reviewQueryService.findByOrderId(1L) } returns
      ReviewModel(orderId = 1L, userId = 1L, deliveryWorkerId = 5L, rating = 4, comment = "不错", isAnonymous = false)

    val result = resolver.orderReviewCheck(1L, principal)

    assertTrue(result.hasReviewed)
    assertFalse(result.canReview)
    assertNotNull(result.review)
    assertEquals(4, result.review!!.rating)
  }

  @Test
  @DisplayName("orderReviewCheck:非本人订单拒绝")
  fun reviewCheckRejectsForeignOrder() {
    every { orderQueryApiService.findById(1L) } returns
      OrderModel(
        id = 1L,
        orderNo = "ORD-001",
        userId = 99L,
        amountCents = 1000,
        status = OrderModelStatus.COMPLETED,
      )

    assertFailsWith<BadRequestException> { resolver.orderReviewCheck(1L, principal) }
  }
}
