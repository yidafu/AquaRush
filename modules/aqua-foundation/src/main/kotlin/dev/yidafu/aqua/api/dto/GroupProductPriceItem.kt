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

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 分组商品专属价条目(PRD 10-marketing.md §F-MKT-002):
 * priceCents 为空=不设置(回退目录价);金额以分计(R-MON-01)。
 */
data class GroupProductPriceItem(
  @field:NotNull(message = "商品ID不能为空")
  val productId: Long,
  @field:Min(value = 1, message = "分组价格无效")
  @field:Max(value = 9999900, message = "分组价格无效(0.01-99999元)")
  val priceCents: Long?,
)
