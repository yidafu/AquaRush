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

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.api.service.notification.NotificationQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.NotificationType
import dev.yidafu.aqua.common.graphql.generated.NotificationVoPage
import dev.yidafu.aqua.notification.mapper.NotificationVoMapper
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

/**
 * 送水工端消息中心 Resolver(PRD 13-notification.md F-NTF-004)
 * 仅本人消息(workerId = 登录账号 id,送水员档案并入 admins 单表,v1.2.1);
 * 送水工与「管理员兼送水」均有 PERMISSION_DELIVERY_TASK。
 */
@Controller
class WorkerNotificationResolver(
  private val notificationQueryApiService: NotificationQueryApiService,
  private val notificationMutationApiService: NotificationMutationApiService,
  private val adminService: AdminService,
) {
  /**
   * 送水工端消息中心:分页查询本人消息(仅本人)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_TASK')")
  fun myWorkerNotifications(
    @Argument type: NotificationType?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal userDetail: UserDetails,
  ): NotificationVoPage {
    val workerId = resolveWorkerId(userDetail)
    val result =
      notificationQueryApiService.findMyNotifications(
        receiverType = NotificationReceiverType.WORKER,
        receiverId = workerId,
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
   * 送水工端未读消息数(任务页/我的页入口角标)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_TASK')")
  fun workerUnreadNotificationCount(@AuthenticationPrincipal userDetail: UserDetails): Long {
    val workerId = resolveWorkerId(userDetail)
    return notificationQueryApiService.countUnread(
      receiverType = NotificationReceiverType.WORKER,
      receiverId = workerId,
    )
  }

  /**
   * 单条消息标已读(幂等,仅本人消息)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_TASK')")
  fun markWorkerNotificationRead(
    @Argument id: Long,
    @AuthenticationPrincipal userDetail: UserDetails,
  ): Boolean {
    val workerId = resolveWorkerId(userDetail)
    return notificationMutationApiService.markRead(
      receiverType = NotificationReceiverType.WORKER,
      receiverId = workerId,
      notificationId = id,
    )
  }

  /**
   * 全部已读
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_DELIVERY_TASK')")
  fun markAllWorkerNotificationsRead(@AuthenticationPrincipal userDetail: UserDetails): Boolean {
    val workerId = resolveWorkerId(userDetail)
    notificationMutationApiService.markAllRead(
      receiverType = NotificationReceiverType.WORKER,
      receiverId = workerId,
    )
    return true
  }

  private fun resolveWorkerId(userDetail: UserDetails): Long =
    adminService.findByUsername(userDetail.username)?.id
      ?: throw UserNotFoundException("送水工 ${userDetail.username} 不存在")
}
