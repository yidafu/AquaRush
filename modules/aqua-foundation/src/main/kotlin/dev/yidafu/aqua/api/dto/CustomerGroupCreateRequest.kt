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

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 新建客户分组请求(PRD 10-marketing.md §F-MKT-001):分组名必填、唯一;描述选填;
 * memberIds 为初始成员(用户ID列表,可选,创建后即时划入分组)。
 */
data class CustomerGroupCreateRequest(
  @field:NotBlank(message = "分组名称不能为空")
  @field:Size(max = 50, message = "分组名称不能超过50个字")
  val name: String,
  @field:Size(max = 500, message = "分组描述不能超过500个字")
  val description: String? = null,
  val memberIds: List<Long>? = null,
)
