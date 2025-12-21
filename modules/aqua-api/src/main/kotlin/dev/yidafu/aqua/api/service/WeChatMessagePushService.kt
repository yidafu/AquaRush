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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.MessageType
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * 微信消息推送服务接口
 */
interface WeChatMessagePushService {
  /**
   * 获取微信访问令牌
   */
  fun getAccessToken(): String

  /**
   * 发送微信消息
   */
  fun sendMessage(
    userId: Long,
    openId: String,
    messageType: MessageType,
    templateData: Map<String, String>,
    page: String? = null,
  ): CompletableFuture<Boolean>

  /**
   * 重试失败的消息
   */
  fun retryFailedMessages(): Int

  /**
   * 获取消息统计
   */
  fun getMessageStatistics(since: LocalDateTime): Map<String, Long>

  /**
   * 发送测试通知
   */
  fun sendTestNotification(userId: Long): Unit
}
