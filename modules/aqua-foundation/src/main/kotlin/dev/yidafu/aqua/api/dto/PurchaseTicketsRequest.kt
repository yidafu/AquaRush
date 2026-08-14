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
 * 购票请求(PRD 10-marketing.md §F-MKT-007):在线支付,购票即预收款项,一经购买不支持退款。
 */
data class PurchaseTicketsRequest(
  @field:NotNull(message = "票种不能为空")
  val ticketTypeId: Long,
  @field:NotNull(message = "购票张数不能为空")
  @field:Min(value = 1, message = "购票张数必须大于0")
  @field:Max(value = 10, message = "单次购票张数不能超过10")
  val packageCount: Int,
)
