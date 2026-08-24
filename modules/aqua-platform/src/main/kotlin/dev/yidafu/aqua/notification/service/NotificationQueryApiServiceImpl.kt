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

package dev.yidafu.aqua.notification.service

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.api.dto.notification.SubscribeTemplateDTO
import dev.yidafu.aqua.api.service.notification.NotificationQueryApiService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * 通知查询服务实现(PRD 13-notification.md F-NTF-002/004/006)
 * 本人消息隔离:receiverType + receiverId 由登录主体注入;按创建时间倒序分页。
 */
@Service
class NotificationQueryApiServiceImpl(
  private val notificationRepository: NotificationRepository,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) : NotificationQueryApiService {
  override fun findMyNotifications(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    messageType: NotificationMessageType?,
    read: Boolean?,
    page: Int,
    size: Int,
  ): Page<NotificationModel> {
    val pageable =
      PageRequest.of(
        page.coerceAtLeast(0),
        size.coerceIn(1, 50),
        Sort.by(Sort.Direction.DESC, "createdAt"),
      )
    return notificationRepository.findMyNotifications(receiverType, receiverId, messageType, read, pageable)
  }

  override fun countUnread(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Long = notificationRepository.countUnread(receiverType, receiverId)

  override fun findEnabledSubscribeTemplates(): List<SubscribeTemplateDTO> =
    SUBSCRIBE_SCENES
      .map { scene ->
        SubscribeTemplateDTO(
          scene = scene,
          templateId = systemConfigQueryApiService.getStringValue("notification_template_${scene}_id", null) ?: "",
          enabled = systemConfigQueryApiService.getBooleanValue("notification_template_${scene}_enabled", false),
        )
      }.filter { it.templateId.isNotBlank() && it.enabled }

  override fun findNotifications(
    request: NotificationListRequest,
    page: Int,
    size: Int,
  ): Page<NotificationModel> {
    val pageable =
      PageRequest.of(
        page.coerceAtLeast(0),
        size.coerceIn(1, 50),
        Sort.by(Sort.Direction.DESC, "createdAt"),
      )
    return notificationRepository.findByFilters(request, pageable)
  }

  override fun findById(id: Long): NotificationModel? = notificationRepository.findById(id).orElse(null)

  companion object {
    /** 微信订阅消息场景(与 SystemSettingKeys.NOTIFICATION_TEMPLATE_* 键后缀一致,F-NTF-001 五节点) */
    private val SUBSCRIBE_SCENES =
      listOf(
        "order_created",
        "delivery_assigned",
        "delivery_started",
        "delivery_completed",
        "order_urging",
        "payment_result",
      )
  }
}
