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
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * 支付退款数据传输对象
 */
data class PaymentRefundDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "退款ID不能为空")
  val id: Long?,
  @field:JsonProperty("paymentId")
  @field:NotNull(message = "支付ID不能为空")
  val paymentId: Long,
  @field:JsonProperty("refundAmount")
  @field:NotNull(message = "退款金额不能为空")
  @field:DecimalMin(value = "0.01", message = "退款金额必须大于0")
  val refundAmount: BigDecimal,
  @field:JsonProperty("refundReason")
  @field:NotBlank(message = "退款原因不能为空")
  @field:Size(max = 500, message = "退款原因长度不能超过500个字符")
  val refundReason: String?,
  @field:JsonProperty("status")
  @field:NotNull(message = "退款状态不能为空")
  val status: RefundStatus,
  @field:JsonProperty("processedAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val processedAt: LocalDateTime? = null,
  @field:JsonProperty("createdAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val createdAt: LocalDateTime,
  @field:JsonProperty("updatedAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val updatedAt: LocalDateTime,
)

/**
 * 退款状态枚举
 */
enum class RefundStatus {
  PENDING, // 待处理
  APPROVED, // 已同意
  REJECTED, // 已拒绝
  PROCESSING, // 处理中
  COMPLETED, // 已完成
  FAILED, // 失败
}
