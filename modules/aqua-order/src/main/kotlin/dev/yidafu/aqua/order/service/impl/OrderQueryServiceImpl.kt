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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service

/**
 * 订单查询服务实现
 */
@Service
class OrderQueryServiceImpl(
  private val orderRepository: OrderRepository,
) : OrderQueryService {
  override fun getOrderById(orderId: Long): OrderModel =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }

  override fun getOrderByNo(orderNumber: String): OrderModel =
    orderRepository.findByOrderNo(orderNumber)
      ?: throw NotFoundException("订单不存在: $orderNumber")

  override fun getUserOrders(userId: Long): List<OrderModel> = orderRepository.findByUserId(userId)

  override fun getUserOrdersByStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel> = orderRepository.findByUserIdAndStatus(userId, status)

  override fun getOrdersByStatus(status: OrderModelStatus): List<OrderModel> = orderRepository.findByStatus(status)

  override fun findAllOrders(): List<OrderModel> = orderRepository.findAll()

  override fun findOrderByIdAndUserId(
    orderId: Long,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findById(orderId).orElse(null) ?: return null
    return if (order.userId == userId) order else null
  }

  override fun findOrderByNumberAndUserId(
    orderNumber: String,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findByOrderNo(orderNumber) ?: return null
    return if (order.userId == userId) order else null
  }

  override fun findOrdersByUserId(userId: Long): List<OrderModel> = orderRepository.findByUserId(userId)

  override fun findOrdersByStatus(status: String): List<OrderModel> =
    try {
      val orderStatus = OrderModelStatus.valueOf(status.uppercase())
      getOrdersByStatus(orderStatus)
    } catch (e: Exception) {
      emptyList()
    }

  override fun findOrdersByUserIdAndStatus(
    userId: Long,
    status: String,
  ): List<OrderModel> =
    try {
      val orderStatus = OrderModelStatus.valueOf(status.uppercase())
      getUserOrdersByStatus(userId, orderStatus)
    } catch (e: Exception) {
      emptyList()
    }

  override fun searchOrders(
    keyword: String?,
    status: String?,
    userId: Long?,
    dateFrom: String?,
    dateTo: String?,
    minAmount: Long?,
    maxAmount: Long?,
    deliveryWorkerId: Long?,
    page: Int,
    size: Int,
    sort: String,
  ): Page<OrderModel> {
    // Parse sort string (e.g., "createdAt,desc")
    val sortParts = sort.split(",")
    val sortField = sortParts.getOrElse(0) { "createdAt" }
    val sortDirection = sortParts.getOrElse(1) { "desc" }

    // Convert date strings to LocalDateTime
    val startDate =
      dateFrom?.let {
        try {
          java.time.LocalDateTime.parse(it)
        } catch (e: Exception) {
          null
        }
      }
    val endDate =
      dateTo?.let {
        try {
          java.time.LocalDateTime.parse(it)
        } catch (e: Exception) {
          null
        }
      }

    // Convert status string to OrderStatus enum
    val orderStatus =
      status?.let {
        try {
          OrderModelStatus.valueOf(it.uppercase())
        } catch (e: Exception) {
          null
        }
      }

    return orderRepository.findOrdersPaginated(
      keyword = keyword,
      status = orderStatus,
      userId = userId,
      deliveryWorkerId = deliveryWorkerId,
      startDate = startDate,
      endDate = endDate,
      minAmount = minAmount,
      maxAmount = maxAmount,
      page = page,
      size = size,
      sortField = sortField,
      sortDirection = sortDirection,
    )
  }

  override fun getDeliveryWorkerHistoryOrders(
    workerId: Long?,
    status: OrderModelStatus?,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    // 历史订单状态：已完成、已取消、已退款
    val historyStatuses =
      if (status != null) {
        listOf(status)
      } else {
        listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED, OrderModelStatus.REFUNDED)
      }

    return orderRepository.findByDeliveryWorkerIdAndStatusIn(
      deliveryWorkerId = workerId,
      statuses = historyStatuses,
      page = page,
      size = size,
    )
  }
}
