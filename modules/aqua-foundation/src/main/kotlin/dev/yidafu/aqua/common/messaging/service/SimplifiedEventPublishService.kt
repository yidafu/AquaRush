/**
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
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.publisher.EventPublisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 简化的事件发布服务
 * 基于数据库 Transactional Outbox 实现可靠事件投递(spec/tech/design/outbox-messaging.md)
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
   *
   * outbox 语义:INSERT 与调用方业务事务同库同事务,序列化/INSERT 失败向上传播
   * (回滚业务事务,保证「业务成功 ⟺ 事件落库」),不吞异常返回 false。
   */
  @Transactional
  fun publishDomainEvent(
    eventType: String,
    aggregateId: String,
    eventData: Map<String, Any>,
  ): Boolean {
    val event =
      DomainEvent(
        id = DefaultIdGenerator().generate(),
        aggregateId = aggregateId,
        eventType = eventType,
        payload = objectMapper.writeValueAsString(eventData),
      )
    logger.debug("Publishing event to outbox: $eventType")
    return eventPublisher.publishSync(event)
  }

  /**
   * 延迟发布领域事件(延迟消息,R-ORD-04 支付超时优化):
   * 写未来 next_run_at,由 outbox 轮询器在到期时刻认领投递;
   * 失败向上传播(与 [publishDomainEvent] 同语义)。
   * 注:私有方法上的 @Transactional 不生效(Spring AOP 代理),事务边界由
   * 调用方(业务方法)或 OutboxEventRepository 自身的 transaction(database) 决定。
   */
  private fun publishDomainEventDelayed(
    eventType: String,
    aggregateId: String,
    eventData: Map<String, Any>,
    deliveryTimeMillis: Long,
  ): Boolean {
    val event =
      DomainEvent(
        id = DefaultIdGenerator().generate(),
        aggregateId = aggregateId,
        eventType = eventType,
        payload = objectMapper.writeValueAsString(eventData),
      )
    return eventPublisher.publishDelayed(event, deliveryTimeMillis)
  }
  // ==================== Long 类型版本 ====================

  /**
   * 发布订单创建事件（Long 类型）
   */
  fun publishOrderCreated(
    orderId: Long,
    userId: Long,
    amountCents: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = DomainEventType.ORDER_CREATED.value,
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
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
    amountCents: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = DomainEventType.ORDER_PAID.value,
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
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
    shouldRefund: Boolean = false,
    paymentTransactionId: String? = null,
  ): Boolean =
    publishDomainEvent(
      eventType = DomainEventType.ORDER_CANCELLED.value,
      aggregateId = orderId.toString(),
      eventData =
        buildMap {
          put("orderId", orderId)
          put("userId", userId)
          put("reason", reason)
          put("shouldRefund", shouldRefund)
          paymentTransactionId?.let { put("paymentTransactionId", it) }
          put("timestamp", System.currentTimeMillis())
        },
    )

  /**
   * 发布订单配送完成事件（Long 类型）
   */
  fun publishDeliveryCompleted(
    orderId: Long,
    deliveryWorkerId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = DomainEventType.DELIVERY_COMPLETED.value,
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
    adminId: Long,
    orderId: Long,
    deliveryWorkerId: Long,
    userId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = DomainEventType.DELIVERY_ASSIGNED.value,
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "adminId" to adminId,
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
      eventType = DomainEventType.DELIVERY_STARTED.value,
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
      eventType = DomainEventType.ORDER_COMPLETED.value,
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
   * 发布支付到期检查延迟消息(R-ORD-04 支付超时优化):
   * 创建/重置待支付记录时发布,outbox 在 expireAt 时刻(到期后)允许认领到
   * payment-expiry-events,仅 admin 服务消费触发支付超时处理;
   * 消费端以 payments 记录守卫(状态+到期时间)保证幂等。
   */
  fun publishPaymentExpiryCheck(
    orderId: Long,
    userId: Long,
    expireAt: LocalDateTime,
  ): Boolean =
    publishDomainEventDelayed(
      eventType = DomainEventType.PAYMENT_EXPIRY_CHECK.value,
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "userId" to userId,
          "expireAt" to expireAt.toString(),
          "timestamp" to System.currentTimeMillis(),
        ),
      deliveryTimeMillis = expireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

  /**
   * 发布支付超时事件（Long 类型）
   */
  fun publishPaymentTimeout(
    orderId: Long,
    userId: Long,
  ): Boolean =
    publishDomainEvent(
      eventType = DomainEventType.PAYMENT_TIMEOUT.value,
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
      eventType = DomainEventType.PAYMENT_TIMEOUT.value,
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
      eventType = DomainEventType.DELIVERY_TIMEOUT.value,
      aggregateId = orderId.toString(),
      eventData =
        mapOf(
          "orderId" to orderId,
          "deliveryWorkerId" to deliveryWorkerId,
          "timestamp" to System.currentTimeMillis(),
        ),
    )

  fun publishAddressUpdate(
    aggregateId: Long,
    addressId: Long,
  ) {
    publishDomainEvent(
      eventType = DomainEventType.ADDRESS_UPDATED.value,
      aggregateId = aggregateId.toString(),
      eventData = mapOf("addressId" to addressId),
    )
  }

  /**
   * 获取简化的系统状态
   */
  fun getSystemStatus(): Map<String, Any> =
    mapOf(
      "messagingEnabled" to messagingProperties.enabled,
      "strategy" to "outbox",
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
