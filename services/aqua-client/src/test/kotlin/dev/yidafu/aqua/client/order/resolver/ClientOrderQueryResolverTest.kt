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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * 用户端订单查询 Resolver 测试(F-ORD-003 详情/F-ORD-002 列表/F-ORD-007 评价):
 * 仅本人可见;列表 userId 强制登录用户;评价检查归属校验。
 */
class ClientOrderQueryResolverTest :
  FunSpec({
    var orderQueryApiService: OrderQueryApiService = mockk()
    var reviewQueryService: ReviewQueryService = mockk()
    var resolver: ClientOrderQueryResolver = mockk()
    var principal: UserPrincipal = mockk()

    beforeTest {
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
    test("orderReturnsOwnOrder") {
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

      result.orderNo shouldBe "ORD-001"
      result.payAmountCents shouldBe 1200L
    }
    test("orderNotFoundThrows") {
      every { orderQueryApiService.findById(1L) } returns null

      shouldThrow<NotFoundException> { resolver.order(1L, principal) }
    }
    test("orderRejectsForeignOrder") {
      every { orderQueryApiService.findById(1L) } returns
        OrderModel(
          id = 1L,
          orderNo = "ORD-001",
          userId = 99L,
          amountCents = 1000,
          status = OrderModelStatus.PENDING_PAYMENT,
        )

      shouldThrow<BadRequestException> { resolver.order(1L, principal) }
    }
    test("myOrdersUsesPrincipalUserId") {
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

      result.totalElements shouldBe 1
      result.content.size shouldBe 1
      result.content[0].orderNo shouldBe "ORD-001"
      result.content[0].items.size shouldBe 1
      result.content[0].items[0].quantity shouldBe 2
    }
    test("myOrdersPassesStatuses") {
      every {
        orderQueryApiService.findByUserIdPaginated(
          1L,
          listOf(OrderModelStatus.PENDING_DISPATCH, OrderModelStatus.PENDING_DELIVERY),
          1,
          20,
        )
      } returns PageImpl(emptyList<OrderModel>())

      val result = resolver.myOrders(listOf(OrderStatus.PENDING_DISPATCH, OrderStatus.PENDING_DELIVERY), 1, 20, principal)

      result.content.size shouldBe 0
      verify(exactly = 1) {
        orderQueryApiService.findByUserIdPaginated(
          1L,
          listOf(OrderModelStatus.PENDING_DISPATCH, OrderModelStatus.PENDING_DELIVERY),
          1,
          20,
        )
      }
    }
    test("reviewCheckReturnsCanReviewAndReview") {
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

      result.hasReviewed shouldBe true
      result.canReview shouldBe false
      result.review.shouldNotBeNull()
      result.review!!.rating shouldBe 4
    }
    test("reviewCheckRejectsForeignOrder") {
      every { orderQueryApiService.findById(1L) } returns
        OrderModel(
          id = 1L,
          orderNo = "ORD-001",
          userId = 99L,
          amountCents = 1000,
          status = OrderModelStatus.COMPLETED,
        )

      shouldThrow<BadRequestException> { resolver.orderReviewCheck(1L, principal) }
    }
  })
