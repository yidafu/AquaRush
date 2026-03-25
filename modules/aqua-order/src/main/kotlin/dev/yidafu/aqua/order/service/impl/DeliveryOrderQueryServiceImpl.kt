package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.order.DeliveryOrderQueryService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DeliveryOrderQueryServiceImpl(
  private val workerRepository: DeliveryWorkerRepository,
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
    val worker = workerRepository.findByAdminId(adminId)
    val workerId = worker?.id ?: throw UserNotFoundException("管理员账号未关联送水员")
    return getOrdersByStatus(workerId, OrderModelStatus.PENDING_DELIVERY)
  }

  override fun getDeliveryStatistics(): DeliveryOrderQueryService.DeliveryStatistics {
    val totalWorkers = workerRepository.count()
    val onlineWorkers =
      workerRepository
        .findByOnlineStatus(
          DeliverWorkerModelStatus.ONLINE,
        ).size
    val pendingOrders = getPendingDeliveryOrders().size
    val deliveringOrders =
      orderRepository.countByStatus(
        OrderModelStatus.DELIVERING,
      )

    return DeliveryOrderQueryService.DeliveryStatistics(
      totalWorkers = totalWorkers.toInt(),
      onlineWorkers = onlineWorkers,
      pendingOrders = pendingOrders,
      deliveringOrders = deliveringOrders.toInt(),
    )
  }

  override fun getTodayStatistics(workerId: Long?): DeliveryOrderQueryService.TodayStatistics {
    val today = LocalDate.now()
    val startOfDay = today.atStartOfDay()
    val endOfDay = today.plusDays(1).atStartOfDay()

    // Use unified repository method to query statistics from database
    val totalOrders = orderRepository.countOrdersByDateRange(startOfDay, endOfDay, workerId, null).toInt()
    val completedOrders =
      orderRepository
        .countOrdersByDateRange(startOfDay, endOfDay, workerId, listOf(OrderModelStatus.COMPLETED))
        .toInt()
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
    val earningCents =
      orderRepository.sumAmountCentsByStatusAndDateRange(
        listOf(OrderModelStatus.COMPLETED),
        startOfDay,
        endOfDay,
        workerId,
      )

    return DeliveryOrderQueryService.TodayStatistics(
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

  override fun getWeekStatistics(workerId: Long?): DeliveryOrderQueryService.WeekStatistics {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val dailyStats = mutableListOf<DeliveryOrderQueryService.DailyStat>()
    var totalOrders = 0
    var totalEarningCents = 0L

    // 遍历最近7天
    for (i in 6 downTo 0) {
      val date = today.minusDays(i.toLong())
      val startOfDay = date.atStartOfDay()
      val endOfDay = date.plusDays(1).atStartOfDay()

      val orderCount = orderRepository
        .countOrdersByDateRange(startOfDay, endOfDay, workerId, listOf(OrderModelStatus.COMPLETED))
        .toInt()
      val earningCents = orderRepository.sumAmountCentsByStatusAndDateRange(
        listOf(OrderModelStatus.COMPLETED),
        startOfDay,
        endOfDay,
        workerId,
      )

      dailyStats.add(
        DeliveryOrderQueryService.DailyStat(
          date = date.format(dateFormatter),
          orderCount = orderCount,
          earningCents = earningCents,
        ),
      )
      totalOrders += orderCount
      totalEarningCents += earningCents
    }

    return DeliveryOrderQueryService.WeekStatistics(
      dailyStats = dailyStats,
      totalOrders = totalOrders,
      totalEarningCents = totalEarningCents,
    )
  }
}
