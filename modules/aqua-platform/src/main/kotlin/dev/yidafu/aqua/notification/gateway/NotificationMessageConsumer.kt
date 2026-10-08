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
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

/**
 * 通知消息消费者(PRD 13-notification.md)
 * 监听 notifications-events 队列,消费 NOTIFICATION_SEND 事件,交由消息网关处理;
 * 处理失败抛出异常触发 Artemis 重投(幂等键保证重投不产生重复消息)。
 */
@Component
class NotificationMessageConsumer(
  private val notificationGatewayService: NotificationGatewayService,
) {
  private val logger = LoggerFactory.getLogger(NotificationMessageConsumer::class.java)

  @JmsListener(destination = EventQueueConst.NOTIFICATIONS_QUEUE_NAME)
  fun handleNotificationEvent(event: DomainEvent) {
    try {
      logger.debug("处理通知事件: {}", event.eventType)
      notificationGatewayService.handle(event)
    } catch (e: Exception) {
      logger.error("处理通知事件失败: {}, eventId={}", event.eventType, event.id, e)
      throw e
    }
  }
}
