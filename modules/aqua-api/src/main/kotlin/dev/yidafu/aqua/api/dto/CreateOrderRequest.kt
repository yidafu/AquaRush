/*
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

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import java.util.*

/**
 * 创建订单请求DTO
 */
data class CreateOrderRequest(
  @field:JsonProperty("orderNumber")
  val orderNumber: String,
  @field:JsonProperty("userId")
  @field:NotNull(message = "用户ID不能为空")
  val userId: Long,
  @field:JsonProperty("productId")
  @field:NotNull(message = "产品ID不能为空")
  val productId: Long,
  @field:JsonProperty("quantity")
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  val quantity: Int,
  @field:JsonProperty("addressId")
  @field:NotNull(message = "配送地址ID不能为空")
  val addressId: Long,
  @field:JsonProperty("amount")
  val amount: Long,
  @field:JsonProperty("notes")
  @field:Size(max = 500, message = "备注长度不能超过500个字符")
  val notes: String? = null,
)
