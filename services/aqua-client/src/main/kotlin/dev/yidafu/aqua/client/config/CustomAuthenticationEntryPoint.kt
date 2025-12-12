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
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 自定义认证入口点
 * 当用户未认证时，返回 JSON 格式的错误信息
 */
@Component
class CustomAuthenticationEntryPoint(
  private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
  private val logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint::class.java)

  override fun commence(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException
  ) {
    val requestURI = request.requestURI
    val method = request.method

    logger.warn(
      "Authentication required - Method: {}, URI: {}, Reason: {}",
      method,
      requestURI,
      authException.message
    )

    // 记录详细的认证失败信息用于审计
    val auditLogger = LoggerFactory.getLogger("dev.yidafu.aqua.audit")
    auditLogger.error(
      "AUTHENTICATION_REQUIRED - Method: {}, URI: {}, UserAgent: {}, RemoteAddr: {}",
      method,
      requestURI,
      request.getHeader("User-Agent"),
      request.remoteAddr
    )

    // 设置响应头
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    response.characterEncoding = "UTF-8"
    response.status = HttpServletResponse.SC_UNAUTHORIZED

    // 构建错误响应
    val errorResponse = ApiResponse.unauthorized(
      message = "认证失败，请先登录后再访问: $method $requestURI"
    )

    // 返回 JSON 格式的错误信息
    response.writer.use { writer ->
      writer.write(objectMapper.writeValueAsString(errorResponse))
    }
  }
}
