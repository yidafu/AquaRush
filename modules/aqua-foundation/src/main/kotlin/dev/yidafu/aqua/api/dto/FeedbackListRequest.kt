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

import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

/**
 * 意见反馈列表查询请求
 * 与 PRD 12-operations-config.md §F-OPS-003 保持一致:类型/状态/联系方式关键字/时间范围筛选;
 * userId 用于用户详情页「意见反馈」Tab 按用户过滤。
 */
data class FeedbackListRequest(
  val feedbackType: FeedbackType? = null,
  val status: FeedbackStatus? = null,
  val userId: Long? = null,
  val contactKeyword: String? = null,
  val dateFrom: LocalDateTime? = null,
  val dateTo: LocalDateTime? = null,
  @field:Min(value = 0, message = "页码不能小于0")
  val page: Int = 0,
  @field:Min(value = 1, message = "每页条数必须大于0")
  val size: Int = 20,
)
