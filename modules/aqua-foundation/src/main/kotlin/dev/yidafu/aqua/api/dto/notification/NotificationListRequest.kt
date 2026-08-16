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

package dev.yidafu.aqua.api.dto.notification

import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import java.time.LocalDateTime

/**
 * 后台消息列表筛选条件(F-NTF-006,PRD 13-notification.md)
 * 关键字匹配 关联单号/标题/摘要;时间范围默认近 30 天由页面层给定。
 */
data class NotificationListRequest(
  val messageType: NotificationMessageType? = null,
  val receiverType: NotificationReceiverType? = null,
  val keyword: String? = null,
  val read: Boolean? = null,
  val startAt: LocalDateTime? = null,
  val endAt: LocalDateTime? = null,
)
