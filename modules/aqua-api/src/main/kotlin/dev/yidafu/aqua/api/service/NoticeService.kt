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

import dev.yidafu.aqua.common.domain.model.MessageHistoryModel
import dev.yidafu.aqua.common.domain.model.MessageType
import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * 通知服务接口
 */
interface NoticeService {
  /**
   * 保存消息历史
   */
  fun save(messageHistory: MessageHistoryModel): MessageHistoryModel

  /**
   * 根据用户ID查找消息历史
   */
  fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<MessageHistoryModel>

  /**
   * 更新消息发送成功状态
   */
  fun updateSuccess(
    messageId: Long,
    wxMessageId: String,
  )

  /**
   * 更新消息发送失败状态
   */
  fun updateFailure(
    messageId: Long,
    errorMessage: String,
  )

  /**
   * 更新消息为重试状态
   */
  fun updateToRetrying(messageId: Long)

  /**
   * 增加重试次数
   */
  fun incrementRetryCount(messageId: Long)

  /**
   * 查找需要重试的失败消息
   */
  fun findFailedMessagesForRetry(): List<MessageHistoryModel>

  /**
   * 获取用户消息统计
   */
  fun getMessageStatistics(userId: Long): Map<String, Long>

  /**
   * 获取系统消息统计
   */
  fun getStatistics(since: LocalDateTime): Map<String, Long>

  /**
   * 根据微信消息ID查找消息
   */
  fun findByWxMessageId(wxMessageId: String): MessageHistoryModel?

  /**
   * 获取未读消息数量
   */
  fun getUnreadCount(userId: Long): Int

  /**
   * 删除旧消息
   */
  fun deleteOldMessages(olderThanDays: Int): Int

  /**
   * 获取用户通知设置
   */
  fun getUserNotificationSettings(userId: Long): UserNotificationSettingsModel

  /**
   * 更新用户通知设置
   */
  fun updateNotificationSettings(
    userId: Long,
    orderUpdates: Boolean? = null,
    paymentNotifications: Boolean? = null,
    deliveryNotifications: Boolean? = null,
    promotionalNotifications: Boolean? = null,
  ): UserNotificationSettingsModel

  /**
   * 检查通知是否启用
   */
  fun isNotificationEnabled(
    userId: Long,
    messageType: MessageType,
  ): Boolean

  /**
   * 启用所有通知
   */
  fun enableAllNotifications(userId: Long): UserNotificationSettingsModel

  /**
   * 禁用所有通知
   */
  fun disableAllNotifications(userId: Long): UserNotificationSettingsModel

  /**
   * 删除用户通知设置
   */
  fun deleteUserNotificationSettings(userId: Long): Boolean

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
}
