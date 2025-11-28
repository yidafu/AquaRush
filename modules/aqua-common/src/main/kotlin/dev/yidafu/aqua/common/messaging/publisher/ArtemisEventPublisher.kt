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

package dev.yidafu.aqua.common.messaging.publisher

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component

/**
 * ActiveMQ Artemis事件发布器
 * 基于ActiveMQ Artemis的消息队列实现，提供可靠且高性能的事件发布机制
 */
@Component
class ArtemisEventPublisher : EventPublisher {
  private val logger = LoggerFactory.getLogger(ArtemisEventPublisher::class.java)
  private val objectMapper = jacksonObjectMapper()

  @Autowired
  private lateinit var jmsTemplate: JmsTemplate

  override suspend fun publish(event: DomainEvent): Boolean =
    try {
      jmsTemplate.convertAndSend(getDestinationForEventType(event.eventType), event)
      logger.debug("异步消息已发送到Artemis队列: {}", event.eventType)
      true
    } catch (e: Exception) {
      logger.error("发送异步消息到Artemis队列失败: {}", event.eventType, e)
      false
    }

  override fun publishSync(event: DomainEvent): Boolean =
    try {
      jmsTemplate.sendAndReceive(getDestinationForEventType(event.eventType)) { session ->
        session.createTextMessage(objectMapper.writeValueAsString(event))
      }
      logger.debug("同步消息发送成功: {}", event.eventType)
      true
    } catch (e: Exception) {
      logger.error("同步消息发送失败: {}", event.eventType, e)
      false
    }

  override fun getName(): String = "ArtemisEventPublisher"

  override fun isAvailable(): Boolean = true

  override fun getType(): EventTypePublisherType = EventTypePublisherType.ARTEMIS

  override fun getPriority(): Int = 1 // 最高优先级

  override suspend fun publishBatch(events: List<DomainEvent>): List<Boolean> =
    events.map { event ->
      try {
        jmsTemplate.convertAndSend(getDestinationForEventType(event.eventType), event)
        logger.debug("批量消息已发送到Artemis队列: {}", event.eventType)
        true
      } catch (e: Exception) {
        logger.error("发送批量消息到Artemis队列失败: {}", event.eventType, e)
        false
      }
    }

  override fun publishBatchSync(events: List<DomainEvent>): List<Boolean> =
    events.map { event ->
      try {
        jmsTemplate.sendAndReceive(getDestinationForEventType(event.eventType)) { session ->
          session.createTextMessage(objectMapper.writeValueAsString(event))
        }
        logger.debug("批量同步消息发送成功: {}", event.eventType)
        true
      } catch (e: Exception) {
        logger.error("批量同步消息发送失败: {}", event.eventType, e)
        false
      }
    }

  /**
   * 根据事件类型确定目标队列
   */
  private fun getDestinationForEventType(eventType: String): String =
    when (eventType) {
      "ORDER_CREATED", "ORDER_PAID", "ORDER_CANCELLED", "ORDER_DELIVERED", "ORDER_ASSIGNED" -> "order-events"
      "PAYMENT_TIMEOUT" -> "payment-events"
      "DELIVERY_TIMEOUT" -> "delivery-events"
      else -> "user-events"
    }
}
