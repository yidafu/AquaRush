package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus

/**
 * 配送任务查询服务接口
 */
interface DeliveryOrderQueryService {
  /**
   * 获取配送员的所有任务
   */
  fun getWorkerTasks(workerId: Long): List<OrderModel>

  /**
   * 获取配送员的所有任务
   */
  fun getOrdersByStatus(
    workerId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  /**
   * 获取所有待分配的订单
   */
  fun getPendingDeliveryOrders(): List<OrderModel>

  /**
   * 获取配送员的已接单未开始配送的订单
   */
  fun getAssignedOrders(adminId: Long): List<OrderModel>

  /**
   * 获取配送统计数据
   */
  fun getDeliveryStatistics(): DeliveryStatistics

  /**
   * 获取配送员当日统计数据
   * @param workerId 配送员ID，如果为null则返回所有配送员的统计数据
   */
  fun getTodayStatistics(workerId: Long?): TodayStatistics

  /**
   * 根据ID获取订单
   */
  fun getOrderById(orderId: Long): OrderModel

  data class DeliveryStatistics(
    val totalWorkers: Int,
    val onlineWorkers: Int,
    val pendingOrders: Int,
    val deliveringOrders: Int,
  )

  data class TodayStatistics(
    val totalOrders: Int,
    val completedOrders: Int,
    val unfinishedOrders: Int,
    val earningCents: Long,
  )
}
