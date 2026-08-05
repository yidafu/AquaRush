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

package dev.yidafu.aqua.common

import java.time.LocalDateTime

/**
 * Common result wrapper for API responses
 */
data class ApiResponse<T>(
  val success: Boolean,
  val data: T? = null,
  val message: String? = null,
  val code: String? = null,
  val timestamp: LocalDateTime = LocalDateTime.now(),
) {
  companion object {
    fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

    fun <T> success(
      data: T,
      message: String,
    ): ApiResponse<T> = ApiResponse(success = true, data = data, message = message)

    /**
     * 失败响应
     */
    fun <T> error(
      message: String,
      code: String? = null,
      data: T? = null,
    ): ApiResponse<T> = ApiResponse(success = false, message = message, code = code, data = data)

    /**
     * 认证失败响应
     */
    fun unauthorized(message: String = "认证失败，请先登录"): ApiResponse<Unit> =
      ApiResponse(
        success = false,
        message = message,
        code = "UNAUTHORIZED",
      )

    /**
     * 权限不足响应
     */
    fun forbidden(message: String = "权限不足"): ApiResponse<Unit> =
      ApiResponse(
        success = false,
        message = message,
        code = "FORBIDDEN",
      )

    /**
     * 资源未找到响应
     */
    fun notFound(message: String = "资源未找到"): ApiResponse<Unit> =
      ApiResponse(
        success = false,
        message = message,
        code = "NOT_FOUND",
      )

    /**
     * 服务器错误响应
     */
    fun internalServerError(message: String = "服务器内部错误"): ApiResponse<Unit> =
      ApiResponse(
        success = false,
        message = message,
        code = "INTERNAL_SERVER_ERROR",
      )
  }
}
