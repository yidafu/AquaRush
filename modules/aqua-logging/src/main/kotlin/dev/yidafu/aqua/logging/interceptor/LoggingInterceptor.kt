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

package dev.yidafu.aqua.logging.interceptor

import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

/**
 * 日志拦截器，用于记录请求和响应信息
 */
@Component
class LoggingInterceptor : HandlerInterceptor {
  private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    val startTime = System.currentTimeMillis()
    request.setAttribute("startTime", startTime)

    // 记录请求信息
    val correlationId = CorrelationIdHolder.getCorrelationId()
    val clientIp = getClientIpAddress(request)
    val userAgent = request.getHeader("User-Agent")

    logger.info(
      "Request started - Method: {}, URI: {}, IP: {}, UserAgent: {}, CorrelationId: {}",
      request.method,
      request.requestURI,
      clientIp,
      userAgent,
      correlationId,
    )

    return true
  }

  override fun postHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    modelAndView: ModelAndView?,
  ) {
    // 后处理，可以在这里记录一些额外的信息
  }

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    exception: Exception?,
  ) {
    val startTime = request.getAttribute("startTime") as? Long ?: return
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime

    val correlationId = CorrelationIdHolder.getCorrelationId()

    if (exception != null) {
      // 记录异常信息
      logger.error(
        "Request completed with error - Method: {}, URI: {}, Status: {}, Duration: {}ms, CorrelationId: {}, Error: {}",
        request.method,
        request.requestURI,
        response.status,
        duration,
        correlationId,
        exception.message,
        exception,
      )
    } else {
      // 记录正常完成信息
      val level =
        when {
          response.status >= 500 -> "ERROR"
          response.status >= 400 -> "WARN"
          duration > 5000 -> "WARN" // 慢请求警告
          else -> "INFO"
        }

      @Suppress("ktlint:standard:max-line-length")
      val message = "Request completed - Method: ${request.method}, URI: ${request.requestURI}, Status: ${response.status}, Duration: ${duration}ms, CorrelationId: $correlationId"

      when (level) {
        "ERROR" -> logger.error(message)
        "WARN" -> logger.warn(message)
        else -> logger.info(message)
      }
    }

    // 性能警告
    if (duration > 5000) {
      logger.warn(
        "Slow request detected - Method: {}, URI: {}, Duration: {}ms, CorrelationId: {}",
        request.method,
        request.requestURI,
        duration,
        correlationId,
      )
    }
  }

  /**
   * 获取客户端IP地址
   */
  private fun getClientIpAddress(request: HttpServletRequest): String {
    val xForwardedFor = request.getHeader("X-Forwarded-For")
    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equals(xForwardedFor, ignoreCase = true)) {
      return xForwardedFor.split(",").first().trim()
    }

    val xRealIp = request.getHeader("X-Real-IP")
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equals(xRealIp, ignoreCase = true)) {
      return xRealIp
    }

    val xForwardedProto = request.getHeader("X-Forwarded-Proto")
    if (xForwardedProto != null && !xForwardedProto.isEmpty()) {
      // 如果通过代理转发，获取代理的IP
      val remoteAddr = request.getHeader("Remote-Addr")
      if (remoteAddr != null && !remoteAddr.isEmpty()) {
        return remoteAddr
      }
    }

    return request.remoteAddr ?: "unknown"
  }
}
