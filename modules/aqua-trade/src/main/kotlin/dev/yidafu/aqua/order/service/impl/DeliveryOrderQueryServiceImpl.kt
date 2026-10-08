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

import dev.yidafu.aqua.api.dto.DailyStatDTO
import dev.yidafu.aqua.api.dto.DeliveryStatisticsDTO
import dev.yidafu.aqua.api.dto.TodayStatisticsDTO
import dev.yidafu.aqua.api.dto.WeekStatisticsDTO
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.DeliveryOrderQueryService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DeliveryOrderQueryServiceImpl(
  private val adminService: AdminService,
  private val orderRepository: OrderRepository,
) : DeliveryOrderQueryService {
  override fun getWorkerTasks(workerId: Long): List<OrderModel> = orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(workerId)

  override fun getOrdersByStatus(
    workerId: Long,
    status: OrderModelStatus,
  ): List<OrderModel> =
    orderRepository.findByStatusOrderByCreatedAtAsc(
      status,
    )

  override fun getPendingDeliveryOrders(): List<OrderModel> =
    orderRepository.findByStatusOrderByCreatedAtAsc(
      OrderModelStatus.PENDING_DISPATCH,
    )

  override fun getAssignedOrders(adminId: Long): List<OrderModel> {
    // 送水员档案并入 admins 单表后,账号 id 即送水员 id(迁移 v1.2.1)
    val worker = adminService.findById(adminId) ?: throw UserNotFoundException("管理员账号未关联送水员")
    if (worker.role != AdminRoleModel.DELIVERY_WORKER && worker.role != AdminRoleModel.ADMIN) {
      throw UserNotFoundException("管理员账号未关联送水员")
    }
    val workerId = worker.id ?: throw UserNotFoundException("管理员账号未关联送水员")
    return getOrdersByStatus(workerId, OrderModelStatus.PENDING_DELIVERY)
  }

  override fun getDeliveryStatistics(): DeliveryStatisticsDTO {
    val allWorkers = adminService.findAllDeliveryCapable()
    val totalWorkers = allWorkers.size
    val onlineWorkers =
      adminService
        .findByOnlineStatus(
          DeliverWorkerModelStatus.ONLINE,
        ).size
    val pendingOrders = getPendingDeliveryOrders().size
    val deliveringOrders =
      orderRepository.countByStatus(
        OrderModelStatus.DELIVERING,
      )

    return DeliveryStatisticsDTO(
      totalWorkers = totalWorkers.toInt(),
      onlineWorkers = onlineWorkers,
      pendingOrders = pendingOrders,
      deliveringOrders = deliveringOrders.toInt(),
    )
  }

  override fun getTodayStatistics(workerId: Long?): TodayStatisticsDTO {
    val today = LocalDate.now()
    val startOfDay = today.atStartOfDay()
    val endOfDay = today.plusDays(1).atStartOfDay()

    // Use unified repository method to query statistics from database
    val totalOrders = orderRepository.countOrdersByDateRange(startOfDay, endOfDay, workerId, null).toInt()
    // 已完成/业绩口径(R-ORD-11):含 待确认收货 + 已完成,按送达时间归属 ——
    // 送水工业绩按送达即时计入,不受用户确认收货延迟影响
    val completedOrders = orderRepository.countDeliveredByDateRange(startOfDay, endOfDay, workerId).toInt()
    val unfinishedOrders =
      orderRepository
        .countOrdersByDateRange(
          startOfDay,
          endOfDay,
          workerId,
          listOf(
            OrderModelStatus.DELIVERING,
            OrderModelStatus.PENDING_DELIVERY,
            OrderModelStatus.PENDING_DISPATCH,
            OrderModelStatus.PENDING_DELIVERY,
          ),
        ).toInt()
    val earningCents = orderRepository.sumDeliveredAmountCentsByDateRange(startOfDay, endOfDay, workerId)

    return TodayStatisticsDTO(
      totalOrders = totalOrders,
      completedOrders = completedOrders,
      unfinishedOrders = unfinishedOrders,
      earningCents = earningCents,
    )
  }

  override fun getOrderById(orderId: Long): OrderModel =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }

  override fun getWeekStatistics(workerId: Long?): WeekStatisticsDTO {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val dailyStats = mutableListOf<DailyStatDTO>()
    var totalOrders = 0
    var totalEarningCents = 0L

    // 遍历最近7天
    for (i in 6 downTo 0) {
      val date = today.minusDays(i.toLong())
      val startOfDay = date.atStartOfDay()
      val endOfDay = date.plusDays(1).atStartOfDay()

      // 业绩口径(R-ORD-11):含 待确认收货 + 已完成,按送达时间归属
      val orderCount = orderRepository.countDeliveredByDateRange(startOfDay, endOfDay, workerId).toInt()
      val earningCents = orderRepository.sumDeliveredAmountCentsByDateRange(startOfDay, endOfDay, workerId)

      dailyStats.add(
        DailyStatDTO(
          date = date.format(dateFormatter),
          orderCount = orderCount,
          earningCents = earningCents,
        ),
      )
      totalOrders += orderCount
      totalEarningCents += earningCents
    }

    return WeekStatisticsDTO(
      dailyStats = dailyStats,
      totalOrders = totalOrders,
      totalEarningCents = totalEarningCents,
    )
  }
}
