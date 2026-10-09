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

import java.time.LocalDateTime

/**
 * API请求日志实体类
 * 用于记录HTTP和GraphQL请求日志
 */
data class ApiLogModel(
  var id: Long? = null,
  val correlationId: String? = null,
  val requestType: RequestType = RequestType.HTTP,
  val operationType: OperationType? = null,
  val operationName: String? = null,
  val method: String? = null,
  val uri: String? = null,
  val query: String? = null,
  val requestBody: String? = null,
  val responseStatus: Int? = null,
  val durationMs: Long? = null,
  val ipAddress: String? = null,
  val userAgent: String? = null,
  val userId: Long? = null,
  val username: String? = null,
  val errorMessage: String? = null,
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
