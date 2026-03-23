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

package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.api.query.CreateOrderRequest
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus

/**
 * 订单变更服务接口
 */
interface OrderMutationService {
  /**
   * 创建订单，给普通用户使用
   */
  fun createOrder(input: CreateOrderRequest): OrderModel

  /**
   * 配送员创建订单
   */
  fun createDeliveryOrder(
    adminId: Long,
    input: CreateOrderRequest,
  ): OrderModel

  /**
   * 取消订单
   */
  fun cancelOrder(orderId: Long): OrderModel

  /**
   * 用户取消订单
   */
  fun cancelOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel?

  /**
   * 管理员取消订单
   */
  fun cancelOrderForAdmin(orderId: Long): OrderModel?

  /**
   * 更新订单状态
   */
  fun updateOrderStatus(
    orderId: Long,
    status: OrderStatus,
  ): OrderModel

  /**
   * 更新订单状态（字符串版本）
   */
  fun updateOrderStatus(
    orderId: Long,
    status: String,
  ): OrderModel?

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
}
