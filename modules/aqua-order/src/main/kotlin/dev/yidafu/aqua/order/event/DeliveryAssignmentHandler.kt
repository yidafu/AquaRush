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

package dev.yidafu.aqua.order.event

import dev.yidafu.aqua.api.service.DeliveryService
import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.DomainEventModel
import dev.yidafu.aqua.common.domain.model.enums.EventStatusModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class DeliveryAssignmentHandler(
  private val orderRepository: OrderRepository,
  private val addressRepository: AddressRepository,
  private val deliveryService: DeliveryService,
) {
  private val logger = LoggerFactory.getLogger(DeliveryAssignmentHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 处理配送分配事件
   */
  @Transactional
  fun handle(event: DomainEventModel) {
    try {
      // 解析payload获取事件数据
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )

      val orderId = eventData["orderId"].toString().toLong()
      val order =
        orderRepository
          .findById(orderId)
          .orElseThrow { IllegalStateException("Order not found: $orderId") }

      logger.info("Processing DELIVERY_ASSIGNMENT event for order: ${order.orderNumber}")

      // 获取配送地址信息
      val address =
        addressRepository
          .findById(order.addressId)
          .orElseThrow { IllegalStateException("Address not found: ${order.addressId}") }

      // 分配送水员
      val assignedWorker = assignDeliveryWorker(order, address)

      if (assignedWorker != null) {
        // 更新订单状态和配送员信息
        order.status = OrderStatus.DELIVERING
        order.deliveryWorkerId = assignedWorker.id
        orderRepository.save(order)

        // 创建配送分配成功事件
        createDeliveryAssignedEvent(order, assignedWorker)

        logger.info("Successfully assigned delivery worker ${assignedWorker.id} to order ${order.orderNumber}")
      } else {
        logger.warn("No available delivery workers found for order: ${order.orderNumber}")

        // 可以创建配送分配失败事件或发送告警
        // createDeliveryAssignmentFailedEvent(order)
      }
    } catch (e: Exception) {
      logger.error("Failed to process DELIVERY_ASSIGNMENT event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }

  /**
   * 分配送水员
   * 简单实现：随机选择一个在线的送水员
   * 实际应用中可以根据地理位置、负载等因素进行智能分配
   */
  private fun assignDeliveryWorker(
    order: OrderModel,
    address: AddressModel,
  ): DeliveryWorkerModel? {
    try {
      // 获取所有在线送水员
      val onlineWorkers = deliveryService.getOnlineWorkers()

      if (onlineWorkers.isEmpty()) {
        logger.warn("No online delivery workers available")
        return null
      }

      // 简单的负载均衡：选择当前任务最少的在线送水员
      // 这里简化实现，随机选择一个送水员
      val selectedWorker = onlineWorkers.random()

      logger.info("Selected delivery worker ${selectedWorker.id} for order ${order.orderNumber}")

      return selectedWorker
    } catch (e: Exception) {
      logger.error("Error assigning delivery worker for order ${order.orderNumber}", e)
      return null
    }
  }

  /**
   * 创建配送分配成功事件
   */
  private fun createDeliveryAssignedEvent(
    order: OrderModel,
    worker: DeliveryWorkerModel,
  ) {
    try {
      val eventData =
        mapOf(
          "orderId" to order.id.toString(),
          "orderNumber" to order.orderNumber,
          "deliveryWorkerId" to worker.id.toString(),
          "deliveryWorkerName" to worker.name,
          "deliveryWorkerPhone" to worker.phone,
          "userId" to order.userId.toString(),
        )

      val eventPayload = objectMapper.writeValueAsString(eventData)

      val event =
        DomainEventModel(
          id = DefaultIdGenerator().generate(),
          eventType = "ORDER_ASSIGNED",
          payload = eventPayload,
          status =  EventStatusModel.PENDING,
          retryCount = 0,
          nextRunAt = java.time.LocalDateTime.now(),
          createdAt = java.time.LocalDateTime.now(),
          updatedAt = java.time.LocalDateTime.now(),
          errorMessage = null,
        )

      // 保存事件（需要DomainEventRepository的注入，这里简化）
      logger.info("Created ORDER_ASSIGNED event for order ${order.orderNumber}")
    } catch (e: Exception) {
      logger.error("Error creating delivery assigned event for order ${order.orderNumber}", e)
    }
  }
}
