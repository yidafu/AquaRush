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

import dev.yidafu.aqua.common.domain.model.OperationType
import dev.yidafu.aqua.common.domain.model.RequestType
import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import dev.yidafu.aqua.logging.service.ApiLogService
import org.slf4j.LoggerFactory
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono
import kotlin.text.isEmpty
import kotlin.text.split

/**
 * GraphQL日志拦截器
 * 用于记录GraphQL请求日志
 */
class GraphQLLoggingInterceptor(
  private val apiLogService: ApiLogService,
) : WebGraphQlInterceptor {
  private val logger = LoggerFactory.getLogger(GraphQLLoggingInterceptor::class.java)

  override fun intercept(
    request: WebGraphQlRequest,
    chain: WebGraphQlInterceptor.Chain,
  ): Mono<WebGraphQlResponse> {
    val startTime = System.currentTimeMillis()
    val correlationId = CorrelationIdHolder.getCorrelationId()

    // 提取GraphQL查询信息
    val query = request.document
    val operationName = request.operationName
    val variables = request.variables.toString()

    val userAgent = request.headers[HttpHeaders.USER_AGENT].toString()
    val ipAddress = getClientIpAddress(request)
    // 解析操作类型（Query/Mutation）
    val operationType = parseOperationType(query)

    logger.info(
      "GraphQL request started - OperationName: {}, OperationType: {}, CorrelationId: {}",
      operationName,
      operationType,
      correlationId,
    )
    return chain
      .next(request)
      .doOnSuccess { response ->
        val duration = System.currentTimeMillis() - startTime
        val errors = response.errors
        val userName = getUserName()
        // 保存到数据库
        apiLogService.saveApiLog(
          username = userName,
          correlationId = correlationId,
          requestType = RequestType.GRAPHQL,
          method = "POST",
          uri = request.uri.toString(),
          query = truncateQuery(query),
          requestBody = variables,
          responseStatus = if (errors.isEmpty()) 200 else 400,
          durationMs = duration,
          ipAddress = ipAddress,
          userAgent = userAgent,
          errorMessage = if (errors.isNotEmpty()) errors.joinToString(";") { it.message.toString() } else null,
          operationType = operationType,
          operationName = operationName,
        )

        // Mutation操作记录业务日志
        if (operationType == OperationType.MUTATION) {
          logger.info(
            "GraphQL mutation executed - OperationName: {}, Duration: {}ms, CorrelationId: {}",
            operationName,
            duration,
            correlationId,
          )
        }

        if (duration > 5000) {
          logger.warn(
            "Slow GraphQL request detected - OperationName: {}, Duration: {}ms, CorrelationId: {}",
            operationName,
            duration,
            correlationId,
          )
        }
      }.doOnError { error ->
        val duration = System.currentTimeMillis() - startTime

        logger.error(
          "GraphQL request failed - OperationName: {}, Duration: {}ms, CorrelationId: {}, Error: {}",
          operationName,
          duration,
          correlationId,
          error.message,
          error,
        )

        // 保存错误到数据库
        apiLogService.saveApiLog(
          username = getUserName(),
          correlationId = correlationId,
          requestType = RequestType.GRAPHQL,
          method = operationName ?: "anonymous",
          uri = request.uri.toString(),
          query = truncateQuery(query),
          requestBody = variables,
          responseStatus = 500,
          durationMs = duration,
          ipAddress = ipAddress,
          userAgent = userAgent,
          errorMessage = error.message,
          operationType = operationType,
          operationName = operationName,
        )
      }
  }

  /**
   * 解析GraphQL查询的操作类型（Query/Mutation）
   */
  private fun parseOperationType(query: String?): OperationType {
    if (query == null) return OperationType.QUERY

    val trimmedQuery = query.trim().lowercase()

    // 检查是否包含 mutation 关键字
    return when {
      trimmedQuery.contains("mutation") -> OperationType.MUTATION
      trimmedQuery.contains("subscription") -> OperationType.SUBSCRIPTION
      else -> OperationType.QUERY
    }
  }

  /**
   * 截断过长的查询
   */
  private fun truncateQuery(query: String?): String? {
    if (query == null) return null
    return if (query.length > 2000) query.substring(0, 2000) + "..." else query
  }

  private fun getUserName(): String? {
    val authentication = SecurityContextHolder.getContext().authentication
    val userDetail = (authentication?.principal as UserDetails?)
    val userName = userDetail?.username
    return userName
  }

  private fun getClientIpAddress(request: WebGraphQlRequest): String {
    val xForwardedFor = request.headers["X-Forwarded-For"]?.joinToString(",")
    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equals(xForwardedFor, ignoreCase = true)) {
      return xForwardedFor.split(",").first().trim()
    }

    val xRealIp = request.headers["X-Real-IP"]?.joinToString(",")
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equals(xRealIp, ignoreCase = true)) {
      return xRealIp
    }

    val xForwardedProto = request.headers["X-Forwarded-Proto"]?.joinToString(",")
    if (xForwardedProto != null && !xForwardedProto.isEmpty()) {
      // 如果通过代理转发，获取代理的IP
      val remoteAddr = request.headers["Remote-Addr"]?.joinToString(",")
      if (remoteAddr != null && !remoteAddr.isEmpty()) {
        return remoteAddr
      }
    }
    return request.remoteAddress.toString()
  }
}
