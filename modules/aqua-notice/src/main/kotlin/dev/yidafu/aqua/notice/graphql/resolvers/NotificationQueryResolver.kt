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

import dev.yidafu.aqua.common.dto.PageRequest
import dev.yidafu.aqua.notice.domain.model.MessageHistoryModel
import dev.yidafu.aqua.notice.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.notice.service.MessageHistoryService
import dev.yidafu.aqua.notice.service.SubscriptionService
import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import org.springframework.data.domain.Page
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Controller
class NotificationQueryResolver(
  private val subscriptionService: SubscriptionService,
  private val messageHistoryService: MessageHistoryService,
  private val weChatMessagePushService: WeChatMessagePushService,
) {
  @QueryMapping
  @PreAuthorize("hasRole('USER')")
  fun getUserNotificationSettings(
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
    val userId = userDetails.username.toLong()
    return subscriptionService.getUserNotificationSettings(userId)
  }

  @QueryMapping
  @PreAuthorize("hasRole('USER')")
  fun getMessageHistory(
    @Argument pageRequest: PageRequest?,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Page<MessageHistoryModel> {
    val userId = userDetails.username.toLong()
    val pageable =
      if (pageRequest != null) {
        SpringPageRequest.of(pageRequest.page, pageRequest.size)
      } else {
        SpringPageRequest.of(0, 20)
      }
    return messageHistoryService.findByUserId(userId, pageable)
  }

  @QueryMapping
  @PreAuthorize("hasRole('USER')")
  fun getMessageStatistics(
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Map<String, Long> {
    val userId = userDetails.username.toLong()
    return messageHistoryService.getMessageStatistics(userId)
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun getSystemMessageStatistics(
    @Argument since: LocalDateTime?,
  ): Map<String, Long> {
    val sinceDate = since ?: LocalDateTime.now().minusDays(7)
    return weChatMessagePushService.getMessageStatistics(sinceDate)
  }

  @QueryMapping
  @PreAuthorize("hasRole('USER')")
  fun isNotificationEnabled(
    @Argument messageType: String,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Boolean {
    val userId = userDetails.username.toLong()
    val messageTypeEnum = dev.yidafu.aqua.notice.domain.model.MessageType.fromString(messageType)
    return subscriptionService.isNotificationEnabled(userId, messageTypeEnum)
  }
}
