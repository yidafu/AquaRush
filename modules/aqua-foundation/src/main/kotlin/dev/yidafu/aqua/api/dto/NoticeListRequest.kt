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

import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import jakarta.validation.constraints.Min

/**
 * 公告列表查询请求
 */
data class NoticeListRequest(
  val keyword: String? = null,
  val type: NoticeType? = null,
  val status: NoticeStatus? = null,
  @field:Min(value = 0, message = "页码不能小于0")
  val page: Int = 0,
  @field:Min(value = 1, message = "每页条数必须大于0")
  val size: Int = 20,
)
