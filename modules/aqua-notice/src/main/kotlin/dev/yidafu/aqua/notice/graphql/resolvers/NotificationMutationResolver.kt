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

package dev.yidafu.aqua.notice.graphql.resolvers

import dev.yidafu.aqua.notice.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.notice.service.SubscriptionService
import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

@Controller
class NotificationMutationResolver(
  private val subscriptionService: SubscriptionService,
  private val weChatMessagePushService: WeChatMessagePushService,
) {
  @MutationMapping
  @PreAuthorize("hasRole('USER')")
  fun updateNotificationSettings(
    @Argument orderUpdates: Boolean?,
    @Argument paymentNotifications: Boolean?,
    @Argument deliveryNotifications: Boolean?,
    @Argument promotionalNotifications: Boolean?,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
    val userId = userDetails.username.toLong()
    return subscriptionService.updateNotificationSettings(
      userId = userId,
      orderUpdates = orderUpdates,
      paymentNotifications = paymentNotifications,
      deliveryNotifications = deliveryNotifications,
      promotionalNotifications = promotionalNotifications,
    )
  }

  @MutationMapping
  @PreAuthorize("hasRole('USER')")
  fun enableAllNotifications(
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
    val userId = userDetails.username.toLong()
    return subscriptionService.enableAllNotifications(userId)
  }

  @MutationMapping
  @PreAuthorize("hasRole('USER')")
  fun disableAllNotifications(
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
    val userId = userDetails.username.toLong()
    return subscriptionService.disableAllNotifications(userId)
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun retryFailedMessages(): Int {
    return weChatMessagePushService.retryFailedMessages()
  }

  @MutationMapping
  @PreAuthorize("hasRole('USER')")
  fun testNotification(
    @Argument messageType: String,
    @Argument testData: Map<String, String>?,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Boolean {
    val userId = userDetails.username.toLong()

    // This is a test function to send a notification message
    // In production, you would need to get the user's actual WeChat openId
    val messageTypeEnum = dev.yidafu.aqua.notice.domain.model.MessageType.fromString(messageType)

    // For testing purposes, we'll use a mock openId
    val testOpenId = "test_open_id_$userId"

    val templateData =
      testData ?: mapOf(
        "thing1" to "测试通知",
        "thing2" to "这是一条测试消息",
        "time3" to System.currentTimeMillis().toString(),
      )

    try {
      val future =
        weChatMessagePushService.sendMessage(
          userId = userId,
          openId = testOpenId,
          messageType = messageTypeEnum,
          templateData = templateData,
        )

      return future.get()
    } catch (e: Exception) {
      return false
    }
  }
}
