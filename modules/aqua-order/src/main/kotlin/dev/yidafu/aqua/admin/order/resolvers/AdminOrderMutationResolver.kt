/*
 * AquaRush Admin Order Mutation Resolver
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
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.order.mapper.OrderMapper
import dev.yidafu.aqua.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@AdminService
@Controller("adminOrderMutationResolver")
class AdminOrderMutationResolver(
  private val orderService: OrderService,
) {

  /**
   * 创建订单 - 管理员权限（管理员可以为任何用户创建订单）
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun createOrder(
    @Argument @Valid input: CreateOrderInput,
    @Argument userId: Long,
  ): Order {
    return OrderMapper.map(orderService.createOrder(input, userId))
  }

  /**
   * 取消订单 - 管理员权限
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun cancelOrder(@Argument orderId: Long): Order {
    return orderService.cancelOrderForAdmin(orderId)?.let { OrderMapper.map(it) }

      ?: throw IllegalArgumentException("Order not found")
  }

  /**
   * 更新订单状态 - 管理员权限
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun updateOrderStatus(
    @Argument orderId: Long,
    @Argument status: OrderStatus,
  ): Order {
    return orderService.updateOrderStatus(orderId, status.name)
      ?.let { OrderMapper.map(it) }
      ?: throw IllegalArgumentException("Order not found")
  }
}
