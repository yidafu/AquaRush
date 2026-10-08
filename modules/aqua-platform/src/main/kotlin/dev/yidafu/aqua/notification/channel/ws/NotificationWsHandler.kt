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

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.security.JwtTokenService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 通知 WebSocket 处理器(PRD 13-notification.md F-NTF-003)
 *
 * 客户端连接 `/ws/notifications` 后首条消息发送 `{"token":"...","receiverType":"USER|WORKER"}`:
 * 服务端校验 JWT 并从 token 解析接收人身份(USER → openid 解析 userId;WORKER → 账号名解析 admin id),
 * 随后服务端推送实时消息到该会话。断线由客户端退避重连;离线消息以消息中心(GraphQL)补齐(R-NTF-03)。
 */
@Component
class NotificationWsHandler(
  private val jwtTokenService: JwtTokenService,
  private val userQueryApiService: UserQueryApiService,
  private val adminService: AdminService,
  private val hub: NotificationWsHub,
) : TextWebSocketHandler() {
  private val logger = LoggerFactory.getLogger(NotificationWsHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun afterConnectionEstablished(session: WebSocketSession) {
    // 等待客户端首条注册消息;连接建立即记录属性,防止首条消息前收到推送
    session.attributes["ws-registered"] = false
  }

  override fun handleTextMessage(
    session: WebSocketSession,
    message: TextMessage,
  ) {
    if (session.attributes["ws-registered"] == true) {
      // 已注册,忽略后续消息(预留心跳)
      return
    }
    try {
      val body = objectMapper.readValue(message.payload, object : TypeReference<Map<String, String>>() {})
      val token = body["token"] ?: return
      val receiverTypeValue = body["receiverType"] ?: return
      val receiverType = NotificationReceiverType.valueOf(receiverTypeValue)
      val receiverId =
        resolveReceiverId(receiverType, token) ?: run {
          logger.warn("WebSocket 鉴权失败,关闭会话: {}", receiverTypeValue)
          session.close(CloseStatus.NOT_ACCEPTABLE)
          return
        }
      session.attributes["ws-receiver-type"] = receiverType
      session.attributes["ws-receiver-id"] = receiverId
      session.attributes["ws-registered"] = true
      hub.register(receiverType, receiverId, session)
    } catch (e: Exception) {
      logger.warn("WebSocket 注册消息解析失败", e)
      try {
        session.close(CloseStatus.BAD_DATA)
      } catch (ignored: Exception) {
        // ignore
      }
    }
  }

  override fun afterConnectionClosed(
    session: WebSocketSession,
    status: CloseStatus,
  ) {
    val receiverType = session.attributes["ws-receiver-type"] as? NotificationReceiverType
    val receiverId = session.attributes["ws-receiver-id"] as? Long
    if (receiverType != null && receiverId != null) {
      hub.unregister(receiverType, receiverId, session)
    }
  }

  /** 校验 JWT 并从 token 解析接收人 id */
  private fun resolveReceiverId(
    receiverType: NotificationReceiverType,
    token: String,
  ): Long? {
    val username = jwtTokenService.extractUsername(token) ?: return null
    if (jwtTokenService.isTokenExpired(token)) {
      return null
    }
    return when (receiverType) {
      NotificationReceiverType.USER -> userQueryApiService.findByWechatOpenId(username)?.id
      NotificationReceiverType.WORKER -> adminService.findByUsername(username)?.id
    }
  }
}
