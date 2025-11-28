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
import java.math.BigDecimal

/**
 * 订单项数据传输对象
 */
data class OrderItemDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "订单项ID不能为空")
  val id: Long,
  @field:JsonProperty("productId")
  @field:NotNull(message = "产品ID不能为空")
  val productId: Long,
  @field:JsonProperty("productName")
  @field:NotBlank(message = "产品名称不能为空")
  val productName: String,
  @field:JsonProperty("productImage")
  val productImage: String?,
  @field:JsonProperty("unitPrice")
  @field:NotNull(message = "单价不能为空")
  @field:DecimalMin(value = "0.01", message = "单价必须大于等于0.01")
  val unitPrice: BigDecimal,
  @field:JsonProperty("quantity")
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  val quantity: Int,
  @field:JsonProperty("subtotal")
  @field:NotNull(message = "小计金额不能为空")
  val subtotal: BigDecimal,
)
