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

package dev.yidafu.aqua.client.notification.resolver

import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端消息中心写操作 Resolver(PRD 13-notification.md R-NTF-01/04)
 * 已读标记幂等;仅本人消息(userId 强制取登录主体,防越权 R-AUTH-02)。
 */
@ClientService
@Controller
class ClientNotificationMutationResolver(
  private val notificationMutationApiService: NotificationMutationApiService,
) {
  /**
   * 单条消息标已读
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun markNotificationRead(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean =
    notificationMutationApiService.markRead(
      receiverType = NotificationReceiverType.USER,
      receiverId = principal.id,
      notificationId = id,
    )

  /**
   * 全部已读
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun markAllNotificationsRead(@AuthenticationPrincipal principal: UserPrincipal): Boolean {
    notificationMutationApiService.markAllRead(
      receiverType = NotificationReceiverType.USER,
      receiverId = principal.id,
    )
    return true
  }

  /**
   * 记录微信订阅消息授权结果(仅记录用户实际接受的模板,一次性模板授权一次可发一次)
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun recordSubscribeAuth(
    @Argument templateIds: List<String>,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    notificationMutationApiService.recordSubscribeAuth(
      userId = principal.id,
      templateIds = templateIds,
    )
    return true
  }
}
