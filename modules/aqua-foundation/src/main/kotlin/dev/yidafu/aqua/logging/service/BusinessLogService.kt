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

package dev.yidafu.aqua.logging.service

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.common.graphql.generated.BusinessLogListInput
import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.repository.BusinessLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 业务日志服务
 */
@Service
class BusinessLogService(
  private val businessLogRepository: BusinessLogRepository,
  private val loggingProperties: LoggingProperties,
) {
  private val logger = LoggerFactory.getLogger(BusinessLogService::class.java)

  /**
   * 保存业务日志
   */
  @Async
  fun saveBusinessLog(
    correlationId: String?,
    level: String,
    loggerName: String?,
    message: String?,
    stackTrace: String? = null,
    userId: Long? = null,
    username: String? = null,
    operation: String? = null,
  ) {
    if (!loggingProperties.database.businessLogEnabled) {
      return
    }

    try {
      val businessLog =
        BusinessLogModel(
          correlationId = correlationId,
          level = level,
          loggerName = loggerName,
          operation = operation,
          message = message,
          stackTrace = stackTrace,
          userId = userId,
          username = username,
          createdAt = LocalDateTime.now(),
        )
      businessLogRepository.save(businessLog)
    } catch (e: Exception) {
      logger.error("Failed to save business log: {}", e.message, e)
    }
  }

  /**
   * 分页查询业务日志(REST 用,operation 不做条件)
   */
  fun queryBusinessLogs(
    level: String?,
    loggerName: String?,
    userId: Long?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<BusinessLogModel> =
    businessLogRepository.findByFilters(
      level,
      loggerName,
      null,
      userId,
      startTime,
      endTime,
      pageable,
    )

  /**
   * 分页查询业务日志(GraphQL 用,含业务域/操作类型维度,恒 createdAt 倒序 —— F-LOG-003)
   *
   * input.domain(业务域枚举名 ORDER/PAYMENT/DELIVERY/USER/AUTH)映射 logger_name 模糊筛选;
   * input.operationType 精确匹配 operation 列;page/size 空值回填默认 0/10。
   */
  fun queryBusinessLogs(input: BusinessLogListInput): Page<BusinessLogModel> =
    businessLogRepository.findByFilters(
      level = input.level?.name,
      loggerName = input.domain?.name,
      operation = input.operationType,
      userId = input.userId,
      startTime = input.startTime,
      endTime = input.endTime,
      pageable =
        PageRequest.of(
          input.page ?: 0,
          input.size ?: 10,
          Sort.by(Sort.Direction.DESC, "createdAt"),
        ),
    )

  /**
   * 根据关联ID查询日志
   */
  fun findByCorrelationId(correlationId: String): List<BusinessLogModel> = businessLogRepository.findByCorrelationId(correlationId)
}
