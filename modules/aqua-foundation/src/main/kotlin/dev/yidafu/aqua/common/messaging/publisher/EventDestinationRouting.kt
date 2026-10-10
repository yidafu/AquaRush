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

import dev.yidafu.aqua.common.messaging.event.DomainEventType

/**
 * 事件类型 → 目标队列路由(发布侧与 outbox 消费分发共用)。
 * 路由表变更需同步消费端订阅配置(aqua.outbox.destinations)。
 */
object EventDestinationRouting {
  fun destinationFor(eventType: String): String {
    val domainEventType = DomainEventType.fromValue(eventType)
    return when (domainEventType) {
      DomainEventType.ORDER_CREATED,
      DomainEventType.ORDER_PAID,
      DomainEventType.ORDER_CANCELLED,
      DomainEventType.ORDER_COMPLETED,
      DomainEventType.ORDER_DELIVERED,
      DomainEventType.ORDER_URGING,
      -> EventQueueName.ORDER_QUEUE.toString()

      DomainEventType.PAYMENT_TIMEOUT,
      DomainEventType.PAYMENT_REFUNDED,
      -> EventQueueName.PAYMENT_QUEUE.toString()

      // 支付到期检查触发:独立队列,仅 admin 服务消费(R-ORD-04 支付超时优化)
      DomainEventType.PAYMENT_EXPIRY_CHECK,
      -> EventQueueName.PAYMENT_EXPIRY_QUEUE.toString()

      DomainEventType.DELIVERY_ASSIGNED,
      DomainEventType.DELIVERY_STARTED,
      DomainEventType.DELIVERY_COMPLETED,
      DomainEventType.DELIVERY_TIMEOUT,
      -> EventQueueName.DELIVERY_QUEUE.toString()

      DomainEventType.USER_REGISTERED,
      DomainEventType.USER_UPDATED,
      DomainEventType.COUPON_EXPIRING,
      -> EventQueueName.USER_QUEUE.toString()

      DomainEventType.NOTIFICATION_SEND,
      -> EventQueueName.NOTIFICATION_QUEUE.toString()

      else -> EventQueueName.DELIVERY_QUEUE.toString()
    }
  }
}
