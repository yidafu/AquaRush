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
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 用户端订单查询 Resolver 测试(F-ORD-003):仅本人可见。
 */
@DisplayName("ClientOrderQueryResolver Tests")
class ClientOrderQueryResolverTest {
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var resolver: ClientOrderQueryResolver
  private lateinit var principal: UserPrincipal

  @BeforeEach
  fun setUp() {
    orderQueryApiService = mockk()
    resolver = ClientOrderQueryResolver(orderQueryApiService)
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
        productId = 1L,
        quantity = 1,
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
        productId = 1L,
        quantity = 1,
        amountCents = 1000,
        status = OrderModelStatus.PENDING_PAYMENT,
      )

    assertFailsWith<BadRequestException> { resolver.order(1L, principal) }
  }
}
