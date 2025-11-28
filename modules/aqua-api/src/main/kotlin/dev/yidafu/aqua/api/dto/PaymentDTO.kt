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
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 支付数据传输对象
 */
data class PaymentDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "支付ID不能为空")
  val id: Long?,
  @field:JsonProperty("orderId")
  @field:NotNull(message = "订单ID不能为空")
  val orderId: Long,
  @field:JsonProperty("amount")
  @field:NotNull(message = "支付金额不能为空")
  @field:DecimalMin(value = "0.01", message = "支付金额必须大于等于0.01")
  val amount: BigDecimal,
  @field:JsonProperty("method")
  @field:NotNull(message = "支付方式不能为空")
  val paymentMethod: PaymentMethod,
  @field:JsonProperty("transactionId")
  val transactionId: String? = null,
  @field:JsonProperty("status")
  @field:NotNull(message = "支付状态不能为空")
  val status: PaymentStatus,
  @field:JsonProperty("paidAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val paidAt: LocalDateTime?,
)
