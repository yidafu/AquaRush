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

package dev.yidafu.aqua.common.messaging.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component

/**
 * Spring Messaging配置属性
 */
@Component
@ConfigurationProperties(prefix = "aqua.messaging")
data class MessagingProperties(
  /**
   * 是否启用消息队列
   */
  var enabled: Boolean = false,
  /**
   * 处理策略: hybrid, messaging-only, outbox-only
   */
  var strategy: String = "hybrid",
  /**
   * RabbitMQ配置
   */
  @NestedConfigurationProperty
  var rabbitmq: RabbitMQProperties = RabbitMQProperties(),
)

/**
 * RabbitMQ配置属性
 */
data class RabbitMQProperties(
  /**
   * 事件交换机配置
   */
  @NestedConfigurationProperty
  var events: EventExchangeProperties = EventExchangeProperties(),
)

/**
 * 事件交换机配置属性
 */
data class EventExchangeProperties(
  /**
   * 交换机名称
   */
  var exchange: String = "aqua.events",
  /**
   * 路由键前缀
   */
  var routingKeyPrefix: String = "aqua.event.",
  /**
   * 队列配置
   */
  @NestedConfigurationProperty
  var queues: QueueProperties = QueueProperties(),
  /**
   * 死信队列配置
   */
  @NestedConfigurationProperty
  var dlq: DeadLetterQueueProperties = DeadLetterQueueProperties(),
)

/**
 * 队列配置属性
 */
data class QueueProperties(
  /**
   * 订单事件队列
   */
  var orderEvents: String = "aqua.order.events",
  /**
   * 支付事件队列
   */
  var paymentEvents: String = "aqua.payment.events",
  /**
   * 配送事件队列
   */
  var deliveryEvents: String = "aqua.delivery.events",
  /**
   * 用户事件队列
   */
  var userEvents: String = "aqua.user.events",
)

/**
 * 死信队列配置属性
 */
data class DeadLetterQueueProperties(
  /**
   * 是否启用死信队列
   */
  var enabled: Boolean = true,
  /**
   * 死信交换机名称
   */
  var exchange: String = "aqua.events.dlq",
  /**
   * 死信路由键前缀
   */
  var routingKeyPrefix: String = "aqua.event.dlq.",
  /**
   * 死信队列TTL (毫秒)
   */
  var ttl: Long = 86400000L, // 24小时
)
