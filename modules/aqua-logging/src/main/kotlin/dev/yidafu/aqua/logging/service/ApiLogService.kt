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

package dev.yidafu.aqua.logging.service

import dev.yidafu.aqua.common.domain.model.ApiLogModel
import dev.yidafu.aqua.common.domain.model.OperationType
import dev.yidafu.aqua.common.domain.model.RequestType
import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.repository.ApiLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * API日志服务
 */
@Service
class ApiLogService(
  private val apiLogRepository: ApiLogRepository,
  private val loggingProperties: LoggingProperties,
) {
  private val logger = LoggerFactory.getLogger(ApiLogService::class.java)

  /**
   * 保存API日志
   */
  @Async
  fun saveApiLog(
    correlationId: String?,
    requestType: RequestType,
    method: String?,
    uri: String?,
    query: String? = null,
    requestBody: String? = null,
    responseStatus: Int? = null,
    durationMs: Long? = null,
    ipAddress: String? = null,
    userAgent: String? = null,
    userId: Long? = null,
    username: String? = null,
    errorMessage: String? = null,
    operationType: OperationType? = null,
    operationName: String? = null,
  ) {
    if (!loggingProperties.database.httpLogEnabled && requestType == RequestType.HTTP) {
      return
    }
    if (!loggingProperties.database.graphqlLogEnabled && requestType == RequestType.GRAPHQL) {
      return
    }

    try {
      val apiLog =
        ApiLogModel(
          correlationId = correlationId,
          requestType = requestType,
          method = method,
          uri = uri,
          query = query,
          requestBody = requestBody,
          responseStatus = responseStatus,
          durationMs = durationMs,
          ipAddress = ipAddress,
          userAgent = userAgent,
          userId = userId,
          username = username,
          errorMessage = errorMessage,
          operationType = operationType,
          operationName = operationName,
          createdAt = LocalDateTime.now(),
        )
      apiLogRepository.save(apiLog)
    } catch (e: Exception) {
      logger.error("Failed to save API log: {}", e.message, e)
    }
  }

  /**
   * 分页查询API日志
   */
  fun queryApiLogs(
    requestType: RequestType?,
    method: String?,
    responseStatus: Int?,
    userId: Long?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<ApiLogModel> =
    apiLogRepository.findByFilters(
      requestType,
      method,
      responseStatus,
      userId,
      startTime,
      endTime,
      pageable,
    )

  /**
   * 根据关联ID查询日志
   */
  fun findByCorrelationId(correlationId: String): List<ApiLogModel> = apiLogRepository.findByCorrelationId(correlationId)
}
