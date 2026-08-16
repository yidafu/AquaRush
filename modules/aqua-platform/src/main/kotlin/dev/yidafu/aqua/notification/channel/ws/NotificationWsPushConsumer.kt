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

import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * WebSocket 推送事件消费者(PRD 13-notification.md F-NTF-003)
 *
 * 监听 broadcast-events 主题(多播,两个服务各自收到全部推送事件),把推送载荷
 * 分发到本服务的在线会话:定向(USER/WORKER 按接收人)或广播(全部在线送水工)。
 */
@Component
class NotificationWsPushConsumer(
  private val hub: NotificationWsHub,
) {
  private val logger = LoggerFactory.getLogger(NotificationWsPushConsumer::class.java)
  private val objectMapper = jacksonObjectMapper()

  @JmsListener(
    destination = PushEvent.TOPIC,
    containerFactory = "notificationTopicJmsListenerContainerFactory",
  )
  fun onPushEvent(payload: String) {
    try {
      val event = objectMapper.readValue(payload, PushEvent::class.java)
      val payloadJson = objectMapper.writeValueAsString(event.payload)
      if (event.broadcast) {
        val delivered = hub.broadcastToWorkers(payloadJson)
        logger.debug("WebSocket 广播推送完成, 会话数={}", delivered)
        return
      }
      val receiverId = event.receiverId ?: return
      val receiverType = NotificationReceiverType.valueOf(event.receiverType)
      val delivered = hub.push(receiverType, receiverId, payloadJson)
      if (delivered > 0) {
        logger.debug("WebSocket 定向推送完成: {}:{}", receiverType, receiverId)
      }
    } catch (e: Exception) {
      logger.error("WebSocket 推送事件处理失败", e)
    }
  }
}
