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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import java.time.LocalDateTime

/**
 * 平台公告实体
 * 与 PRD 12-operations-config.md §F-OPS-002 / 13-misc.md §核心实体 保持一致:
 * 标题、内容、类型、置顶、时间窗、状态(草稿/已发布/已停用);时间窗为空=长期。
 */
open class NoticeModel(
  var id: Long? = null,
  var title: String = "",
  var content: String = "",
  var type: NoticeType = NoticeType.PLATFORM_NOTICE,
  var pinned: Boolean = false,
  var startAt: LocalDateTime? = null,
  var endAt: LocalDateTime? = null,
  var status: NoticeStatus = NoticeStatus.DRAFT,
  var publishedAt: LocalDateTime? = null,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable
