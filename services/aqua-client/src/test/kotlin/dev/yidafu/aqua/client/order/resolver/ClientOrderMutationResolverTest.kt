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

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 用户端订单 Resolver 单元测试(F-ORD-001/F-ORD-005):
 * 下单 userId 强制取登录用户;发起支付校验归属;假支付模式 autoCompleted=true。
 */
@DisplayName("ClientOrderMutationResolver Tests")
class ClientOrderMutationResolverTest {
  private lateinit var orderMutationService: OrderMutationService
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var paymentService: PaymentService
  private lateinit var userQueryApiService: UserQueryApiService
  private lateinit var resolver: ClientOrderMutationResolver
  private lateinit var principal: UserPrincipal

  @BeforeEach
  fun setUp() {
    orderMutationService = mockk()
    orderQueryApiService = mockk()
    paymentService = mockk()
    userQueryApiService = mockk()
    resolver =
      ClientOrderMutationResolver(
        orderMutationService,
        orderQueryApiService,
        paymentService,
        userQueryApiService,
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

  private fun order(
    id: Long = 1L,
    userId: Long = 1L,
    status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
  ) = OrderModel(
    id = id,
    orderNo = "ORD-001",
    userId = userId,
    productId = 1L,
    quantity = 1,
    amountCents = 1000,
    deliveryFeeCents = 200,
    status = status,
  )

  @Test
  @DisplayName("createOrder:userId 强制取登录用户,返回订单")
  fun createOrderUsesPrincipalUserId() {
    val created = order()
    every { orderMutationService.createOrder(any()) } answers {
      val req = firstArg<dev.yidafu.aqua.api.dto.CreateOrderRequest>()
      assertEquals(1L, req.userId, "userId 必须为登录用户")
      created
    }

    val result =
      resolver.createOrder(
        CreateOrderInput(productId = 10L, addressId = 20L, quantity = 2),
        principal,
      )

    assertEquals("ORD-001", result.orderNo)
    verify(exactly = 1) { orderMutationService.createOrder(any()) }
  }

  @Test
  @DisplayName("createOrderPayment:假支付模式 autoCompleted=true 且返回 mock 参数")
  fun createOrderPaymentAutoCompletedInMockMode() {
    every { orderQueryApiService.findById(1L) } returns order()
    every { userQueryApiService.findById(1L) } returns UserModel(id = 1L, wechatOpenId = "openid-1")
    every { paymentService.createWechatJsapiPay(1L, 1200, any(), "openid-1") } returns
      mapOf("mock" to true, "paid" to true, "transactionId" to "MOCK-ORD-001")

    val result = resolver.createOrderPayment(1L, principal)

    assertTrue(result.autoCompleted)
    assertFalse(result.requiresSimulation)
    assertNotNull(result.paymentParams)
    assertTrue(result.paymentParams!!.get("mock").asBoolean())
  }

  @Test
  @DisplayName("createOrderPayment:支付未接入时 requiresSimulation=true")
  fun createOrderPaymentRequiresSimulationWhenNotImplemented() {
    every { orderQueryApiService.findById(1L) } returns order()
    every { userQueryApiService.findById(1L) } returns UserModel(id = 1L, wechatOpenId = "openid-1")
    every { paymentService.createWechatJsapiPay(1L, 1200, any(), "openid-1") } throws NotImplementedError("未配置")

    val result = resolver.createOrderPayment(1L, principal)

    assertTrue(result.requiresSimulation)
    assertEquals(null, result.paymentParams)
  }

  @Test
  @DisplayName("createOrderPayment:非本人订单拒绝")
  fun createOrderPaymentRejectsForeignOrder() {
    every { orderQueryApiService.findById(1L) } returns order(userId = 99L)

    assertFailsWith<BadRequestException> { resolver.createOrderPayment(1L, principal) }
  }
}
