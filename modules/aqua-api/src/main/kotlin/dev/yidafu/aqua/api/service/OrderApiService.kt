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

import dev.yidafu.aqua.api.common.PagedResponse
import dev.yidafu.aqua.api.dto.*
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.utils.MoneyUtils
import java.math.BigDecimal
import java.util.*

/**
 * 订单API服务接口
 */
interface OrderApiService {
  /**
   * 创建新订单
   */
  fun createOrder(request: CreateOrderRequest): OrderDTO

  /**
   * 获取订单详情
   */
  fun getOrderById(orderId: Long): OrderDTO?

  /**
   * 获取用户订单列表
   */
  fun getUserOrders(
    userId: Long,
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<OrderDTO>

  /**
   * 获取用户订单列表（按状态筛选）
   */
  fun getUserOrdersByStatus(
    userId: Long,
    status: OrderStatus,
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<OrderDTO>

  /**
   * 取消订单
   */
  fun cancelOrder(
    orderId: Long,
    reason: String,
  ): OrderDTO

  /**
   * 确认订单送达
   */
  fun confirmOrderDelivered(orderId: Long): OrderDTO

  /**
   * 获取订单统计信息
   */
  fun getOrderStatistics(userId: Long): OrderStatisticsDTO

  /**
   * 更新订单状态
   */
  fun updateOrderStatus(
    orderId: Long,
    status: OrderStatus,
  ): OrderDTO

  /**
   * 分配配送员
   */
  fun assignDeliveryWorker(
    orderId: Long,
    workerId: Long,
  ): OrderDTO

  /**
   * 获取配送员当前订单
   */
  fun getDeliveryWorkerCurrentOrders(workerId: Long): List<OrderDTO>

  /**
   * 获取附近待分配订单
   */
  fun getNearbyPendingOrders(
    latitude: BigDecimal,
    longitude: BigDecimal,
    radiusKm: Int = 5,
  ): List<OrderDTO>

  /**
   * 批量更新订单状态
   */
  fun batchUpdateOrderStatus(
    orderIds: List<Long>,
    status: OrderStatus,
  ): List<OrderDTO>

  /**
   * 重新计算订单金额
   */
  fun recalculateOrderAmount(orderId: Long): OrderDTO
}

/**
 * 订单统计DTO
 */
data class OrderStatisticsDTO(
  val totalOrders: Int = 0,
  val pendingOrders: Int = 0,
  val paidOrders: Int = 0,
  val deliveredOrders: Int = 0,
  val cancelledOrders: Int = 0,
  val totalAmountCents: Long = 0L,
  val averageOrderValueCents: Long = 0L,
) {
  val totalAmount: BigDecimal get() = MoneyUtils.fromCents(totalAmountCents)
  val averageOrderValue: BigDecimal get() = MoneyUtils.fromCents(averageOrderValueCents)
}
