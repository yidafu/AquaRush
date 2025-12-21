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

package dev.yidafu.aqua.notice.service.impl

import dev.yidafu.aqua.api.service.SubscriptionService
import dev.yidafu.aqua.common.domain.model.MessageType
import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.notice.domain.repository.UserNotificationSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 订阅服务实现
 */
@Service
class SubscriptionServiceImpl(
  private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
) : SubscriptionService {
  /**
   * 获取用户通知设置
   */
  override fun getUserNotificationSettings(userId: Long): UserNotificationSettingsModel {
    return userNotificationSettingsRepository.findByUserId(userId)
      .orElseGet {
        // 如果用户没有通知设置，则创建默认设置
        val defaultSettings =
          UserNotificationSettingsModel(
            userId = userId,
            orderUpdates = true,
            paymentNotifications = true,
            deliveryNotifications = true,
            promotionalNotifications = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
          )
        userNotificationSettingsRepository.save(defaultSettings)
      }
  }

  /**
   * 更新用户通知设置
   */
  @Transactional
  override fun updateNotificationSettings(
    userId: Long,
    orderUpdates: Boolean?,
    paymentNotifications: Boolean?,
    deliveryNotifications: Boolean?,
    promotionalNotifications: Boolean?,
  ): UserNotificationSettingsModel {
    val settings = getUserNotificationSettings(userId)

    orderUpdates?.let { settings.orderUpdates = it }
    paymentNotifications?.let { settings.paymentNotifications = it }
    deliveryNotifications?.let { settings.deliveryNotifications = it }
    promotionalNotifications?.let { settings.promotionalNotifications = it }

    settings.updatedAt = LocalDateTime.now()

    return userNotificationSettingsRepository.save(settings)
  }

  /**
   * 检查通知是否启用
   */
  override fun isNotificationEnabled(
    userId: Long,
    messageType: MessageType,
  ): Boolean {
    val settings = getUserNotificationSettings(userId)

    return when (messageType) {
      MessageType.ORDER_UPDATE -> settings.orderUpdates
      MessageType.PAYMENT_SUCCESS, MessageType.PAYMENT_FAILURE -> settings.paymentNotifications
      MessageType.DELIVERY_UPDATE -> settings.deliveryNotifications
      MessageType.SYSTEM_NOTICE -> true // 系统公告总是启用
      MessageType.PROMOTIONAL -> settings.promotionalNotifications
      else -> true // 默认启用所有通知
    }
  }

  /**
   * 启用所有通知
   */
  @Transactional
  override fun enableAllNotifications(userId: Long): UserNotificationSettingsModel {
    return updateNotificationSettings(
      userId = userId,
      orderUpdates = true,
      paymentNotifications = true,
      deliveryNotifications = true,
      promotionalNotifications = true,
    )
  }

  /**
   * 禁用所有通知
   */
  @Transactional
  override fun disableAllNotifications(userId: Long): UserNotificationSettingsModel {
    return updateNotificationSettings(
      userId = userId,
      orderUpdates = false,
      paymentNotifications = false,
      deliveryNotifications = false,
      promotionalNotifications = false,
    )
  }

  /**
   * 删除用户通知设置
   */
  @Transactional
  override fun deleteUserNotificationSettings(userId: Long): Boolean {
    val deletedCount = userNotificationSettingsRepository.deleteByUserId(userId)
    return deletedCount > 0
  }
}
