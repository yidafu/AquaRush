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
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.util.StringUtils

/**
 * 关联ID过滤器，用于在请求开始时设置关联ID，在请求结束时清除
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationFilter : Filter {
  override fun doFilter(
    request: ServletRequest,
    response: ServletResponse,
    chain: FilterChain,
  ) {
    val httpRequest = request as HttpServletRequest
    val httpResponse = response as HttpServletResponse

    try {
      // 从请求头中获取关联ID，如果没有则生成新的
      val correlationId = getOrGenerateCorrelationId(httpRequest)

      // 设置到ThreadLocal中
      CorrelationIdHolder.setCorrelationId(correlationId)

      // 添加到响应头中
      httpResponse.setHeader("X-Correlation-ID", correlationId)

      // 继续处理请求
      chain.doFilter(request, response)
    } finally {
      // 请求处理完成后清理ThreadLocal
      CorrelationIdHolder.clear()
    }
  }

  /**
   * 从请求头中获取关联ID，如果没有则生成新的
   */
  private fun getOrGenerateCorrelationId(request: HttpServletRequest): String {
    // 尝试从多个可能的头部名称中获取关联ID
    val possibleHeaderNames =
      listOf(
        "X-Correlation-ID",
        "X-Request-ID",
        "X-Trace-ID",
        "Correlation-ID",
        "Request-ID",
      )

    for (headerName in possibleHeaderNames) {
      val correlationId = request.getHeader(headerName)
      if (StringUtils.hasText(correlationId)) {
        return correlationId
      }
    }

    // 如果没有找到关联ID，则生成新的
    return CorrelationIdHolder.generateAndSet()
  }

  override fun init(filterConfig: FilterConfig) {
    // 初始化方法，可以在这里进行一些初始化配置
  }

  override fun destroy() {
    // 销毁方法，可以在这里进行一些清理工作
  }
}
