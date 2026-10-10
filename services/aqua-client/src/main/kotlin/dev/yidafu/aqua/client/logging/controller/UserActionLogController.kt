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

package dev.yidafu.aqua.client.logging.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.logging.dto.BatchUserActionLogRequest
import dev.yidafu.aqua.logging.service.UserActionEventService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 用户操作日志上报控制器(client 端)
 *
 * 小程序端经 `@aquarush/common/tracking` SDK 统一上报到本服务
 * (`POST /api/logs/user-actions/batch`,见 spec/prd/09-platform-logs.md),
 * 因此 client 服务必须注册与 admin 服务一致的批次上报端点,否则请求会落到
 * 静态资源兜底并抛 NoResourceFoundException 变成 500。
 *
 * 处理逻辑复用 aqua-foundation 的 UserActionEventService 共享入口,
 * 与 admin 服务上报链路保持一致;查询/管理类端点仅存在于 admin 服务。
 */
@RestController
@RequestMapping("/api/logs")
class UserActionLogController(
  private val userActionEventService: UserActionEventService,
) {
  private val logger = LoggerFactory.getLogger(UserActionLogController::class.java)

  /**
   * 批量记录用户操作日志
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/user-actions/batch")
  fun logUserActionsBatch(
    @Valid @RequestBody request: BatchUserActionLogRequest,
    httpRequest: HttpServletRequest,
  ): ResponseEntity<ApiResponse<String>> {
    try {
      val userAgent = httpRequest.getHeader("User-Agent") ?: "Unknown"
      val clientIp = getClientIp(httpRequest)

      // 批量处理用户操作(userAgent/clientIp 由服务端注入)
      userActionEventService.processUserActionsBatch(request.actions, userAgent, clientIp)

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
}
