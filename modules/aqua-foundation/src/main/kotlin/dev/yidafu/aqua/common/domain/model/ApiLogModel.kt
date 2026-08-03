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

package dev.yidafu.aqua.common.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * API请求日志实体类
 * 用于记录HTTP和GraphQL请求日志
 */
@Entity
@Table(
  name = "api_logs",
  indexes = [
    Index(name = "idx_api_logs_correlation_id", columnList = "correlation_id"),
    Index(name = "idx_api_logs_request_type", columnList = "request_type"),
    Index(name = "idx_api_logs_created_at", columnList = "created_at"),
    Index(name = "idx_api_logs_user_id", columnList = "user_id"),
    Index(name = "idx_api_logs_response_status", columnList = "response_status"),
  ],
)
data class ApiLogModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "correlation_id", length = 50)
  val correlationId: String? = null,
  @Column(name = "request_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  val requestType: RequestType = RequestType.HTTP,
  @Column(name = "operation_type", length = 20)
  @Enumerated(EnumType.STRING)
  val operationType: OperationType? = null,
  @Column(name = "operation_name", length = 100)
  val operationName: String? = null,
  @Column(name = "method", length = 10)
  val method: String? = null,
  @Column(name = "uri", length = 500)
  val uri: String? = null,
  @Column(name = "query", columnDefinition = "text")
  val query: String? = null,
  @Column(name = "request_body", columnDefinition = "text")
  val requestBody: String? = null,
  @Column(name = "response_status")
  val responseStatus: Int? = null,
  @Column(name = "duration_ms")
  val durationMs: Long? = null,
  @Column(name = "ip_address", length = 50)
  val ipAddress: String? = null,
  @Column(name = "user_agent", length = 500)
  val userAgent: String? = null,
  @Column(name = "user_id")
  val userId: Long? = null,
  @Column(name = "username", length = 100)
  val username: String? = null,
  @Column(name = "error_message", columnDefinition = "text")
  val errorMessage: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * 请求类型枚举
 */
enum class RequestType {
  HTTP,
  GRAPHQL,
}

/**
 * GraphQL操作类型枚举
 */
enum class OperationType {
  QUERY,
  MUTATION,
  SUBSCRIPTION,
}
