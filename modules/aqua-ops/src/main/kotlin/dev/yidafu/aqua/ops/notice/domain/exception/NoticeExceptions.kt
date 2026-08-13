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

package dev.yidafu.aqua.ops.notice.domain.exception

import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus

/**
 * 公告未找到异常
 */
class NoticeNotFoundException(
  noticeId: Long,
) : RuntimeException("Notice not found with ID: $noticeId")

/**
 * 公告状态流转非法异常
 * 状态机(PRD 12-operations-config.md §F-OPS-002):草稿 → 已发布 → 已停用;
 * 已停用公告不可再编辑/发布/置顶(PRD 开放问题,采用最简规则)。
 */
class InvalidNoticeStateTransitionException(
  noticeId: Long,
  from: NoticeStatus,
  to: String,
) : RuntimeException("Notice $noticeId cannot transition from $from to $to")

/**
 * 时间窗非法异常(开始时间必须早于结束时间)
 */
class InvalidNoticeTimeWindowException(
  message: String,
) : RuntimeException(message)
