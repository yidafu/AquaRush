/*
 * AquaRush Admin Order Query Resolver
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

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.Order
import dev.yidafu.aqua.order.service.OrderService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@AdminService
@Controller("adminOrderQueryResolver")
class OrderQueryResolver(
  private val orderService: OrderService,
) {

  /**
   * 获取所有订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun orders(): List<Order> {
    return orderService.findAllOrders()
  }

  /**
   * 根据ID获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun order(@Argument orderId: Long): Order? {
    return orderService.getOrderById(orderId)
  }

  /**
   * 根据订单号获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun orderByNumber(@Argument orderNumber: String): Order? {
    return orderService.getOrderByNumber(orderNumber)
  }

  /**
   * 根据用户ID获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun ordersByUser(@Argument userId: Long): List<Order> {
    return orderService.findOrdersByUserId(userId)
  }

  /**
   * 根据状态获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun ordersByStatus(@Argument status: String): List<Order> {
    return orderService.findOrdersByStatus(status)
  }

  /**
   * 根据用户ID和状态获取订单 - 管理员权限
   */
  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun ordersByUserAndStatus(
    @Argument userId: Long,
    @Argument status: String,
  ): List<Order> {
    return orderService.findOrdersByUserIdAndStatus(userId, status)
  }
}
