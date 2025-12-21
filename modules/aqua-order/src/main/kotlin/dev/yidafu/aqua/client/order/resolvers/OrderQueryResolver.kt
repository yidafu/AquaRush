/*
 * AquaRush Client Order Query Resolver
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

import dev.yidafu.aqua.api.service.OrderService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@ClientService
@Controller("clientOrderQueryResolver")
class OrderQueryResolver(
  private val orderService: OrderService,
) {

  /**
   * 获取当前用户的订单 - 客户端
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myOrders(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<OrderModel> {
    return orderService.findOrdersByUserId(userPrincipal.id)
  }

  /**
   * 根据ID获取订单（仅限当前用户的订单）- 客户端
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun order(
    @Argument orderId: Long,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): OrderModel? {
    return orderService.findOrderByIdAndUserId(orderId, userPrincipal.id)
      ?: throw IllegalArgumentException("Order not found or access denied")
  }

  /**
   * 根据订单号获取订单（仅限当前用户的订单）- 客户端
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun orderByNumber(
    @Argument orderNumber: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): OrderModel? {
    return orderService.findOrderByNumberAndUserId(orderNumber, userPrincipal.id)
      ?: throw IllegalArgumentException("Order not found or access denied")
  }

  /**
   * 根据状态获取当前用户的订单 - 客户端
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun ordersByStatus(
    @Argument status: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): List<OrderModel> {
    return orderService.findOrdersByUserIdAndStatus(userPrincipal.id, status)
  }
}
