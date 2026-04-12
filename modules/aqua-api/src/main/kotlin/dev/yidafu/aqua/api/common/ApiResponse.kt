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

package dev.yidafu.aqua.api.common

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 分页响应格式
 */
data class PagedResponse<T>(
  @field:JsonProperty("content")
  val content: List<T>,
  @field:JsonProperty("page")
  val page: Int,
  @field:JsonProperty("size")
  val size: Int,
  @field:JsonProperty("totalElements")
  val totalElements: Long,
  @field:JsonProperty("totalPages")
  val totalPages: Int,
  @field:JsonProperty("first")
  val first: Boolean,
  @field:JsonProperty("last")
  val last: Boolean,
)
