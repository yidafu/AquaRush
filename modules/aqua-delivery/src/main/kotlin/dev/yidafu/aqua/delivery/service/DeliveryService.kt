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

package dev.yidafu.aqua.delivery.service

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.WorkerStatus
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class DeliveryService(
  private val workerRepository: DeliveryWorkerRepository,
  private val areaRepository: DeliveryAreaRepository,
  private val orderRepository: OrderRepository,
) {
  private val logger = LoggerFactory.getLogger(DeliveryService::class.java)

  // 配送员管理

  fun getWorkerById(workerId: Long): DeliveryWorkerModel =
    workerRepository.findById(workerId).orElseThrow {
      NotFoundException("配送员不存在: $workerId")
    }

  fun getOrderById(orderId: Long): OrderModel =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }

  fun getAllWorkers(): List<DeliveryWorkerModel> = workerRepository.findAll()

  fun getOnlineWorkers(): List<DeliveryWorkerModel> = workerRepository.findByStatus(WorkerStatus.ONLINE)

  @Transactional
  fun updateWorkerStatus(
    workerId: Long,
    status: WorkerStatus,
  ): DeliveryWorkerModel {
    val worker = getWorkerById(workerId)
    worker.status = status
    return workerRepository.save(worker)
  }

  // 配送区域管理

  fun isAddressInDeliveryArea(
    province: String,
    city: String,
    district: String,
  ): Boolean {
    val area = areaRepository.findByProvinceAndCityAndDistrict(province, city, district)
    return area != null && area.enabled
  }

  fun validateDeliveryAddress(
    province: String,
    city: String,
    district: String,
  ) {
    if (!isAddressInDeliveryArea(province, city, district)) {
      throw BadRequestException("该地址不在配送范围内")
    }
  }

  fun getAllDeliveryAreas(): List<DeliveryAreaModel> = areaRepository.findAll()

  fun getEnabledDeliveryAreas(): List<DeliveryAreaModel> = areaRepository.findByEnabledTrue()

  @Transactional
  fun createDeliveryArea(area: DeliveryAreaModel): DeliveryAreaModel = areaRepository.save(area)

  @Transactional
  fun updateDeliveryArea(
    areaId: Long,
    enabled: Boolean,
  ): DeliveryAreaModel {
    val area =
      areaRepository.findById(areaId).orElseThrow {
        NotFoundException("配送区域不存在: $areaId")
      }
    area.enabled = enabled
    return areaRepository.save(area)
  }

  // 配送任务管理

  /**
   * 分配送水员给订单
   */
  @Transactional
  fun assignDeliveryWorker(
    orderId: Long,
    workerId: Long,
  ): Boolean {
    try {
      val order =
        orderRepository.findById(orderId).orElseThrow {
          NotFoundException("订单不存在: $orderId")
        }

      val worker =
        workerRepository.findById(workerId).orElseThrow {
          NotFoundException("配送员不存在: $workerId")
        }

      // 验证订单状态
      if (order.status != dev.yidafu.aqua.common.domain.model.OrderStatus.PENDING_DELIVERY) {
        throw BadRequestException("订单状态不正确，无法分配配送员")
      }

      // 验证配送员状态
      if (worker.status != WorkerStatus.ONLINE) {
        throw BadRequestException("配送员不在线，无法分配任务")
      }

      // 分配配送员
      order.deliveryWorkerId = workerId
      order.status = dev.yidafu.aqua.common.domain.model.OrderStatus.DELIVERING
      orderRepository.save(order)

      logger.info("Successfully assigned worker $workerId to order $orderId")
      return true
    } catch (e: Exception) {
      logger.error("Failed to assign delivery worker to order $orderId", e)
      return false
    }
  }

  /**
   * 自动分配配送员
   * 根据负载均衡和地理位置选择最优配送员
   */
  fun autoAssignDeliveryWorker(orderId: Long): Long? {
    try {
      val order =
        orderRepository.findById(orderId).orElse(null)
          ?: return null

      if (order.status != dev.yidafu.aqua.common.domain.model.OrderStatus.PENDING_DELIVERY) {
        return null
      }

      // 获取所有在线配送员
      val onlineWorkers = getOnlineWorkers()
      if (onlineWorkers.isEmpty()) {
        logger.warn("No online delivery workers available for order $orderId")
        return null
      }

      // 获取每个配送员的当前任务数量
      val workerLoads =
        onlineWorkers.map { worker ->
          val currentTaskCount = getCurrentTaskCount(worker.id!!)
          WorkerLoad(worker, currentTaskCount)
        }

      // 选择任务最少的配送员
      val selectedWorker = workerLoads.minByOrNull { it.taskCount }?.worker

      return if (selectedWorker != null && selectedWorker.id != null) {
        assignDeliveryWorker(orderId, selectedWorker.id!!)
        selectedWorker.id!!
      } else {
        null
      }
    } catch (e: Exception) {
      logger.error("Failed to auto assign delivery worker for order $orderId", e)
      return null
    }
  }

  /**
   * 获取配送员当前任务数量
   */
  private fun getCurrentTaskCount(workerId: Long): Int =
    orderRepository
      .countByDeliveryWorkerIdAndStatus(
        workerId,
        dev.yidafu.aqua.common.domain.model.OrderStatus.DELIVERING,
      ).toInt()

  /**
   * 获取配送员的所有任务
   */
  fun getWorkerTasks(workerId: Long): List<OrderModel> {
    val worker = getWorkerById(workerId)
    return orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(workerId)
  }

  /**
   * 获取配送员的进行中任务
   */
  fun getWorkerActiveTasks(workerId: Long): List<OrderModel> {
    val worker = getWorkerById(workerId)
    return orderRepository.findByDeliveryWorkerIdAndStatusOrderByCreatedAtDesc(
      workerId,
      OrderStatus.DELIVERING,
    )
  }

  /**
   * 获取配送员的活跃任务数量
   */
  fun getWorkerActiveTaskCount(workerId: Long): Int =
    orderRepository
      .countByDeliveryWorkerIdAndStatus(
        workerId,
        dev.yidafu.aqua.common.domain.model.OrderStatus.DELIVERING
      )
      .toInt()

  /**
   * 完成配送任务
   */
  @Transactional
  fun completeDelivery(
    orderId: Long,
    deliveryPhotos: List<String>,
  ): Boolean {
    try {
      val order =
        orderRepository.findById(orderId).orElseThrow {
          NotFoundException("订单不存在: $orderId")
        }

      if (order.status != dev.yidafu.aqua.common.domain.model.OrderStatus.DELIVERING) {
        throw BadRequestException("订单状态不正确，无法完成配送")
      }

      // 更新订单状态
      order.status = dev.yidafu.aqua.common.domain.model.OrderStatus.COMPLETED
      order.deliveryPhotos = deliveryPhotos.joinToString(",")
      order.completedAt = java.time.LocalDateTime.now()
      orderRepository.save(order)

      // 释放配送员（可选择将配送员状态设为空闲，这里保持在线状态）
      // updateWorkerStatus(order.deliveryWorkerId!!, WorkerStatus.ONLINE)

      logger.info("Successfully completed delivery for order $orderId")
      return true
    } catch (e: Exception) {
      logger.error("Failed to complete delivery for order $orderId", e)
      return false
    }
  }

  /**
   * 获取所有待分配的订单
   */
  fun getPendingDeliveryOrders(): List<OrderModel> =
    orderRepository.findByStatusOrderByCreatedAtAsc(
      OrderStatus.PENDING_DELIVERY,
    )

  /**
   * 获取配送统计数据
   */
  fun getDeliveryStatistics(): DeliveryStatistics {
    val totalWorkers = workerRepository.count()
    val onlineWorkers = getOnlineWorkers().size
    val pendingOrders = getPendingDeliveryOrders().size
    val deliveringOrders =
      orderRepository.countByStatus(
        dev.yidafu.aqua.common.domain.model.OrderStatus.DELIVERING,
      )

    return DeliveryStatistics(
      totalWorkers = totalWorkers.toInt(),
      onlineWorkers = onlineWorkers,
      pendingOrders = pendingOrders,
      deliveringOrders = deliveringOrders.toInt(),
    )
  }

  // 内部数据类
  private data class WorkerLoad(
    val worker: DeliveryWorkerModel,
    val taskCount: Int,
  )

  data class DeliveryStatistics(
    val totalWorkers: Int,
    val onlineWorkers: Int,
    val pendingOrders: Int,
    val deliveringOrders: Int,
  )
}
