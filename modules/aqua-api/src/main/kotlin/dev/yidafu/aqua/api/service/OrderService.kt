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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus

/**
 * 订单服务接口
 */
interface OrderService {
  /**
   * 创建订单
   */
  fun createOrder(
    userId: Long,
    productId: Long,
    addressId: Long,
    quantity: Int,
  ): OrderModel

  /**
   * 获取订单详情
   */
  fun getOrderById(orderId: Long): OrderModel

  /**
   * 根据订单号获取订单
   */
  fun getOrderByNumber(orderNumber: String): OrderModel

  /**
   * 获取用户订单列表
   */
  fun getUserOrders(userId: Long): List<OrderModel>

  /**
   * 根据状态获取用户订单列表
   */
  fun getUserOrdersByStatus(
    userId: Long,
    status: OrderStatus,
  ): List<OrderModel>

  /**
   * 取消订单
   */
  fun cancelOrder(orderId: Long): OrderModel

  /**
   * 更新订单状态
   */
  fun updateOrderStatus(
    orderId: Long,
    status: OrderStatus,
  ): OrderModel

  /**
   * 根据状态获取订单列表
   */
  fun getOrdersByStatus(status: OrderStatus): List<OrderModel>

  /**
   * 处理支付成功
   */
  fun handlePaymentSuccess(
    orderId: Long,
    paymentTransactionId: String,
  )

  /**
   * 处理支付超时
   */
  fun handlePaymentTimeout(orderId: Long)

  // Additional methods for GraphQL resolvers
  fun createOrder(
    input: Any,
    userId: Long,
  ): OrderModel

  fun cancelOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel?

  fun cancelOrderForAdmin(orderId: Long): OrderModel?

  fun updateOrderStatus(
    orderId: Long,
    status: String,
  ): OrderModel?

  fun findAllOrders(): List<OrderModel>

  fun findOrderByIdAndUserId(
    orderId: Long,
    userId: Long,
  ): OrderModel?

  fun findOrderByNumberAndUserId(
    orderNumber: String,
    userId: Long,
  ): OrderModel?

  fun findOrdersByUserId(userId: Long): List<OrderModel>

  fun findOrdersByStatus(status: String): List<OrderModel>

  fun findOrdersByUserIdAndStatus(
    userId: Long,
    status: String,
  ): List<OrderModel>
}
