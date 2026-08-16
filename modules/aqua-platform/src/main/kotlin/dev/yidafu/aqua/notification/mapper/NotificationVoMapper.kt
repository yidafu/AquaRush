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

package dev.yidafu.aqua.notification.mapper

import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.graphql.generated.NotificationReceiverType as GraphQlNotificationReceiverType
import dev.yidafu.aqua.common.graphql.generated.NotificationType
import dev.yidafu.aqua.common.graphql.generated.NotificationVo

/**
 * 站内消息 GraphQL VO 映射(PRD 13-notification.md F-NTF-002/004/006)
 * 用户端与后台共用;本人消息隔离由调用方 resolver 保证。
 */
object NotificationVoMapper {
  fun toVo(model: NotificationModel): NotificationVo =
    NotificationVo(
      id = model.id ?: 0L,
      type = NotificationType.valueOfLabel(model.messageType.name) ?: NotificationType.SYSTEM,
      receiverType =
        if (model.receiverType == NotificationReceiverType.USER) {
          GraphQlNotificationReceiverType.USER
        } else {
          GraphQlNotificationReceiverType.WORKER
        },
      scene = model.scene,
      title = model.title,
      summary = model.summary,
      relatedNo = model.relatedNo,
      read = model.readAt != null,
      readAt = model.readAt,
      createdAt = model.createdAt,
    )

  /** GraphQL 消息分类 → 领域枚举(为空 = 全部) */
  fun toDomainMessageType(type: NotificationType?): NotificationMessageType? =
    when (type) {
      NotificationType.ORDER -> NotificationMessageType.ORDER
      NotificationType.SYSTEM -> NotificationMessageType.SYSTEM
      NotificationType.ANNOUNCEMENT -> NotificationMessageType.ANNOUNCEMENT
      NotificationType.FEEDBACK -> NotificationMessageType.FEEDBACK
      null -> null
    }

  /** GraphQL 接收人类型 → 领域枚举(为空 = 全部) */
  fun toDomainReceiverType(receiverType: GraphQlNotificationReceiverType?): NotificationReceiverType? =
    when (receiverType) {
      GraphQlNotificationReceiverType.USER -> NotificationReceiverType.USER
      GraphQlNotificationReceiverType.WORKER -> NotificationReceiverType.WORKER
      null -> null
    }
}
