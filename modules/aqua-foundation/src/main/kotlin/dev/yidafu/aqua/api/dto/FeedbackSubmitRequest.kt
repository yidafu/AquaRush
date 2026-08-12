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

import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 反馈提交请求(用户端)
 * 与 PRD 07-user.md §F-USR-005 保持一致:类型必选、内容10~500字、图片≤3张、联系方式可选。
 */
data class FeedbackSubmitRequest(
  @field:NotNull(message = "反馈类型不能为空")
  val feedbackType: FeedbackType,
  @field:NotBlank(message = "反馈内容不能为空")
  @field:Size(min = 10, max = 500, message = "反馈内容长度需在10~500字之间")
  val content: String,
  @field:Size(max = 3, message = "最多上传3张图片")
  val images: List<String> = emptyList(),
  @field:Size(max = 100, message = "联系方式长度不能超过100个字符")
  val contactInfo: String? = null,
)
