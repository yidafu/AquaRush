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

package dev.yidafu.aqua.common.messaging.service

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.messaging.config.SimplifiedMessagingProperties
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.publisher.EventPublisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

/**
 * 简化的事件发布服务
 * 基于ActiveMQ Artemis实现可靠的消息队列
 */
@Service
@ConditionalOnProperty(name = ["aqua.messaging.enabled"], havingValue = "true", matchIfMissing = false)
class SimplifiedEventPublishService(
  private val eventPublisher: EventPublisher,
  private val messagingProperties: SimplifiedMessagingProperties,
) {
  private val logger = LoggerFactory.getLogger(SimplifiedEventPublishService::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 发布领域事件（智能路由）
   * 根据事件类型选择最优发布策略
   */
  @Transactional
  fun publishDomainEvent(
    eventType: String,
    aggregateId: String,
    eventData: Map<String, Any>,
  ): Boolean =
    try {
      val event =
        DomainEvent(
          id = DefaultIdGenerator().generate(),
          aggregateId = aggregateId,
          eventType = eventType,
          payload = objectMapper.writeValueAsString(eventData),
        )

      // 使用ActiveMQ Artemis发布事件（异步发送，不阻塞主请求）
      logger.debug("Publishing event to ActiveMQ Artemis: $eventType")

      // 使用异步发布，避免阻塞主业务流程
      eventPublisher.publishSync(event)
    } catch (e: Exception) {
      logger.error("Failed to publish domain event: $eventType", e)
      false
    }
  // ==================== Long 类型版本 ====================

  /**
   * 发布订单创建事件（Long 类型）
   */
  fun publishOrderCreated(
    orderId: Long,
    userId: Long,
    productId: Long,
    quantity: Int,
    amountCents: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_CREATED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "productId" to productId,
          "quantity" to quantity,
          "amountCents" to amountCents,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布订单支付成功事件（Long 类型）
   */
  fun publishOrderPaid(
    orderId: Long,
    userId: Long,
    productId: Long,
    amountCents: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_PAID",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "productId" to productId,
          "amountCents" to amountCents,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布订单取消事件（Long 类型）
   */
  fun publishOrderCancelled(
    orderId: Long,
    userId: Long,
    reason: String,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_CANCELLED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "reason" to reason,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布订单配送完成事件（Long 类型）
   */
  fun publishDeliveryCompleted(
    orderId: Long,
    deliveryWorkerId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "DELIVERY_COMPLETED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布订单分配事件（Long 类型）
   */
  fun publishDeliveryAssigned(
    orderId: Long,
    deliveryWorkerId: Long,
    userId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "DELIVERY_ASSIGNED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
          "userId" to userId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布配送开始事件（Long 类型）
   */
  fun publishDeliveryStarted(
    orderId: Long,
    deliveryWorkerId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "DELIVERY_STARTED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布订单完成事件（Long 类型）
   */
  fun publishOrderCompleted(
    orderId: Long,
    userId: Long,
    deliveryWorkerId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_COMPLETED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "deliveryWorkerId" to deliveryWorkerId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布支付超时事件（Long 类型）
   */
  fun publishPaymentTimeout(
    orderId: Long,
    userId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = "PAYMENT_TIMEOUT",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布支付相关事件
   */
  fun publishPaymentTimeout(
    orderId: UUID,
    userId: UUID,
  ): Boolean =
    publishDomainEvent(
      eventType = "PAYMENT_TIMEOUT",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 发布配送相关事件
   */
  fun publishDeliveryTimeout(
    orderId: UUID,
    deliveryWorkerId: UUID,
  ): Boolean =
    publishDomainEvent(
      eventType = "DELIVERY_TIMEOUT",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  /**
   * 获取简化的系统状态
   */
  fun getSystemStatus(): Map<String, Any> =
    mapOf(
      "messagingEnabled" to messagingProperties.enabled,
      "strategy" to "artemis",
    )

  /**
   * 检查发布器健康状态
   */
  fun isPublisherHealthy(): Boolean =
    try {
      eventPublisher.isAvailable()
    } catch (e: Exception) {
      logger.error("Error checking publisher health", e)
      false
    }
}
