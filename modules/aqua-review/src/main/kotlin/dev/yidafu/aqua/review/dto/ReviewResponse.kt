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
import java.time.LocalDateTime

/**
 * 评价响应DTO
 */
data class ReviewResponse(
  @field:JsonProperty("reviewId")
  val reviewId: Long,
  @field:JsonProperty("orderId")
  val orderId: Long,
  @field:JsonProperty("userId")
  val userId: Long? = null, // 匿名评价时隐藏
  @field:JsonProperty("deliveryWorkerId")
  val deliveryWorkerId: Long,
  @field:JsonProperty("deliveryWorkerName")
  val deliveryWorkerName: String? = null,
  @field:JsonProperty("rating")
  val rating: Int,
  @field:JsonProperty("comment")
  val comment: String? = null,
  @field:JsonProperty("isAnonymous")
  val isAnonymous: Boolean,
  @field:JsonProperty("createdAt")
  val createdAt: LocalDateTime,
)
