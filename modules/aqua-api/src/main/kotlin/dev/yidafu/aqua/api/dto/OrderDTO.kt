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

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * 订单数据传输对象
 */
data class OrderDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "订单ID不能为空")
  val id: Long?,
  @field:JsonProperty("orderNumber")
  @field:NotBlank(message = "订单号不能为空")
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
  @field:JsonProperty("amount")
  @field:NotNull(message = "金额不能为空")
  @field:DecimalMin(value = "0.01", message = "金额必须大于等于0.01")
  val amount: BigDecimal,
  @field:JsonProperty("status")
  @field:NotNull(message = "订单状态不能为空")
  val status: OrderStatus,
  @field:JsonProperty("addressId")
  @field:NotNull(message = "地址ID不能为空")
  val addressId: Long,
  @field:JsonProperty("deliveryAddressId")
  @field:NotNull(message = "配送地址ID不能为空")
  val deliveryAddressId: Long,
  //    @field:JsonProperty("paymentTransactionId")
  @field:JsonProperty("paymentMethod")
  val paymentMethod: PaymentMethod? = null,
  val paymentTransactionId: String? = null,
  @field:JsonProperty("deliveryWorkerId")
  val deliveryWorkerId: Long? = null,
  @field:JsonProperty("paymentTime")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val paymentTime: LocalDateTime? = null,
  @field:JsonProperty("deliveryTime")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val deliveryTime: LocalDateTime? = null,
  @field:JsonProperty("completedAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val completedAt: LocalDateTime? = null,
  @field:JsonProperty("createdAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val createdAt: LocalDateTime,
  @field:JsonProperty("updatedAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val updatedAt: LocalDateTime,
//  @field:JsonProperty("items")
//  val items: List<OrderItemDTO>,
  @field:JsonProperty("totalAmount")
  @field:NotNull(message = "总金额不能为空")
  val totalAmount: BigDecimal,
  @field:JsonProperty("notes")
  val notes: String? = null,
)
