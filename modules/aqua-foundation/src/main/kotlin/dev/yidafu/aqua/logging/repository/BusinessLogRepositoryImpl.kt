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

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.common.domain.table.BusinessLogsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
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
 * 业务日志仓储实现(JPA→Exposed 迁移 P5,替代原 QueryDSL 实现)。
 *
 * 与迁移前语义一致:
 * - [findByFilters] 动态条件(loggerName 模糊) + created_at 倒序(补 id 倒序破平);
 * - `business_logs` 无软删列,查询不做 `is_deleted` 过滤。
 */
@Repository
class BusinessLogRepositoryImpl(
  private val database: Database,
) : BusinessLogRepository {
  private fun toModel(row: ResultRow): BusinessLogModel =
    BusinessLogModel(
      id = row[BusinessLogsTable.id],
      correlationId = row[BusinessLogsTable.correlationId],
      level = row[BusinessLogsTable.level],
      loggerName = row[BusinessLogsTable.loggerName],
      operation = row[BusinessLogsTable.operation],
      message = row[BusinessLogsTable.message],
      stackTrace = row[BusinessLogsTable.stackTrace],
      userId = row[BusinessLogsTable.userId],
      username = row[BusinessLogsTable.username],
      createdAt = row[BusinessLogsTable.createdAt],
    )

  override fun save(entity: BusinessLogModel): BusinessLogModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        BusinessLogsTable
          .selectAll()
          .where { BusinessLogsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        BusinessLogsTable.update({ BusinessLogsTable.id eq id }) {
          it[correlationId] = entity.correlationId
          it[level] = entity.level
          it[loggerName] = entity.loggerName
          it[operation] = entity.operation
          it[message] = entity.message
          it[stackTrace] = entity.stackTrace
          it[userId] = entity.userId
          it[username] = entity.username
        }
      } else {
        BusinessLogsTable.insert {
          it[BusinessLogsTable.id] = id
          it[correlationId] = entity.correlationId
          it[level] = entity.level
          it[loggerName] = entity.loggerName
          it[operation] = entity.operation
          it[message] = entity.message
          it[stackTrace] = entity.stackTrace
          it[userId] = entity.userId
          it[username] = entity.username
          it[createdAt] = entity.createdAt
        }
      }
      entity
    }

  override fun findByCorrelationId(correlationId: String): List<BusinessLogModel> =
    transaction(database) {
      BusinessLogsTable
        .selectAll()
        .where { BusinessLogsTable.correlationId eq correlationId }
        .orderBy(BusinessLogsTable.createdAt to SortOrder.ASC, BusinessLogsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByOperation(operation: String): List<BusinessLogModel> =
    transaction(database) {
      BusinessLogsTable
        .selectAll()
        .where { BusinessLogsTable.operation eq operation }
        .orderBy(BusinessLogsTable.createdAt to SortOrder.ASC, BusinessLogsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByFilters(
    level: String?,
    loggerName: String?,
    operation: String?,
    userId: Long?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<BusinessLogModel> {
    val conditions =
      buildList {
        level?.let { add(BusinessLogsTable.level eq it) }
        loggerName?.let { add(BusinessLogsTable.loggerName like "%$it%") }
        operation?.let { add(BusinessLogsTable.operation eq it) }
        userId?.let { add(BusinessLogsTable.userId eq it) }
        startTime?.let { add(BusinessLogsTable.createdAt greaterEq it) }
        endTime?.let { add(BusinessLogsTable.createdAt lessEq it) }
      }
    val condition = conditions.reduceOrNull { a, b -> a and b } ?: Op.TRUE
    val (content, total) =
      transaction(database) {
        val content =
          BusinessLogsTable
            .selectAll()
            .where { condition }
            .orderBy(BusinessLogsTable.createdAt to SortOrder.DESC, BusinessLogsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to BusinessLogsTable.selectAll().where { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }
}
