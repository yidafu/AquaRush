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

package dev.yidafu.aqua.common.messaging.publisher

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import org.apache.activemq.artemis.core.server.plugin.impl.LoggingActiveMQServerPlugin

/**
 * 事件发布器接口
 * 定义了事件发布的核心方法，支持同步和异步发布
 */
interface EventPublisher {
  /**
   * 异步发布事件
   * @param event 要发布的事件
   * @return 发布是否成功
   */
  suspend fun publish(event: DomainEvent): Boolean

  /**
   * 同步发布事件
   * @param event 要发布的事件
   * @return 发布是否成功
   */
  fun publishSync(event: DomainEvent): Boolean

  /**
   * 获取发布器名称
   * @return 发布器名称
   */
  fun getName(): String

  /**
   * 检查发布器是否可用
   * @return 是否可用
   */
  fun isAvailable(): Boolean

  /**
   * 获取发布器类型
   * @return 发布器类型
   */
  fun getType(): EventTypePublisherType

  /**
   * 获取优先级（数字越小优先级越高）
   * @return 优先级
   */
  fun getPriority(): Int = 100

  /**
   * 批量发布事件
   * @param events 要发布的事件列表
   * @return 发布结果列表
   */
  suspend fun publishBatch(events: List<DomainEvent>): List<Boolean> = events.map { publish(it) }

  /**
   * 同步批量发布事件
   * @param events 要发布的事件列表
   * @return 发布结果列表
   */
  fun publishBatchSync(events: List<DomainEvent>): List<Boolean> = events.map { publishSync(it) }
}

/**
 * 事件发布器类型
 */
enum class EventTypePublisherType {
  /**
   * ActiveMQ Artemis模式（可靠且高性能）
   */
  ARTEMIS,
}

object EventQueueConst {
  const val ORDER_QUEUE_NAME = "order-events"
  const val PAYMENT_QUEUE_NAME = "payment-events"
  const val DELIVERY_QUEUE_NAME = "delivery-events"
  const val USER_QUEUE_NAME = "user-events"
  const val NOTIFICATIONS_QUEUE_NAME = "notifications-events"
  const val COMMON_QUEUE_NAME = "common-events"
}

enum class EventQueueName(
  private val tag: String,
) {
  ORDER_QUEUE(EventQueueConst.ORDER_QUEUE_NAME),
  PAYMENT_QUEUE(EventQueueConst.PAYMENT_QUEUE_NAME),
  DELIVERY_QUEUE(EventQueueConst.DELIVERY_QUEUE_NAME),
  USER_QUEUE(EventQueueConst.USER_QUEUE_NAME),
  NOTIFICATION_QUEUE(EventQueueConst.NOTIFICATIONS_QUEUE_NAME),
  COMMON_QUEUE(EventQueueConst.COMMON_QUEUE_NAME),
  ;

  override fun toString(): String = tag
}
