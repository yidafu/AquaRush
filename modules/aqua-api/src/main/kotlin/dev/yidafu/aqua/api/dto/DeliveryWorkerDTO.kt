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
import dev.yidafu.aqua.common.domain.model.WorkerStatus
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 配送员数据传输对象
 */
data class DeliveryWorkerDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "配送员ID不能为空")
  val id: Long?,
  @field:JsonProperty("userId")
  @field:NotNull(message = "用户ID不能为空")
  val userId: Long,
  @field:JsonProperty("name")
  @field:NotBlank(message = "姓名不能为空")
  val name: String,
  @field:JsonProperty("phone")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String,
  @field:JsonProperty("avatarUrl")
  val avatarUrl: String?,
  @field:JsonProperty("status")
  @field:NotNull(message = "状态不能为空")
  val status: WorkerStatus,
  //    @field:JsonProperty("coordinates")
  @field:JsonProperty("currentLocation")
  val currentLocation: Coordinates? = null,
  @field:JsonProperty("rating")
  @field:DecimalMin(value = "0.0", message = "评分必须大于等于0.0")
  @field:DecimalMax(value = "5.0", message = "评分必须小于等于5.0")
  val rating: BigDecimal? = null,
  @field:JsonProperty("totalOrders")
  @field:NotNull(message = "总订单数不能为空")
  val totalOrders: Int = 0,
  @field:JsonProperty("completedOrders")
  @field:NotNull(message = "已完成订单数不能为空")
  val completedOrders: Int = 0,
  @field:JsonProperty("averageRating")
  @field:DecimalMin(value = "0.0", message = "平均评分必须大于等于0.0")
  @field:DecimalMax(value = "5.0", message = "平均评分必须小于等于5.0")
  val averageRating: BigDecimal? = null,
  @field:JsonProperty("earning")
  @field:DecimalMin(value = "0.0", message = "收入必须大于等于0.0")
  val earning: BigDecimal? = null,
  @field:JsonProperty("isAvailable")
  @field:NotNull(message = "可用状态不能为空")
  val isAvailable: Boolean = true,
  @field:JsonProperty("createdAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val createdAt: LocalDateTime,
  @field:JsonProperty("updatedAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val updatedAt: LocalDateTime,
)
