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

package dev.yidafu.aqua.notification.gateway

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.outbox.OutboxDestinationHandler
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 通知消息 outbox 处理器(PRD 13-notification.md)
 * 消费 notifications-events 队列的 NOTIFICATION_SEND 事件,交由消息网关落库+分发;
 * 失败异常上抛 → outbox 退避重试(幂等键保证重试不产生重复消息)。
 */
@Component
class NotificationMessageConsumer(
  private val notificationGatewayService: NotificationGatewayService,
) : OutboxDestinationHandler {
  private val logger = LoggerFactory.getLogger(NotificationMessageConsumer::class.java)
  private val objectMapper = jacksonObjectMapper()

  override val destinations: Set<String> = setOf(EventQueueConst.NOTIFICATIONS_QUEUE_NAME)

  override fun handle(record: OutboxEventRecord) {
    val event = objectMapper.readValue(record.payload, DomainEvent::class.java)
    logger.debug("处理通知事件: {}", event.eventType)
    notificationGatewayService.handle(event)
  }
}
