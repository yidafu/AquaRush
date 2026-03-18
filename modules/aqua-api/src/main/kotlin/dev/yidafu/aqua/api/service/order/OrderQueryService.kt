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

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import org.springframework.data.domain.Page

/**
 * 订单查询服务接口
 */
interface OrderQueryService {
  /**
   * 获取订单详情
   */
  fun getOrderById(orderId: Long): OrderModel

  /**
   * 根据订单号获取订单
   */
  fun getOrderByNo(orderNumber: String): OrderModel

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
   * 根据状态获取订单列表
   */
  fun getOrdersByStatus(status: OrderStatus): List<OrderModel>

  /**
   * 获取所有订单
   */
  fun findAllOrders(): List<OrderModel>

  /**
   * 根据ID和用户ID查询订单
   */
  fun findOrderByIdAndUserId(
    orderId: Long,
    userId: Long,
  ): OrderModel?

  /**
   * 根据订单号和用户ID查询订单
   */
  fun findOrderByNumberAndUserId(
    orderNumber: String,
    userId: Long,
  ): OrderModel?

  /**
   * 查询用户所有订单
   */
  fun findOrdersByUserId(userId: Long): List<OrderModel>

  /**
   * 根据状态字符串查询订单
   */
  fun findOrdersByStatus(status: String): List<OrderModel>

  /**
   * 根据用户ID和状态查询
   */
  fun findOrdersByUserIdAndStatus(
    userId: Long,
    status: String,
  ): List<OrderModel>

  /**
   * 分页搜索订单（管理员使用）
   */
  fun searchOrders(
    keyword: String? = null,
    status: String? = null,
    userId: Long? = null,
    dateFrom: String? = null,
    dateTo: String? = null,
    minAmount: Long? = null,
    maxAmount: Long? = null,
    deliveryWorkerId: Long? = null,
    page: Int = 0,
    size: Int = 20,
    sort: String = "createdAt,desc",
  ): Page<OrderModel>
}
