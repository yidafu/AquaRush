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

package dev.yidafu.aqua.common.messaging.consumer

import dev.yidafu.aqua.common.domain.model.DomainEvent
import jakarta.jms.Session
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

/**
 * 消息消费者
 * 监听ActiveMQ Artemis队列并处理消息
 */
@Component
class MessageConsumer {
  private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

  @JmsListener(destination = "order-events")
  fun handleOrderEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理订单事件
      logger.info("处理订单事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processOrderEvent(event)
      logger.debug("订单事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理订单事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      // 这里我们让异常抛出，消息会自动重新入队
      throw e
    }
  }

  @JmsListener(destination = "payment-events")
  fun handlePaymentEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理支付事件
      logger.info("处理支付事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processPaymentEvent(event)
      logger.debug("支付事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理支付事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      throw e
    }
  }

  @JmsListener(destination = "delivery-events")
  fun handleDeliveryEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理配送事件
      logger.info("处理配送事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processDeliveryEvent(event)
      logger.debug("配送事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理配送事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      throw e
    }
  }

  @JmsListener(destination = "user-events")
  fun handleUserEvent(
    event: DomainEvent,
    session: Session,
  ) {
    try {
      // 处理用户事件
      logger.info("处理用户事件: {}", event.eventType)
      // 具体的业务逻辑处理
      processUserEvent(event)
      logger.debug("用户事件处理成功: {}", event.eventType)
    } catch (e: Exception) {
      logger.error("处理用户事件失败: {}", event.eventType, e)
      // 根据需要决定是否重新入队或发送到死信队列
      throw e
    }
  }

  private fun processOrderEvent(event: DomainEvent) {
    // 订单事件处理逻辑
    logger.info("执行订单事件处理逻辑: {}", event.eventType)
    // 这里应该调用相应的业务服务来处理订单事件
  }

  private fun processPaymentEvent(event: DomainEvent) {
    // 支付事件处理逻辑
    logger.info("执行支付事件处理逻辑: {}", event.eventType)
    // 这里应该调用相应的业务服务来处理支付事件
  }

  private fun processDeliveryEvent(event: DomainEvent) {
    // 配送事件处理逻辑
    logger.info("执行配送事件处理逻辑: {}", event.eventType)
    // 这里应该调用相应的业务服务来处理配送事件
  }

  private fun processUserEvent(event: DomainEvent) {
    // 用户事件处理逻辑
    logger.info("执行用户事件处理逻辑: {}", event.eventType)
    // 这里应该调用相应的业务服务来处理用户事件
  }
}
