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

import dev.yidafu.aqua.api.service.notification.NotificationQueryApiService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.graphql.generated.NotificationType
import dev.yidafu.aqua.common.graphql.generated.NotificationVoPage
import dev.yidafu.aqua.common.graphql.generated.SubscribeTemplateVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.notification.mapper.NotificationVoMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端消息中心查询 Resolver(PRD 13-notification.md F-NTF-002)
 * 消息中心列表/未读角标/订阅模板;仅本人消息(userId 强制取登录主体,防越权 R-AUTH-02)。
 */
@ClientService
@Controller
class ClientNotificationQueryResolver(
  private val notificationQueryApiService: NotificationQueryApiService,
) {
  /**
   * 消息中心:分页查询本人消息(仅本人),type 为空 = 全部
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myNotifications(
    @Argument type: NotificationType?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): NotificationVoPage {
    val result =
      notificationQueryApiService.findMyNotifications(
        receiverType = NotificationReceiverType.USER,
        receiverId = principal.id,
        messageType = NotificationVoMapper.toDomainMessageType(type),
        page = page,
        size = size,
      )
    return NotificationVoPage(
      content = result.content.map { NotificationVoMapper.toVo(it) },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 未读消息数(我的页消息中心入口角标)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun unreadNotificationCount(
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Long =
    notificationQueryApiService.countUnread(
      receiverType = NotificationReceiverType.USER,
      receiverId = principal.id,
    )

  /**
   * 已启用的订阅消息模板(下单确认页订阅授权用)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun subscribeTemplates(): List<SubscribeTemplateVo> =
    notificationQueryApiService
      .findEnabledSubscribeTemplates()
      .map { SubscribeTemplateVo(scene = it.scene, templateId = it.templateId, enabled = it.enabled) }
}
