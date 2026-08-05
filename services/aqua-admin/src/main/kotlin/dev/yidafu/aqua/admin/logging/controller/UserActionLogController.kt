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

package dev.yidafu.aqua.admin.logging.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.logging.dto.BatchUserActionLogRequest
import dev.yidafu.aqua.logging.dto.UserActionLogRequest
import dev.yidafu.aqua.logging.service.UserActionEventService
import dev.yidafu.aqua.logging.service.UserActionLogService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 用户操作日志控制器
 * 提供RESTful API接口用于接收前端上报的用户操作日志及查询
 */
@RestController
@RequestMapping("/api/logs")
@ConditionalOnProperty(prefix = "aqua.logging.userAction", name = ["enabled"], matchIfMissing = true)
class UserActionLogController(
  private val userActionLogService: UserActionLogService,
  private val userActionEventService: UserActionEventService,
) {
  private val logger = LoggerFactory.getLogger(UserActionLogController::class.java)

  /**
   * 分页查询用户操作日志
   */
  @GetMapping("/user-actions")
  fun queryUserActionLogs(
    @RequestParam(required = false) userId: String?,
    @RequestParam(required = false) actionType: String?,
    @RequestParam(required = false) username: String?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime?,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
  ): Page<UserActionLogModel> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    return userActionLogService.queryUserActionLogs(
      userId,
      actionType,
      username,
      startTime,
      endTime,
      pageable,
    )
  }

  /**
   * 记录用户操作日志
   */
  @PostMapping("/user-actions/log")
  fun logUserAction(
    @Valid @RequestBody request: UserActionLogRequest,
    httpRequest: HttpServletRequest,
  ): ResponseEntity<ApiResponse<String>> {
    try {
      // 使用异步处理服务处理用户操作
      userActionEventService.processUserActionAsync(request)
      return ResponseEntity.ok(ApiResponse.success("User action logged successfully"))
    } catch (e: Exception) {
      logger.error("Failed to log user action", e)
      return ResponseEntity.ok(ApiResponse.success("User action logged with warnings"))
    }
  }

  /**
   * 批量记录用户操作日志
   */
  @PostMapping("/user-actions/batch")
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

    return request.remoteAddr
  }

  // DTOs moved to dev.yidafu.aqua.logging.dto package
}
