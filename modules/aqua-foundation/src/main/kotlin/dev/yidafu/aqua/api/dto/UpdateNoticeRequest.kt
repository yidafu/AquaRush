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

package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 编辑公告请求(全量更新)
 * 与 PRD 12-operations-config.md §F-OPS-002 保持一致:标题≤50字、内容≤2000字、时间窗开始<结束(空=长期)。
 */
data class UpdateNoticeRequest(
  @field:NotBlank(message = "公告标题不能为空")
  @field:Size(max = 50, message = "公告标题不能超过50个字")
  val title: String,
  @field:NotBlank(message = "公告内容不能为空")
  @field:Size(max = 2000, message = "公告内容不能超过2000个字")
  val content: String,
  @field:NotNull(message = "公告类型不能为空")
  val type: NoticeType,
  val pinned: Boolean = false,
  val startAt: LocalDateTime? = null,
  val endAt: LocalDateTime? = null,
)
