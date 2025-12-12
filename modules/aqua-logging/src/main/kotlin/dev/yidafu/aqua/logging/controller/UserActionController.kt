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

package dev.yidafu.aqua.logging.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.logging.service.UserActionEventService
import dev.yidafu.aqua.logging.util.UserActionLogger
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 用户操作日志记录控制器
 * 提供RESTful API接口用于接收前端上报的用户操作日志
 */
@RestController
@RequestMapping("/api/user-actions")
@ConditionalOnProperty(prefix = "aqua.logging.userAction", name = ["enabled"], matchIfMissing = true)
class UserActionController(
  private val userActionLogger: UserActionLogger,
  private val userActionEventService: UserActionEventService,
) {
  private val logger = LoggerFactory.getLogger(UserActionController::class.java)

  /**
   * 记录用户操作日志
   */
  @PostMapping("/log")
  fun logUserAction(
    @Valid @RequestBody request: UserActionLogRequest,
    httpRequest: HttpServletRequest,
  ): ResponseEntity<ApiResponse<String>> {
    try {
      // 设置用户上下文信息
      val properties = mutableMapOf<String, Any>()

      // 添加请求相关信息
      properties["userAgent"] = httpRequest.getHeader("User-Agent") ?: "Unknown"
      properties["clientIp"] = getClientIp(httpRequest)
      properties["timestamp"] = request.timestamp

      // 添加用户信息（如果有）
      request.userId?.let { properties["userId"] = it }
      request.username?.let { properties["username"] = it }

      // 使用异步处理服务处理用户操作
      val success = userActionEventService.processUserActionAsync(request)

      return ResponseEntity.ok(ApiResponse.success("User action logged successfully"))
    } catch (e: Exception) {
      logger.error("Failed to log user action", e)
      return ResponseEntity.ok(ApiResponse.success("User action logged with warnings"))
    }
  }

  /**
   * 批量记录用户操作日志
   */
  @PostMapping("/batch")
  fun logUserActionsBatch(
    @Valid @RequestBody request: BatchUserActionLogRequest,
    httpRequest: HttpServletRequest,
  ): ResponseEntity<ApiResponse<String>> {
    try {
      val userAgent = httpRequest.getHeader("User-Agent") ?: "Unknown"
      val clientIp = getClientIp(httpRequest)

      // 转换为单个请求并批量处理
      val singleRequests =
        request.actions.map { action ->
          action.userAgent = userAgent
          action.clientIp = clientIp

          UserActionLogRequest(
            userId = action.userId,
            username = action.username,
            actionType = action.actionType,
            target = action.target,
            coordinates = action.coordinates,
            properties = action.properties,
            timestamp = action.timestamp,
          )
        }

      // 批量处理用户操作
      userActionEventService.processUserActionsBatch(singleRequests)

      return ResponseEntity.ok(ApiResponse.success("Batch user actions logged successfully"))
    } catch (e: Exception) {
      logger.error("Failed to log batch user actions", e)
      return ResponseEntity.ok(ApiResponse.success("Batch user actions logged with warnings"))
    }
  }

  /**
   * 获取客户端真实IP地址
   */
  private fun getClientIp(request: HttpServletRequest): String {
    val xForwardedFor = request.getHeader("X-Forwarded-For")
    if (!xForwardedFor.isNullOrEmpty()) {
      return xForwardedFor.split(",")[0].trim()
    }

    val xRealIp = request.getHeader("X-Real-IP")
    if (!xRealIp.isNullOrEmpty()) {
      return xRealIp
    }

    val xForwardedProto = request.getHeader("X-Forwarded-Proto")
    if (!xForwardedProto.isNullOrEmpty()) {
      return request.remoteAddr
    }

    return request.remoteAddr
  }

  /**
   * 用户操作日志请求体
   */
  data class UserActionLogRequest(
    val userId: String? = null,
    val username: String? = null,
    @field:NotBlank val actionType: String,
    @field:NotBlank val target: String,
    val coordinates: CoordinatesDto? = null,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
  )

  /**
   * 坐标信息DTO
   */
  data class CoordinatesDto(
    val screenX: Int,
    val screenY: Int,
    val pageX: Int,
    val pageY: Int,
  )

  /**
   * 批量用户操作日志请求体
   */
  data class BatchUserActionLogRequest(
    @field:NotNull val actions: List<BatchUserAction>,
  )

  /**
   * 批量操作中的单个用户操作
   */
  data class BatchUserAction(
    val userId: String? = null,
    val username: String? = null,
    @field:NotBlank val actionType: String,
    @field:NotBlank val target: String,
    val coordinates: CoordinatesDto? = null,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    var userAgent: String = "",
    var clientIp: String = "",
  )
}
