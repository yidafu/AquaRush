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

import dev.yidafu.aqua.common.domain.model.DomainEvent
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.messaging.config.SimplifiedMessagingProperties
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
          eventType = eventType,
          payload = objectMapper.writeValueAsString(eventData),
        )

      // 使用ActiveMQ Artemis发布事件
      logger.debug("Publishing event to ActiveMQ Artemis: $eventType")

      eventPublisher.publishSync(event)
    } catch (e: Exception) {
      logger.error("Failed to publish domain event: $eventType", e)
      false
    }

  /**
   * 批量发布领域事件（优化版）
   */
  @Transactional
  fun publishDomainEventsBatch(events: List<Triple<String, String, Map<String, Any>>>): List<Boolean> =
    try {
      val domainEvents =
        events.map { (eventType, aggregateId, eventData) ->
          DomainEvent(
            id = DefaultIdGenerator().generate(),
            eventType = eventType,
            payload = objectMapper.writeValueAsString(eventData),
          )
        }

      val results = eventPublisher.publishBatchSync(domainEvents)

      // 记录批量发布统计
      val successCount = results.count { it }
      val totalCount = results.size
      logger.info("Batch publish completed: $successCount/$totalCount successful")

      results
    } catch (e: Exception) {
      logger.error("Failed to publish batch domain events", e)
      List(events.size) { false }
    }

  /**
   * 发布订单相关事件
   */
  fun publishOrderCreated(
    orderId: UUID,
    userId: UUID,
    productId: UUID,
    quantity: Int,
    amount: java.math.BigDecimal,
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
          "amount" to amount,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  fun publishOrderPaid(
    orderId: UUID,
    userId: UUID,
    productId: UUID,
    amount: java.math.BigDecimal,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_PAID",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "productId" to productId,
          "amount" to amount,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  fun publishOrderCancelled(
    orderId: UUID,
    userId: UUID,
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

  fun publishOrderDelivered(
    orderId: UUID,
    deliveryWorkerId: UUID,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_DELIVERED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  fun publishOrderAssigned(
    orderId: UUID,
    deliveryWorkerId: UUID,
  ): Boolean =
    publishDomainEvent(
      eventType = "ORDER_ASSIGNED",
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
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
      "strategy" to messagingProperties.strategy,
      "memoryQueue" to
        mapOf(
          "enabled" to messagingProperties.memoryQueue.enabled,
          "maxSize" to messagingProperties.memoryQueue.maxSize,
          "batchSize" to messagingProperties.memoryQueue.batchSize,
          "pollIntervalMs" to messagingProperties.memoryQueue.pollIntervalMs,
          "highFrequencyEvents" to messagingProperties.memoryQueue.highFrequencyEvents,
        ),
      "outbox" to
        mapOf(
          "enabled" to messagingProperties.outbox.enabled,
          "pollIntervalSeconds" to messagingProperties.outbox.pollIntervalSeconds,
          "maxRetryCount" to messagingProperties.outbox.maxRetryCount,
          "cleanupDays" to messagingProperties.outbox.cleanupDays,
        ),
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

  /**
   * 快速发布高频事件（专用方法）
   */
  fun publishHighFrequencyEvent(
    eventType: String,
    aggregateId: String,
    eventData: Map<String, Any>,
  ): Boolean =
    try {
      val event =
        DomainEvent(
          id = DefaultIdGenerator().generate(),
          eventType = eventType,
          payload = objectMapper.writeValueAsString(eventData),
        )

      logger.debug("Publishing high-frequency event: $eventType")
      eventPublisher.publishSync(event)
    } catch (e: Exception) {
      logger.error("Failed to publish high-frequency event: $eventType", e)
      false
    }
}
