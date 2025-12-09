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

package dev.yidafu.aqua.common.domain.service

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import java.util.*

/**
 * Order service interface to avoid circular dependencies
 */
interface OrderService {
  fun getOrderById(orderId: Long): OrderModel

  fun getOrderByNumber(orderNumber: String): OrderModel

  fun handlePaymentSuccess(
    orderId: Long,
    transactionId: String,
  )

  fun handlePaymentTimeout(orderId: Long)

  // Additional methods for GraphQL resolvers
  fun createOrder(input: Any, userId: Long): OrderModel
  fun cancelOrder(orderId: Long, userId: Long): OrderModel?
  fun cancelOrderForAdmin(orderId: Long): OrderModel?
  fun updateOrderStatus(orderId: Long, status: String): OrderModel?
  fun findAllOrders(): List<OrderModel>
  fun findOrderByIdAndUserId(orderId: Long, userId: Long): OrderModel?
  fun findOrderByNumberAndUserId(orderNumber: String, userId: Long): OrderModel?
  fun findOrdersByUserId(userId: Long): List<OrderModel>
  fun findOrdersByStatus(status: String): List<OrderModel>
  fun findOrdersByUserIdAndStatus(userId: Long, status: String): List<OrderModel>
}
