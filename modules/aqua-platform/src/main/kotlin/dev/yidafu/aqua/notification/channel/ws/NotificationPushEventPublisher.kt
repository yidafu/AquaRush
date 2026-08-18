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

package dev.yidafu.aqua.notification.channel.ws

import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import jakarta.jms.Topic
import org.slf4j.LoggerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * WebSocket 推送事件发布器(PRD 13-notification.md F-NTF-003)
 *
 * 消息网关落库后,把推送载荷发布到 broadcast-events 主题(多播);
 * 两个服务各自消费并推送到本服务在线会话,实现跨服务实时触达
 * (用户会话连 aqua-client,送水工会话连 aqua-admin,无需关心网关落在哪个服务)。
 */
@Component
class NotificationPushEventPublisher(
  private val jmsTemplate: JmsTemplate,
) {
  private val logger = LoggerFactory.getLogger(NotificationPushEventPublisher::class.java)
  private val objectMapper = jacksonObjectMapper()

  @Volatile
  private var topic: Topic? = null

  fun publish(event: PushEvent) {
    try {
      val destination = topic ?: jmsTemplate.execute { session -> session.createTopic(PushEvent.TOPIC) } ?: return
      topic = destination
      jmsTemplate.convertAndSend(destination, objectMapper.writeValueAsString(event))
    } catch (e: Exception) {
      logger.error("WebSocket 推送事件发布失败: {}", event, e)
    }
  }
}

/**
 * WebSocket 推送事件载荷
 */
data class PushEvent(
  val receiverType: String, // USER | WORKER
  val receiverId: Long? = null,
  val broadcast: Boolean = false,
  val payload: Map<String, Any?>,
) {
  companion object {
    /** 复用既有 broadcast-events 多播主题 */
    const val TOPIC: String = EventQueueConst.BROADCAST_TOPIC_NAME
  }
}
