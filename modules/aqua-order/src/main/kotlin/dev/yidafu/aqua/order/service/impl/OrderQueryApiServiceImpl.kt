/**
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

import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 跨模块订单查询服务实现
 * 供其他模块使用
 */
@Service
class OrderQueryApiServiceImpl(
  private val orderRepository: OrderRepository,
) : OrderQueryApiService {
  override fun findById(id: Long): OrderModel? = orderRepository.findById(id).orElse(null)

  override fun findByIds(ids: List<Long>): List<OrderModel> = orderRepository.findAllById(ids)

  override fun findByOrderNumber(orderNumber: String): OrderModel? = orderRepository.findByOrderNo(orderNumber)

  override fun findByUserId(userId: Long): List<OrderModel> = orderRepository.findByUserId(userId)

  override fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel> = orderRepository.findByUserIdAndStatus(userId, status)

  override fun findByStatus(status: OrderModelStatus): List<OrderModel> = orderRepository.findByStatus(status)

  override fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<OrderModel> =
    orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId)

  override fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel> =
    orderRepository.findAll().filter {
      it.createdAt?.isAfter(startDate) == true && it.createdAt?.isBefore(endDate) == true
    }

  override fun findByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel> =
    orderRepository.findAll().filter { order ->
      order.status in statuses &&
        order.createdAt?.isAfter(startDate) == true &&
        order.createdAt?.isBefore(endDate) == true
    }

  override fun countByStatusAndCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long {
    val orders =
      orderRepository.findAll().filter { order ->
        order.createdAt?.isAfter(startDate) == true &&
          order.createdAt?.isBefore(endDate) == true &&
          (statuses == null || order.status in statuses) &&
          (deliveryWorkerId == null || order.deliveryWorkerId == deliveryWorkerId)
      }
    return orders.size.toLong()
  }

  override fun sumAmountCentsByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long {
    val orders =
      orderRepository.findAll().filter { order ->
        order.status in statuses &&
          order.createdAt?.isAfter(startDate) == true &&
          order.createdAt?.isBefore(endDate) == true &&
          (deliveryWorkerId == null || order.deliveryWorkerId == deliveryWorkerId)
      }
    return orders.sumOf { it.amountCents ?: 0L }
  }
}
