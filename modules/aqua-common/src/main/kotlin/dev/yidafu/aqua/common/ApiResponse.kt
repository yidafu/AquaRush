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

package dev.yidafu.aqua.common

/**
 * Common result wrapper for API responses
 */
data class ApiResponse<T>(
  val success: Boolean,
  val data: T? = null,
  val message: String? = null,
  val code: String? = null,
) {
  companion object {
    fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

    fun <T> error(
      message: String,
      code: String? = null,
    ): ApiResponse<T> = ApiResponse(success = false, message = message, code = code)
  }
}
