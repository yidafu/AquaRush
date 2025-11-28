/*
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

package dev.yidafu.aqua.order.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.common.domain.model.Order
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.order.service.OrderService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(
  private val orderService: OrderService,
) {
  @PostMapping
  fun createOrder(
    @RequestBody order: Order,
  ): ApiResponse<Order> {
    val createdOrder = orderService.createOrder(order)
    return ApiResponse.success(createdOrder)
  }

  @GetMapping("/{orderId}")
  fun getOrder(
    @PathVariable orderId: Long,
  ): ApiResponse<Order> {
    val order = orderService.getOrderById(orderId)
    return ApiResponse.success(order)
  }

  @GetMapping("/number/{orderNumber}")
  fun getOrderByNumber(
    @PathVariable orderNumber: String,
  ): ApiResponse<Order> {
    val order = orderService.getOrderByNumber(orderNumber)
    return ApiResponse.success(order)
  }

  @GetMapping("/user/{userId}")
  fun getUserOrders(
    @PathVariable userId: Long,
  ): ApiResponse<List<Order>> {
    val orders = orderService.getUserOrders(userId)
    return ApiResponse.success(orders)
  }

  @GetMapping("/user/{userId}/status/{status}")
  fun getUserOrdersByStatus(
    @PathVariable userId: Long,
    @PathVariable status: OrderStatus,
  ): ApiResponse<List<Order>> {
    val orders = orderService.getUserOrdersByStatus(userId, status)
    return ApiResponse.success(orders)
  }

  @PostMapping("/{orderId}/cancel")
  fun cancelOrder(
    @PathVariable orderId: Long,
  ): ApiResponse<Order> {
    val order = orderService.cancelOrder(orderId)
    return ApiResponse.success(order)
  }

  @GetMapping("/status/{status}")
  fun getOrdersByStatus(
    @PathVariable status: OrderStatus,
  ): ApiResponse<List<Order>> {
    val orders = orderService.getOrdersByStatus(status)
    return ApiResponse.success(orders)
  }
}
