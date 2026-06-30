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

package dev.yidafu.aqua.client.user.controller.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * Request DTO for JWT generation
 */
data class JwtGenerationRequest(
  @field:NotNull(message = "User ID is required")
  val userId: Long,
)

/**
 * Response DTO for JWT generation
 */
data class JwtGenerationResponse(
  val token: String,
  val userId: Long,
  val username: String,
  val userType: String,
  val nickname: String?,
  val status: String,
  val authorities: List<String>,
)

/**
 * Debug info DTO for user details
 */
data class UserDebugInfo(
  val id: Long,
  val wechatOpenId: String,
  val nickname: String?,
  val phone: String?,
  val email: String,
  val role: String,
  val status: String,
  val balance: Long,
  val totalSpent: Long,
  val createdAt: LocalDateTime,
  val lastLoginAt: LocalDateTime,
)
