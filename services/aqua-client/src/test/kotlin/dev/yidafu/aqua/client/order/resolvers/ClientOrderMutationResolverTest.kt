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

package dev.yidafu.aqua.client.order.resolvers

import dev.yidafu.aqua.api.dto.CreateReviewRequest
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.review.ReviewMutationService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.graphql.generated.CreateReviewRequestInput
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot
import dev.yidafu.aqua.common.graphql.generated.OrderItemInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import tools.jackson.databind.ObjectMapper
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot as DomainDeliveryTimeSlot

/**
 * 用户端订单 Resolver 单元测试(F-ORD-001/005/006/007):
 * 下单 userId 强制取登录用户;发起支付校验归属;假支付模式 autoCompleted=true;
 * 取消订单仅本人;评价 userId 强制登录用户。
 */
class ClientOrderMutationResolverTest :
  FunSpec({
    var orderMutationService: OrderMutationService = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var paymentService: PaymentService = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var reviewMutationService: ReviewMutationService = mockk()
    var resolver: ClientOrderMutationResolver = mockk()
    var principal: UserPrincipal = mockk()

    fun order(
      id: Long = 1L,
      userId: Long = 1L,
      status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
    ) = OrderModel(
      id = id,
      orderNo = "ORD-001",
      userId = userId,
      amountCents = 1000,
      deliveryFeeCents = 200,
      status = status,
    )

    beforeTest {
      orderMutationService = mockk()
      orderQueryApiService = mockk()
      paymentService = mockk()
      userQueryApiService = mockk()
      reviewMutationService = mockk()
      resolver =
        ClientOrderMutationResolver(
          orderMutationService,
          orderQueryApiService,
          paymentService,
          userQueryApiService,
          reviewMutationService,
          ObjectMapper(),
        )
      principal =
        UserPrincipal(
          id = 1L,
          _username = "openid-1",
          userType = "USER",
          _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }
    test("createOrderUsesPrincipalUserId") {
      val created = order()
      every { orderMutationService.createOrder(any()) } answers {
        val req = firstArg<dev.yidafu.aqua.api.dto.CreateOrderRequest>()
        withClue("userId 必须为登录用户") { req.userId shouldBe 1L }
        withClue("deliveryTime → deliveryTimeSlot 同名转换(AC-ORD-001-05)") {
          req.deliveryTimeSlot shouldBe DomainDeliveryTimeSlot.AFTERNOON
        }
        withClue("clientRequestId 幂等键透传(R-ORD-06)") { req.clientRequestId shouldBe "req-001" }
        created
      }

      val result =
        resolver.createOrder(
          CreateOrderInput(
            items = listOf(OrderItemInput(productId = 10L, quantity = 2)),
            addressId = 20L,
            deliveryTime = DeliveryTimeSlot.AFTERNOON,
            clientRequestId = "req-001",
          ),
          principal,
        )

      result.orderNo shouldBe "ORD-001"
      verify(exactly = 1) { orderMutationService.createOrder(any()) }
    }
    test("createOrderPaymentAutoCompletedInMockMode") {
      every { orderQueryApiService.findById(1L) } returns order()
      every { userQueryApiService.findById(1L) } returns UserModel(id = 1L, wechatOpenId = "openid-1")
      every { paymentService.createWechatJsapiPay(1L, 1200, any(), "openid-1") } returns
        mapOf("mock" to true, "paid" to true, "transactionId" to "MOCK-ORD-001")

      val result = resolver.createOrderPayment(1L, principal)

      result.autoCompleted shouldBe true
      result.requiresSimulation shouldBe false
      result.paymentParams.shouldNotBeNull()
      result.paymentParams!!.get("mock").asBoolean() shouldBe true
    }
    test("cancelOrderCancelsOwnOrder") {
      every { orderQueryApiService.findById(1L) } returns order(status = OrderModelStatus.PENDING_PAYMENT)
      val cancelled = order(status = OrderModelStatus.CANCELLED)
      every { orderMutationService.cancelOrder(1L, 1L) } returns cancelled

      val result = resolver.cancelOrder(1L, principal)

      result.status.name shouldBe OrderModelStatus.CANCELLED.name
      verify(exactly = 1) { orderMutationService.cancelOrder(1L, 1L) }
    }
    test("cancelOrderRejectsForeignOrder") {
      every { orderQueryApiService.findById(1L) } returns order(userId = 99L)

      shouldThrow<BadRequestException> { resolver.cancelOrder(1L, principal) }
    }
    test("cancelOrderNotFoundThrows") {
      every { orderQueryApiService.findById(1L) } returns null

      shouldThrow<NotFoundException> { resolver.cancelOrder(1L, principal) }
    }
    test("confirmReceiptConfirmsOwnOrder") {
      every { orderQueryApiService.findById(1L) } returns order(status = OrderModelStatus.AWAITING_CONFIRMATION)
      val completed = order(status = OrderModelStatus.COMPLETED)
      every { orderMutationService.confirmReceipt(1L, 1L) } returns completed

      val result = resolver.confirmReceipt(1L, principal)

      result.status.name shouldBe OrderModelStatus.COMPLETED.name
      verify(exactly = 1) { orderMutationService.confirmReceipt(1L, 1L) }
    }
    test("confirmReceiptRejectsForeignOrder") {
      every { orderQueryApiService.findById(1L) } returns order(userId = 99L)

      shouldThrow<BadRequestException> { resolver.confirmReceipt(1L, principal) }
    }
    test("confirmReceiptNotFoundThrows") {
      every { orderQueryApiService.findById(1L) } returns null

      shouldThrow<NotFoundException> { resolver.confirmReceipt(1L, principal) }
    }
    test("createReviewUsesPrincipalUserId") {
      every { reviewMutationService.createReview(any(), any()) } answers {
        val req = secondArg<CreateReviewRequest>()
        req.orderId shouldBe 1L
        withClue("配送员由服务端解析,调用方不传") { (req.deliveryWorkerId == null) shouldBe true }
        ReviewModel(orderId = 1L, userId = 1L, deliveryWorkerId = 5L, rating = 5, comment = "很好", isAnonymous = false)
      }

      val result =
        resolver.createReview(
          CreateReviewRequestInput(orderId = 1L, rating = 5, comment = "很好", deliveryWorkerId = 5L),
          principal,
        )

      result.rating shouldBe 5
      verify(exactly = 1) {
        reviewMutationService.createReview(1L, CreateReviewRequest(orderId = 1L, rating = 5, comment = "很好", isAnonymous = false))
      }
    }
    test("createOrderPaymentRequiresSimulationWhenNotImplemented") {
      every { orderQueryApiService.findById(1L) } returns order()
      every { userQueryApiService.findById(1L) } returns UserModel(id = 1L, wechatOpenId = "openid-1")
      every { paymentService.createWechatJsapiPay(1L, 1200, any(), "openid-1") } throws NotImplementedError("未配置")

      val result = resolver.createOrderPayment(1L, principal)

      result.requiresSimulation shouldBe true
      result.paymentParams shouldBe null
    }
    test("createOrderPaymentRejectsForeignOrder") {
      every { orderQueryApiService.findById(1L) } returns order(userId = 99L)

      shouldThrow<BadRequestException> { resolver.createOrderPayment(1L, principal) }
    }
  })
