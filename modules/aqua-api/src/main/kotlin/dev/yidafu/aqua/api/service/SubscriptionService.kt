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
import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel

/**
 * 订阅服务接口
 */
interface SubscriptionService {
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
}
