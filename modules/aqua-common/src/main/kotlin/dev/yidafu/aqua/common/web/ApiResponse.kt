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

package dev.yidafu.aqua.common.web

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

/**
 * 统一 API 响应格式
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
  val success: Boolean,
  val message: String,
  val data: T? = null,
  val errorCode: String? = null,
  val timestamp: LocalDateTime = LocalDateTime.now()
) {
  companion object {
    /**
     * 成功响应
     */
    fun <T> success(data: T, message: String = "操作成功"): ApiResponse<T> {
      return ApiResponse(success = true, message = message, data = data)
    }

    /**
     * 成功响应（无数据）
     */
    fun success(message: String = "操作成功"): ApiResponse<Unit> {
      return ApiResponse(success = true, message = message)
    }

    /**
     * 失败响应
     */
    fun <T> error(
      message: String,
      errorCode: String? = null,
      data: T? = null
    ): ApiResponse<T> {
      return ApiResponse(success = false, message = message, errorCode = errorCode, data = data)
    }

    /**
     * 认证失败响应
     */
    fun unauthorized(message: String = "认证失败，请先登录"): ApiResponse<Unit> {
      return ApiResponse(
        success = false,
        message = message,
        errorCode = "UNAUTHORIZED"
      )
    }

    /**
     * 权限不足响应
     */
    fun forbidden(message: String = "权限不足"): ApiResponse<Unit> {
      return ApiResponse(
        success = false,
        message = message,
        errorCode = "FORBIDDEN"
      )
    }

    /**
     * 资源未找到响应
     */
    fun notFound(message: String = "资源未找到"): ApiResponse<Unit> {
      return ApiResponse(
        success = false,
        message = message,
        errorCode = "NOT_FOUND"
      )
    }

    /**
     * 服务器错误响应
     */
    fun internalServerError(message: String = "服务器内部错误"): ApiResponse<Unit> {
      return ApiResponse(
        success = false,
        message = message,
        errorCode = "INTERNAL_SERVER_ERROR"
      )
    }
  }
}