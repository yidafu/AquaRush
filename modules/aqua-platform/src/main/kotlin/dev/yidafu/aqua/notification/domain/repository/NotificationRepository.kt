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

package dev.yidafu.aqua.notification.domain.repository

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 站内消息仓库(PRD 13-notification.md,统一落库表 notifications)
 */
@Repository
interface NotificationRepository {
  fun existsByIdempotencyKey(idempotencyKey: String): Boolean

  fun findById(id: Long): Optional<NotificationModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: NotificationModel): NotificationModel

  /** 语义对齐 JPA saveAndFlush:Exposed 写路径即时提交,flush 为空操作。 */
  fun saveAndFlush(entity: NotificationModel): NotificationModel = save(entity)

  /**
   * 分页查询本人消息(仅本人;messageType/read 为空 = 全部),按创建时间倒序
   */
  fun findMyNotifications(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    messageType: NotificationMessageType?,
    read: Boolean?,
    pageable: Pageable,
  ): Page<NotificationModel>

  /**
   * 本人未读数(入口角标)
   */
  fun countUnread(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Long

  /**
   * 本人全部置已读,返回置读条数(幂等)
   */
  fun markAllRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Int

  /**
   * 后台消息列表(组合筛选,近 30 天由页面层给定时间范围)
   */
  fun findByFilters(
    request: NotificationListRequest,
    pageable: Pageable,
  ): Page<NotificationModel>

  /**
   * 清理创建时间早于 before 的消息(R-NTF-04 保留策略)
   */
  fun deleteExpired(before: LocalDateTime): Int
}
