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

package dev.yidafu.aqua.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * 创建评价请求DTO
 */
data class CreateReviewRequest(
  @field:JsonProperty("orderId")
  @field:NotNull(message = "订单ID不能为空")
  val orderId: Long,
  @field:JsonProperty("deliveryWorkerId")
  @field:NotNull(message = "配送员ID不能为空")
  val deliveryWorkerId: Long,
  @field:JsonProperty("rating")
  @field:NotNull(message = "评分不能为空")
  @field:Min(value = 1, message = "评分不能低于1星")
  @field:Max(value = 5, message = "评分不能高于5星")
  val rating: Int,
  @field:JsonProperty("comment")
  @field:Size(max = 500, message = "评论长度不能超过500个字符")
  val comment: String? = null,
  @field:JsonProperty("isAnonymous")
  val isAnonymous: Boolean = false,
)
