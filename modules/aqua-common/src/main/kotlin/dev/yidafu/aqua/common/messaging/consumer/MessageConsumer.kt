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

package dev.yidafu.aqua.common.messaging.consumer

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import jakarta.jms.Session
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

/**
 * 消息消费者
 * 监听ActiveMQ Artemis队列并处理消息
 */
@Component
class MessageConsumer(
  private val orderEventProcessors: List<EventProcessor>,
) {
  private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

  // 构建事件类型到处理器的映射
  private val eventProcessorMap: Map<DomainEventType, List<EventProcessor>> by lazy {
    orderEventProcessors.groupBy { it.getSupportedEventType() }
  }

  @JmsListener(destination = EventQueueConst.ORDER_QUEUE_NAME)
  fun handleOrderEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理订单事件
      logger.info("处理订单事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processEventProcessor(event)
      logger.debug("订单事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理订单事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      // 这里我们让异常抛出，消息会自动重新入队
      throw e
    }
  }

  @JmsListener(destination = EventQueueConst.PAYMENT_QUEUE_NAME)
  fun handlePaymentEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理支付事件
      logger.info("处理支付事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processEventProcessor(event)
      logger.debug("支付事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理支付事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      throw e
    }
  }

  @JmsListener(destination = EventQueueConst.DELIVERY_QUEUE_NAME)
  fun handleDeliveryEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理配送事件
      logger.info("处理配送事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processEventProcessor(event)
      logger.debug("配送事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理配送事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      throw e
    }
  }

  @JmsListener(destination = EventQueueConst.USER_QUEUE_NAME)
  fun handleUserEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理用户事件
      logger.info("处理用户事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processEventProcessor(event)
      logger.debug("用户事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理用户事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      throw e
    }
  }

  private fun processEventProcessor(event: DomainEvent) {
    // 订单事件处理逻辑
    logger.info("执行事件处理逻辑: {}", event.eventType)
    val eventType = DomainEventType.fromValue(event.eventType)
    // 根据事件类型分发到对应的处理器
    val processorList = eventProcessorMap[eventType]
    if (processorList != null) {
      processorList.forEach { p -> p.handle(event) }
    } else {
      logger.warn("未知的事件类型: ${event.eventType}")
    }
  }
}
