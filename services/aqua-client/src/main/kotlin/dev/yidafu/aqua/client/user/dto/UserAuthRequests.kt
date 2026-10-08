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

package dev.yidafu.aqua.client.user.dto

/**
 * Request DTO for WeChat login
 */
data class WeChatLoginRequest(
  val code: String,
)

/**
 * Request DTO for refreshing token
 */
data class RefreshTokenRequest(
  val refreshToken: String,
)

/**
 * Request DTO for user login
 */
data class LoginRequest(
  val username: String,
  val password: String,
)

/**
 * Request DTO for updating user profile
 */
data class UpdateProfileRequest(
  val nickname: String? = null,
  val phone: String? = null,
  val avatar: String? = null,
)
