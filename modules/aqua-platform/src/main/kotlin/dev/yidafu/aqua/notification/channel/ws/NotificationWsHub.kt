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
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * WebSocket 推送会话注册表(PRD 13-notification.md F-NTF-003)
 *
 * 按 (receiverType, receiverId) 注册在线会话;服务端经 broadcast-events 主题
 * 收到推送事件后,由本注册表推送到对应接收人的会话。同接收人多端在线全部推送。
 */
@Component
class NotificationWsHub {
  private val logger = LoggerFactory.getLogger(NotificationWsHub::class.java)

  /** key = "USER:123" / "WORKER:9" */
  private val sessions: MutableMap<String, CopyOnWriteArraySet<WebSocketSession>> = ConcurrentHashMap()

  fun register(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    session: WebSocketSession,
  ) {
    sessions
      .computeIfAbsent(key(receiverType, receiverId)) { CopyOnWriteArraySet() }
      .add(session)
    logger.info("WebSocket 会话注册: {}:{} sessions={}", receiverType, receiverId, sessions[key(receiverType, receiverId)]?.size)
  }

  fun unregister(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    session: WebSocketSession,
  ) {
    val set = sessions[key(receiverType, receiverId)] ?: return
    set.remove(session)
    if (set.isEmpty()) {
      sessions.remove(key(receiverType, receiverId))
    }
  }

  /** 定向推送(该接收人所有在线会话) */
  fun push(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    payloadJson: String,
  ): Int {
    val set = sessions[key(receiverType, receiverId)] ?: return 0
    var delivered = 0
    set.forEach { session ->
      if (session.isOpen) {
        try {
          session.sendMessage(TextMessage(payloadJson))
          delivered++
        } catch (e: Exception) {
          logger.warn("WebSocket 推送失败: {}:{}", receiverType, receiverId, e)
        }
      }
    }
    return delivered
  }

  /** 广播推送(全部在线送水工,如新订单可抢单) */
  fun broadcastToWorkers(payloadJson: String): Int {
    var delivered = 0
    sessions.entries.forEach { (k, set) ->
      if (k.startsWith("WORKER:")) {
        set.forEach { session ->
          if (session.isOpen) {
            try {
              session.sendMessage(TextMessage(payloadJson))
              delivered++
            } catch (e: Exception) {
              logger.warn("WebSocket 广播失败: {}", k, e)
            }
          }
        }
      }
    }
    return delivered
  }

  /** 当前在线会话数(监控/测试) */
  fun onlineCount(): Int = sessions.values.sumOf { it.size }

  private fun key(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): String = "$receiverType:$receiverId"
}
