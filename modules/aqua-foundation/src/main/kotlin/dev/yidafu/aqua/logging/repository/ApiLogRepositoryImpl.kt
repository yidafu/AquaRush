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

package dev.yidafu.aqua.logging.repository

import dev.yidafu.aqua.common.domain.model.ApiLogModel
import dev.yidafu.aqua.common.domain.model.OperationType
import dev.yidafu.aqua.common.domain.model.RequestType
import dev.yidafu.aqua.common.domain.table.ApiLogsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * API 请求日志仓储实现(JPA→Exposed 迁移 P5,替代原 QueryDSL 实现)。
 *
 * 与迁移前语义一致:
 * - [findByFilters] 动态条件 + created_at 倒序(补 id 倒序破平,避免同刻日志顺序不稳定,分页不重不漏);
 * - [save] 为日志写入场景的 upsert;
 * - `api_logs` 无软删列(实体无 `@SoftDelete`),查询不做 `is_deleted` 过滤。
 */
@Repository
class ApiLogRepositoryImpl(
  private val database: Database,
) : ApiLogRepository {
  private fun toModel(row: ResultRow): ApiLogModel =
    ApiLogModel(
      id = row[ApiLogsTable.id],
      correlationId = row[ApiLogsTable.correlationId],
      requestType = RequestType.valueOf(row[ApiLogsTable.requestType]),
      operationType = row[ApiLogsTable.operationType]?.let { OperationType.valueOf(it) },
      operationName = row[ApiLogsTable.operationName],
      method = row[ApiLogsTable.method],
      uri = row[ApiLogsTable.uri],
      query = row[ApiLogsTable.query],
      requestBody = row[ApiLogsTable.requestBody],
      responseStatus = row[ApiLogsTable.responseStatus],
      durationMs = row[ApiLogsTable.durationMs],
      ipAddress = row[ApiLogsTable.ipAddress],
      userAgent = row[ApiLogsTable.userAgent],
      userId = row[ApiLogsTable.userId],
      username = row[ApiLogsTable.username],
      errorMessage = row[ApiLogsTable.errorMessage],
      createdAt = row[ApiLogsTable.createdAt],
    )

  override fun save(entity: ApiLogModel): ApiLogModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        ApiLogsTable
          .selectAll()
          .where { ApiLogsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ApiLogsTable.update({ ApiLogsTable.id eq id }) {
          it[correlationId] = entity.correlationId
          it[requestType] = entity.requestType.name
          it[operationType] = entity.operationType?.name
          it[operationName] = entity.operationName
          it[method] = entity.method
          it[uri] = entity.uri
          it[query] = entity.query
          it[requestBody] = entity.requestBody
          it[responseStatus] = entity.responseStatus
          it[durationMs] = entity.durationMs
          it[ipAddress] = entity.ipAddress
          it[userAgent] = entity.userAgent
          it[userId] = entity.userId
          it[username] = entity.username
          it[errorMessage] = entity.errorMessage
        }
      } else {
        ApiLogsTable.insert {
          it[ApiLogsTable.id] = id
          it[correlationId] = entity.correlationId
          it[requestType] = entity.requestType.name
          it[operationType] = entity.operationType?.name
          it[operationName] = entity.operationName
          it[method] = entity.method
          it[uri] = entity.uri
          it[query] = entity.query
          it[requestBody] = entity.requestBody
          it[responseStatus] = entity.responseStatus
          it[durationMs] = entity.durationMs
          it[ipAddress] = entity.ipAddress
          it[userAgent] = entity.userAgent
          it[userId] = entity.userId
          it[username] = entity.username
          it[errorMessage] = entity.errorMessage
          it[createdAt] = entity.createdAt
        }
      }
      entity
    }

  override fun findByCorrelationId(correlationId: String): List<ApiLogModel> =
    transaction(database) {
      ApiLogsTable
        .selectAll()
        .where { ApiLogsTable.correlationId eq correlationId }
        .orderBy(ApiLogsTable.createdAt to SortOrder.ASC, ApiLogsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByFilters(
    requestType: RequestType?,
    method: String?,
    responseStatus: Int?,
    userId: Long?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<ApiLogModel> {
    val conditions =
      buildList {
        requestType?.let { add(ApiLogsTable.requestType eq it.name) }
        method?.let { add(ApiLogsTable.method eq it) }
        responseStatus?.let { add(ApiLogsTable.responseStatus eq it) }
        userId?.let { add(ApiLogsTable.userId eq it) }
        startTime?.let { add(ApiLogsTable.createdAt greaterEq it) }
        endTime?.let { add(ApiLogsTable.createdAt lessEq it) }
      }
    val condition = conditions.reduceOrNull { a, b -> a and b } ?: Op.TRUE
    val (content, total) =
      transaction(database) {
        val content =
          ApiLogsTable
            .selectAll()
            .where { condition }
            .orderBy(ApiLogsTable.createdAt to SortOrder.DESC, ApiLogsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to ApiLogsTable.selectAll().where { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }
}
