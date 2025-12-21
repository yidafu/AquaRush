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

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel

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
   */
  fun assignDeliveryWorker(
    orderId: Long,
    workerId: Long,
  ): Boolean

  /**
   * 自动分配配送员
   * 根据负载均衡和地理位置选择最优配送员
   */
  fun autoAssignDeliveryWorker(orderId: Long): Long?

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
   */
  fun completeDelivery(
    orderId: Long,
    deliveryPhotos: List<String>,
  ): Boolean

  /**
   * 获取所有待分配的订单
   */
  fun getPendingDeliveryOrders(): List<OrderModel>

  /**
   * 获取配送统计数据
   */
  fun getDeliveryStatistics(): DeliveryStatistics

  data class DeliveryStatistics(
    val totalWorkers: Int,
    val onlineWorkers: Int,
    val pendingOrders: Int,
    val deliveringOrders: Int,
  )
}
