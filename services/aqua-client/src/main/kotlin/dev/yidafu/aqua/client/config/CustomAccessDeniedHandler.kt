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

package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.common.web.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 自定义访问拒绝处理器
 * 当用户已认证但没有足够权限访问资源时，返回 JSON 格式的错误信息
 */
@Component
class CustomAccessDeniedHandler(
  private val objectMapper: ObjectMapper
) : AccessDeniedHandler {
  private val logger = LoggerFactory.getLogger(CustomAccessDeniedHandler::class.java)

  override fun handle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    accessDeniedException: AccessDeniedException
  ) {
    val requestURI = request.requestURI
    val method = request.method

    logger.warn(
      "Access denied - Method: {}, URI: {}, User: {}, Reason: {}",
      method,
      requestURI,
      request.userPrincipal?.name ?: "anonymous",
      accessDeniedException.message
    )

    // 记录详细的访问拒绝信息用于审计
    val auditLogger = LoggerFactory.getLogger("dev.yidafu.aqua.audit")
    auditLogger.error(
      "ACCESS_DENIED - Method: {}, URI: {}, User: {}, UserAgent: {}, RemoteAddr: {}",
      method,
      requestURI,
      request.userPrincipal?.name ?: "anonymous",
      request.getHeader("User-Agent"),
      request.remoteAddr
    )

    // 设置响应头
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    response.characterEncoding = "UTF-8"
    response.status = HttpServletResponse.SC_FORBIDDEN

    // 构建错误响应
    val errorResponse = ApiResponse.forbidden(
      message = "权限不足，无法访问: $method $requestURI"
    )

    // 返回 JSON 格式的错误信息
    response.writer.use { writer ->
      writer.write(objectMapper.writeValueAsString(errorResponse))
    }
  }
}
