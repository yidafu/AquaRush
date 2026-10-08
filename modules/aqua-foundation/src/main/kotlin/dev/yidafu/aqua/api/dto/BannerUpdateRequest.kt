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

import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 编辑轮播图请求(全量更新)
 */
data class BannerUpdateRequest(
  @field:NotBlank(message = "图片不能为空")
  @field:Size(max = 1000, message = "图片地址长度不能超过1000个字符")
  val imageUrl: String,
  @field:NotBlank(message = "标题不能为空")
  @field:Size(max = 20, message = "标题不能超过20个字")
  val title: String,
  @field:Size(max = 100, message = "副标题不能超过100个字")
  val subtitle: String? = null,
  @field:NotNull(message = "跳转类型不能为空")
  val linkType: BannerLinkType,
  @field:NotBlank(message = "跳转目标不能为空")
  @field:Size(max = 1000, message = "跳转目标长度不能超过1000个字符")
  val linkTarget: String,
  val sortOrder: Int? = null,
  val enabled: Boolean = false,
  val startAt: LocalDateTime? = null,
  val endAt: LocalDateTime? = null,
)
