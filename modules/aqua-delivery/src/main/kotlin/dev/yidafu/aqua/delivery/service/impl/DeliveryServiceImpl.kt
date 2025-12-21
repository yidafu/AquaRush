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

import dev.yidafu.aqua.api.service.DeliveryService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.delivery.mapper.DeliveryAreaMapper
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerModelStatusMapper
import dev.yidafu.aqua.delivery.mapper.DeliveryWorkerStatusMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryServiceImpl(
  private val workerRepository: DeliveryWorkerRepository,
  private val areaRepository: DeliveryAreaRepository,
  private val orderRepository: OrderRepository,
) : DeliveryService {
  private val logger = LoggerFactory.getLogger(DeliveryService::class.java)

  // 配送员管理

  override fun getWorkerById(workerId: Long): DeliveryWorkerModel =
    workerRepository.findById(workerId).orElseThrow {
      NotFoundException("配送员不存在: $workerId")
    }

  override fun getOrderById(orderId: Long): OrderModel =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }

  override fun getAllWorkers(): List<DeliveryWorkerModel> = workerRepository.findAll()

  override fun getOnlineWorkers(): List<DeliveryWorkerModel>
    {
    return workerRepository.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE)
  }

  @Transactional
  override fun updateWorkerStatus(
    workerId: Long,
    status: DeliverWorkerModelStatus
  ): DeliveryWorkerModel {
    val worker = getWorkerById(workerId)
    worker.onlineStatus = status
    return workerRepository.save(worker)
  }

  // 配送区域管理

  override fun isAddressInDeliveryArea(
    province: String,
    city: String,
    district: String,
  ): Boolean {
    val area = areaRepository.findByProvinceAndCityAndDistrict(province, city, district)
    return area != null && area.enabled
  }

  override fun validateDeliveryAddress(
    province: String,
    city: String,
    district: String,
  ) {
    if (!isAddressInDeliveryArea(province, city, district)) {
      throw BadRequestException("该地址不在配送范围内")
    }
  }

  override fun getAllDeliveryAreas(): List<DeliveryAreaModel> = areaRepository.findAll()

  override fun getEnabledDeliveryAreas(): List<DeliveryAreaModel> = areaRepository.findByEnabledTrue()

  @Transactional
  override fun createDeliveryArea(area: DeliveryAreaModel): DeliveryAreaModel = areaRepository.save(area)

  @Transactional
  override fun updateDeliveryArea(
    areaId: Long,
    enabled: Boolean,
  ): DeliveryAreaModel {
    val area =
      areaRepository.findById(areaId).orElseThrow {
        NotFoundException("配送区域不存在: $areaId")
      }
    area.enabled = enabled
    areaRepository.save(area)
    return area
  }

  // 配送任务管理

  /**
   * 分配送水员给订单
   */
  @Transactional
  override fun assignDeliveryWorker(
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
      if (worker.onlineStatus != DeliverWorkerModelStatus.ONLINE) {
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
  override fun autoAssignDeliveryWorker(orderId: Long): Long? {
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
  override fun getWorkerTasks(workerId: Long): List<OrderModel> {
    val worker = getWorkerById(workerId)
    return orderRepository.findByDeliveryWorkerIdOrderByCreatedAtDesc(workerId)
  }

  /**
   * 获取配送员的进行中任务
   */
  override fun getWorkerActiveTasks(workerId: Long): List<OrderModel> {
    val worker = getWorkerById(workerId)
    return orderRepository.findByDeliveryWorkerIdAndStatusOrderByCreatedAtDesc(
      workerId,
      OrderStatus.DELIVERING,
    )
  }

  /**
   * 获取配送员的活跃任务数量
   */
  override fun getWorkerActiveTaskCount(workerId: Long): Int =
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
  override fun completeDelivery(
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
  override fun getPendingDeliveryOrders(): List<OrderModel> =
    orderRepository.findByStatusOrderByCreatedAtAsc(
      OrderStatus.PENDING_DELIVERY,
    )

  /**
   * 获取配送统计数据
   */
  override fun getDeliveryStatistics(): dev.yidafu.aqua.api.service.DeliveryService.DeliveryStatistics {
    val totalWorkers = workerRepository.count()
    val onlineWorkers = getOnlineWorkers().size
    val pendingOrders = getPendingDeliveryOrders().size
    val deliveringOrders =
      orderRepository.countByStatus(
        dev.yidafu.aqua.common.domain.model.OrderStatus.DELIVERING,
      )

    return dev.yidafu.aqua.api.service.DeliveryService.DeliveryStatistics(
      totalWorkers = totalWorkers.toInt(),
      onlineWorkers = onlineWorkers,
      pendingOrders = pendingOrders,
      deliveringOrders = deliveringOrders.toInt(),
    )
  }

  /**
   * Worker load data class for internal use
   */
  private data class WorkerLoad(
    val worker: DeliveryWorkerModel,
    val taskCount: Int,
  )

}
