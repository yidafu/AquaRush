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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.delivery.DeliveryTaskQueryService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.springframework.stereotype.Service

@Service
class DeliveryTaskQueryServiceImpl(
  private val workerRepository: DeliveryWorkerRepository,
  private val orderRepository: OrderRepository,
) : DeliveryTaskQueryService {
  override fun getWorkerTasks(adminId: Long): List<OrderModel> = orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(adminId)

  override fun getPendingDeliveryOrders(): List<OrderModel> =
    orderRepository.findByStatusOrderByCreatedAtAsc(
      OrderStatus.PENDING_DISPATCH,
    )

  override fun getAssignedOrders(adminId: Long): List<OrderModel> {
    val worker = workerRepository.findByAdminId(adminId)
    val workerId = worker?.id ?: throw UserNotFoundException("管理员账号未关联送水员")
    return orderRepository
      .findByDeliveryWorkerIdAndStatusOrderByCreatedAtDesc(
        workerId,
        OrderStatus.PENDING_DELIVERY,
      ).filter { it.deliveryStartedAt == null }
  }

  override fun getDeliveryStatistics(): DeliveryTaskQueryService.DeliveryStatistics {
    val totalWorkers = workerRepository.count()
    val onlineWorkers =
      workerRepository
        .findByOnlineStatus(
          dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus.ONLINE,
        ).size
    val pendingOrders = getPendingDeliveryOrders().size
    val deliveringOrders =
      orderRepository.countByStatus(
        OrderStatus.DELIVERING,
      )

    return DeliveryTaskQueryService.DeliveryStatistics(
      totalWorkers = totalWorkers.toInt(),
      onlineWorkers = onlineWorkers,
      pendingOrders = pendingOrders,
      deliveringOrders = deliveringOrders.toInt(),
    )
  }

  override fun getTodayStatistics(workerId: Long?): DeliveryTaskQueryService.TodayStatistics {
    val today = java.time.LocalDate.now()
    val startOfDay = today.atStartOfDay()
    val endOfDay = today.plusDays(1).atStartOfDay()

    val orders =
      if (workerId != null) {
        orderRepository.findByDeliveryWorkerId(workerId)
      } else {
        orderRepository.findAll()
      }

    val todayOrders =
      orders.filter { order ->
        order.createdAt >= startOfDay && order.createdAt < endOfDay
      }

    val completedToday =
      todayOrders.filter { order ->
        order.status == OrderStatus.COMPLETED &&
          order.completedAt != null &&
          order.completedAt!! >= startOfDay && order.completedAt!! < endOfDay
      }

    val pendingToday =
      todayOrders.filter { order ->
        order.status == OrderStatus.DELIVERING
      }

    val totalEarning = completedToday.sumOf { it.amountCents }

    return DeliveryTaskQueryService.TodayStatistics(
      totalOrders = todayOrders.size,
      completedOrders = completedToday.size,
      pendingOrders = pendingToday.size,
      earningCents = totalEarning,
    )
  }

  override fun getOrderById(orderId: Long): OrderModel =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }
}
