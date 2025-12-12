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

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.delivery.service.DeliveryService
import dev.yidafu.aqua.order.domain.model.DomainEventModel
import dev.yidafu.aqua.order.domain.model.EventStatusModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class OrderPaidHandler(
  private val orderRepository: OrderRepository,
  private val deliveryService: DeliveryService,
  private val deliveryAssignmentHandler: DeliveryAssignmentHandler,
) {
  private val logger = LoggerFactory.getLogger(OrderPaidHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 处理订单支付成功事件
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

      logger.info("Processing ORDER_PAID event for order: ${order.orderNumber}")

      // 验证订单状态
      if (order.status != OrderStatus.PENDING_DELIVERY) {
        logger.warn("Order ${order.orderNumber} is not in PENDING_DELIVERY status, current status: ${order.status}")
        return
      }

      // 触发配送分配
      triggerDeliveryAssignment(order)

      logger.info("Successfully processed ORDER_PAID event for order: ${order.orderNumber}")
    } catch (e: Exception) {
      logger.error("Failed to process ORDER_PAID event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }

  /**
   * 触发配送分配
   */
  private fun triggerDeliveryAssignment(order: OrderModel) {
    // 创建配送分配事件
    val eventData =
      mapOf(
        "orderId" to order.id.toString(),
        "orderNumber" to order.orderNumber,
        "userId" to order.userId.toString(),
        "productId" to order.productId.toString(),
        "addressId" to order.addressId.toString(),
      )

    val eventPayload = objectMapper.writeValueAsString(eventData)

    val deliveryAssignmentEvent =
      DomainEventModel(
        id = DefaultIdGenerator().generate(),
        eventType = "ORDER_DELIVERY_ASSIGNMENT",
        payload = eventPayload,
        status = EventStatusModel.PENDING,
        retryCount = 0,
        nextRunAt = java.time.LocalDateTime.now(),
        createdAt = java.time.LocalDateTime.now(),
        updatedAt = java.time.LocalDateTime.now(),
        errorMessage = null,
      )

    // 这里可以保存事件到数据库，或者直接调用配送服务
    // 为了简化，直接调用配送分配处理器
    deliveryAssignmentHandler.handle(deliveryAssignmentEvent)
  }
}
