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

import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 编辑桶水票票种请求(全量覆盖;已购票按快照不受影响,票种配置变更不影响已购票)。
 */
data class TicketTypeUpdateRequest(
  @field:NotBlank(message = "票种名称不能为空")
  @field:Size(max = 100, message = "票种名称不能超过100个字")
  val name: String,
  @field:NotNull(message = "票种类型不能为空")
  val ticketType: TicketTypeKind,
  val productId: Long? = null,
  @field:Size(max = 100, message = "适用规格不能超过100个字符")
  val specification: String? = null,
  @field:NotNull(message = "套餐张数不能为空")
  @field:Min(value = 1, message = "套餐张数必须大于0")
  @field:Max(value = 1000, message = "套餐张数不能超过1000")
  val packageCount: Int,
  @field:NotNull(message = "购票价不能为空")
  @field:Min(value = 1, message = "购票价无效(0.01-99999元)")
  @field:Max(value = 9999900, message = "购票价无效(0.01-99999元)")
  val packagePriceCents: Long,
  @field:NotNull(message = "赠送张数不能为空")
  @field:Min(value = 0, message = "赠送张数无效")
  @field:Max(value = 1000, message = "赠送张数不能超过1000")
  val giftCount: Int,
  @field:NotNull(message = "有效期类型不能为空")
  val validityType: ValidityType,
  val validFrom: LocalDateTime? = null,
  val validTo: LocalDateTime? = null,
  @field:Min(value = 1, message = "有效天数无效")
  @field:Max(value = 3650, message = "有效天数不能超过3650天")
  val validDays: Int? = null,
  @field:NotNull(message = "限购数不能为空")
  @field:Min(value = 0, message = "限购数无效")
  @field:Max(value = 1000, message = "限购数不能超过1000")
  val perUserLimit: Int,
)
