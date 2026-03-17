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

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OperatorType
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderOperationType
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class OrderPaidHandler(
  private val orderRepository: OrderRepository,
  private val orderOperationService: OrderOperationService,
  private val simplifiedEventPublishService: SimplifiedEventPublishService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(OrderPaidHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_PAID

  /**
   * 处理订单支付成功事件
   */
  @Transactional
  override fun handle(event: DomainEvent) {
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

      // 记录订单操作
      orderOperationService.recordOperation(
        orderId = order.id,
        operationType = OrderOperationType.ORDER_PAID,
        operatorType = OperatorType.USER,
        operatorId = order.userId,
        description = "订单支付成功",
      )

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
    // 通过 Artemis 发布配送分配事件
    val eventData =
      mapOf(
        "orderId" to order.id.toString(),
        "orderNumber" to order.orderNumber,
        "userId" to order.userId.toString(),
        "productId" to order.productId.toString(),
        "addressId" to order.addressId.toString(),
      )

    simplifiedEventPublishService.publishDomainEvent(
      eventType = "ORDER_DELIVERY_ASSIGNMENT",
      aggregateId = order.id.toString(),
      eventData = eventData,
    )

    logger.info("Published ORDER_DELIVERY_ASSIGNMENT event for order: ${order.orderNumber}")
  }
}
