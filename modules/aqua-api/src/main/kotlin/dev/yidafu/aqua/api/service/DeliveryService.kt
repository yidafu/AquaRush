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
 * along with this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 */

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.*

/**
 * 配送服务接口
 */
interface DeliveryService {
  // 配送员管理

  fun getWorkerById(workerId: Long): DeliveryWorkerModel

  fun getOrderById(orderId: Long): OrderModel

  fun getAllWorkers(): List<DeliveryWorkerModel>

  fun getOnlineWorkers(): List<DeliveryWorkerModel>

  fun updateWorkerStatus(
    workerId: Long,
    status: DeliverWorkerModelStatus,
  ): DeliveryWorkerModel

  // 配送区域管理

  fun isAddressInDeliveryArea(
    province: String,
    city: String,
    district: String,
  ): Boolean

  fun validateDeliveryAddress(
    province: String,
    city: String,
    district: String,
  )

  fun getAllDeliveryAreas(): List<DeliveryAreaModel>

  fun getEnabledDeliveryAreas(): List<DeliveryAreaModel>

  fun createDeliveryArea(area: DeliveryAreaModel): DeliveryAreaModel

  fun updateDeliveryArea(
    areaId: Long,
    enabled: Boolean,
  ): DeliveryAreaModel

  // 配送任务管理

  /**
   * 分配送水员给订单
   * @param orderId 订单ID
   * @param workerId 配送员ID
   * @param isSelfCollect 是否自收（水钱已收/水票已扣）
   */
  fun assignDeliveryWorker(
    orderId: Long,
    workerId: Long,
    isSelfCollect: Boolean,
  ): OrderModel

  /**
   * 批量分配订单给配送员
   * @param orderIds 订单ID列表
   * @param workerId 配送员ID
   */
  fun batchAssignOrders(
    orderIds: List<Long>,
    workerId: Long,
  ): List<OrderModel>

  /**
   * 自动分配配送员
   * 根据负载均衡和地理位置选择最优配送员
   */
  fun autoAssignDeliveryWorker(orderId: Long): Long?

  /**
   * 配送员接单
   * @param orderId 订单ID
   * @param workerId 配送员ID
   */
  fun acceptDelivery(orderId: Long, workerId: Long): OrderModel

  /**
   * 开始配送（配送员点击开始配送按钮）
   * @param orderId 订单ID
   */
  fun startDelivery(orderId: Long): OrderModel

  /**
   * 获取配送员的所有任务
   */
  fun getWorkerTasks(workerId: Long): List<OrderModel>

  /**
   * 获取配送员的进行中任务
   */
  fun getWorkerActiveTasks(workerId: Long): List<OrderModel>

  /**
   * 获取配送员的活跃任务数量
   */
  fun getWorkerActiveTaskCount(workerId: Long): Int

  /**
   * 完成配送任务
   * @param orderId 订单ID
   * @param deliveryPhotos 配送照片列表
   * @param paymentType 收款方式（非自收订单需要记录）
   */
  fun completeDelivery(
    orderId: Long,
    deliveryPhotos: List<String>,
    paymentType: PaymentType?,
  ): OrderModel

  /**
   * 获取所有待分配的订单
   */
  fun getPendingDeliveryOrders(): List<OrderModel>

  /**
   * 获取配送员的已接单未开始配送的订单
   */
  fun getAssignedOrders(workerId: Long): List<OrderModel>

  /**
   * 获取配送统计数据
   */
  fun getDeliveryStatistics(): DeliveryStatistics

  /**
   * 获取配送员当日统计数据
   * @param workerId 配送员ID，如果为null则返回所有配送员的统计数据
   */
  fun getTodayStatistics(workerId: Long?): TodayStatistics

  data class DeliveryStatistics(
    val totalWorkers: Int,
    val onlineWorkers: Int,
    val pendingOrders: Int,
    val deliveringOrders: Int,
  )

  data class TodayStatistics(
    val totalOrders: Int,
    val completedOrders: Int,
    val pendingOrders: Int,
    val earningCents: Long,
  )
}
