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

package dev.yidafu.aqua.admin.notification.resolvers

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.api.service.notification.NotificationQueryApiService
import dev.yidafu.aqua.common.graphql.generated.NotificationReceiverType
import dev.yidafu.aqua.common.graphql.generated.NotificationType
import dev.yidafu.aqua.common.graphql.generated.NotificationVo
import dev.yidafu.aqua.common.graphql.generated.NotificationVoPage
import dev.yidafu.aqua.notification.mapper.NotificationVoMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 后台消息列表 Resolver(PRD 13-notification.md F-NTF-006)
 * 按类型/接收人/关键字/已读状态/时间范围组合筛选,默认近 30 天由页面层给定;
 * 仅 PERMISSION_NOTIFICATION_READ(管理员/超管);手动重发失败消息需 PERMISSION_NOTIFICATION_WRITE(仅超管)。
 */
@Controller
class AdminNotificationResolver(
  private val notificationQueryApiService: NotificationQueryApiService,
  private val notificationMutationApiService: NotificationMutationApiService,
) {
  /**
   * 后台消息列表(F-NTF-006)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_NOTIFICATION_READ')")
  fun notifications(
    @Argument messageType: NotificationType?,
    @Argument receiverType: NotificationReceiverType?,
    @Argument keyword: String?,
    @Argument read: Boolean?,
    @Argument startAt: LocalDateTime?,
    @Argument endAt: LocalDateTime?,
    @Argument page: Int,
    @Argument size: Int,
  ): NotificationVoPage {
    val request =
      NotificationListRequest(
        messageType = NotificationVoMapper.toDomainMessageType(messageType),
        receiverType = NotificationVoMapper.toDomainReceiverType(receiverType),
        keyword = keyword,
        read = read,
        startAt = startAt,
        endAt = endAt,
      )
    val result = notificationQueryApiService.findNotifications(request, page, size)
    return NotificationVoPage(
      content = result.content.map { NotificationVoMapper.toVo(it) },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 后台消息详情(F-NTF-006)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_NOTIFICATION_READ')")
  fun notification(
    @Argument id: Long,
  ): NotificationVo? = notificationQueryApiService.findById(id)?.let { NotificationVoMapper.toVo(it) }

  /**
   * 手动重发失败/待发送的微信订阅消息(F-NTF-006)
   * 仅超管(PERMISSION_NOTIFICATION_WRITE,推送与消息配置);不可重发返回 null。
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_NOTIFICATION_WRITE')")
  fun resendNotification(
    @Argument id: Long,
  ): NotificationVo? = notificationMutationApiService.resendNotification(id)?.let { NotificationVoMapper.toVo(it) }
}
