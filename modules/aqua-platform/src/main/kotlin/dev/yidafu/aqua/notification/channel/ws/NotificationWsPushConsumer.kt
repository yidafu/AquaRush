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
import dev.yidafu.aqua.common.messaging.outbox.OutboxDestinationHandler
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * WebSocket 推送事件 outbox 处理器(PRD 13-notification.md F-NTF-003)
 *
 * 消费 broadcast-events 行(按 subscriber 扇出,两服务各消费自己名下的行),
 * 把推送载荷分发到本服务的在线会话:定向(USER/WORKER 按接收人)或广播(全部在线送水工)。
 *
 * best-effort 语义(与原 JMS 监听行为一致):推送失败仅记日志不重试,
 * 由轮询器置 DONE —— 实时推送不值得为个别会话失败反复退避。
 */
@Component
class NotificationWsPushConsumer(
  private val hub: NotificationWsHub,
) : OutboxDestinationHandler {
  private val logger = LoggerFactory.getLogger(NotificationWsPushConsumer::class.java)
  private val objectMapper = jacksonObjectMapper()

  override val destinations: Set<String> = setOf(PushEvent.TOPIC)

  override fun handle(record: OutboxEventRecord) {
    try {
      val event = objectMapper.readValue(record.payload, PushEvent::class.java)
      val payloadJson = objectMapper.writeValueAsString(event.payload)
      if (event.broadcast) {
        val delivered = hub.broadcastToWorkers(payloadJson)
        logger.debug("WebSocket 广播推送完成, 会话数={}", delivered)
        return
      }
      val receiverId = event.receiverId ?: return
      val receiverType = NotificationReceiverType.fromValue(event.receiverType)
      val delivered = hub.push(receiverType, receiverId, payloadJson)
      if (delivered > 0) {
        logger.debug("WebSocket 定向推送完成: {}:{}", receiverType, receiverId)
      }
    } catch (e: Exception) {
      logger.error("WebSocket 推送事件处理失败: id={}", record.id, e)
    }
  }
}
