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

package dev.yidafu.aqua.notice.service

import dev.yidafu.aqua.notice.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.notice.domain.repository.UserNotificationSettingsRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SubscriptionService(
  private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
) {
  private val logger = LoggerFactory.getLogger(SubscriptionService::class.java)

  @Cacheable(value = ["user_notification_settings"], key = "#userId")
  fun getUserNotificationSettings(userId: Long): UserNotificationSettingsModel {
    logger.debug("Getting notification settings for user $userId")

    return userNotificationSettingsRepository.findByUserId(userId)
      .orElseGet {
        logger.info("Creating default notification settings for user $userId")
        createDefaultSettings(userId)
      }
  }

  @CacheEvict(value = ["user_notification_settings"], key = "#userId")
  fun updateNotificationSettings(
    userId: Long,
    orderUpdates: Boolean? = null,
    paymentNotifications: Boolean? = null,
    deliveryNotifications: Boolean? = null,
    promotionalNotifications: Boolean? = null,
  ): UserNotificationSettingsModel {
    logger.info("Updating notification settings for user $userId")

    val settings =
      userNotificationSettingsRepository.findByUserId(userId)
        .orElseGet { createDefaultSettings(userId) }

    val updatedSettings =
      settings.copy(
        orderUpdates = orderUpdates ?: settings.orderUpdates,
        paymentNotifications = paymentNotifications ?: settings.paymentNotifications,
        deliveryNotifications = deliveryNotifications ?: settings.deliveryNotifications,
        promotionalNotifications = promotionalNotifications ?: settings.promotionalNotifications,
      )

    val savedSettings = userNotificationSettingsRepository.save(updatedSettings)
    logger.info("Updated notification settings for user $userId")
    return savedSettings
  }

  fun isNotificationEnabled(
    userId: Long,
    messageType: dev.yidafu.aqua.notice.domain.model.MessageType,
  ): Boolean {
    logger.debug("Checking if notification is enabled for user $userId, type: $messageType")

    val settings = getUserNotificationSettings(userId)

    return when (messageType) {
      dev.yidafu.aqua.notice.domain.model.MessageType.ORDER_CREATED,
      dev.yidafu.aqua.notice.domain.model.MessageType.ORDER_PAID,
      dev.yidafu.aqua.notice.domain.model.MessageType.ORDER_CANCELLED,
      dev.yidafu.aqua.notice.domain.model.MessageType.ORDER_DELIVERED,
      -> settings.orderUpdates

      dev.yidafu.aqua.notice.domain.model.MessageType.PAYMENT_FAILED -> settings.paymentNotifications

      dev.yidafu.aqua.notice.domain.model.MessageType.DELIVERY_ASSIGNED,
      dev.yidafu.aqua.notice.domain.model.MessageType.DELIVERY_STARTED,
      dev.yidafu.aqua.notice.domain.model.MessageType.DELIVERY_DELAYED,
      -> settings.deliveryNotifications

      dev.yidafu.aqua.notice.domain.model.MessageType.PROMOTIONAL -> settings.promotionalNotifications
    }
  }

  fun enableAllNotifications(userId: Long): UserNotificationSettingsModel {
    logger.info("Enabling all notifications for user $userId")
    return updateNotificationSettings(
      userId = userId,
      orderUpdates = true,
      paymentNotifications = true,
      deliveryNotifications = true,
      promotionalNotifications = false,
    )
  }

  fun disableAllNotifications(userId: Long): UserNotificationSettingsModel {
    logger.info("Disabling all notifications for user $userId")
    return updateNotificationSettings(
      userId = userId,
      orderUpdates = false,
      paymentNotifications = false,
      deliveryNotifications = false,
      promotionalNotifications = false,
    )
  }

  fun deleteUserNotificationSettings(userId: Long): Boolean {
    logger.info("Deleting notification settings for user $userId")
    return userNotificationSettingsRepository.deleteByUserId(userId) > 0
  }

  private fun createDefaultSettings(userId: Long): UserNotificationSettingsModel {
    val defaultSettings =
      UserNotificationSettingsModel(
        userId = userId,
        orderUpdates = true,
        paymentNotifications = true,
        deliveryNotifications = true,
        promotionalNotifications = false,
      )

    return userNotificationSettingsRepository.save(defaultSettings)
  }
}
