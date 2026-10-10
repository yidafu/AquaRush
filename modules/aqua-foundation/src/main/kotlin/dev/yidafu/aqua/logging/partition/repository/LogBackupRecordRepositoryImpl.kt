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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.logging.partition.repository

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.common.domain.table.LogBackupRecordsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.logging.partition.domain.repository.LogBackupRecordRepository
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 日志离线备份记录仓储实现(JPA→Exposed 迁移 P5,替代 Spring Data JPA 派生查询)。
 *
 * 语义与迁移前一致:查询无 `is_deleted` 过滤(备份记录表无软删列);
 * [findAll] 按调用方 [Pageable] 的 Sort 解析列,缺省 started_at 倒序(id 倒序破平)。
 */
@Repository
class LogBackupRecordRepositoryImpl(
  private val database: Database,
) : LogBackupRecordRepository {
  /** [Pageable] Sort 属性名 → 列映射(原 JPA 派生查询按实体属性名解析 Sort)。 */
  private val sortColumns: Map<String, Expression<*>> =
    mapOf(
      "id" to LogBackupRecordsTable.id,
      "logTable" to LogBackupRecordsTable.logTable,
      "partitionName" to LogBackupRecordsTable.partitionName,
      "partitionMonth" to LogBackupRecordsTable.partitionMonth,
      "status" to LogBackupRecordsTable.status,
      "rowCount" to LogBackupRecordsTable.rowCount,
      "startedAt" to LogBackupRecordsTable.startedAt,
      "finishedAt" to LogBackupRecordsTable.finishedAt,
      "createdAt" to LogBackupRecordsTable.createdAt,
    )

  private fun toModel(row: ResultRow): LogBackupRecordModel =
    LogBackupRecordModel(
      id = row[LogBackupRecordsTable.id],
      logTable = LogTableType.valueOf(row[LogBackupRecordsTable.logTable]),
      partitionName = row[LogBackupRecordsTable.partitionName],
      partitionMonth = row[LogBackupRecordsTable.partitionMonth],
      coldTableName = row[LogBackupRecordsTable.coldTableName],
      fromBound = row[LogBackupRecordsTable.fromBound],
      toBound = row[LogBackupRecordsTable.toBound],
      rowCount = row[LogBackupRecordsTable.rowCount],
      filePath = row[LogBackupRecordsTable.filePath],
      fileSizeBytes = row[LogBackupRecordsTable.fileSizeBytes],
      fileChecksum = row[LogBackupRecordsTable.fileChecksum],
      status = LogBackupStatus.valueOf(row[LogBackupRecordsTable.status]),
      errorMessage = row[LogBackupRecordsTable.errorMessage],
      operatorId = row[LogBackupRecordsTable.operatorId],
      operatorName = row[LogBackupRecordsTable.operatorName],
      startedAt = row[LogBackupRecordsTable.startedAt],
      finishedAt = row[LogBackupRecordsTable.finishedAt],
      createdAt = row[LogBackupRecordsTable.createdAt],
    )

  override fun save(entity: LogBackupRecordModel): LogBackupRecordModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        LogBackupRecordsTable
          .selectAll()
          .where { LogBackupRecordsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        LogBackupRecordsTable.update({ LogBackupRecordsTable.id eq id }) {
          it[logTable] = entity.logTable.name
          it[partitionName] = entity.partitionName
          it[partitionMonth] = entity.partitionMonth
          it[coldTableName] = entity.coldTableName
          it[fromBound] = entity.fromBound
          it[toBound] = entity.toBound
          it[rowCount] = entity.rowCount
          it[filePath] = entity.filePath
          it[fileSizeBytes] = entity.fileSizeBytes
          it[fileChecksum] = entity.fileChecksum
          it[status] = entity.status.name
          it[errorMessage] = entity.errorMessage
          it[operatorId] = entity.operatorId
          it[operatorName] = entity.operatorName
          it[finishedAt] = entity.finishedAt
        }
      } else {
        LogBackupRecordsTable.insert {
          it[LogBackupRecordsTable.id] = id
          it[logTable] = entity.logTable.name
          it[partitionName] = entity.partitionName
          it[partitionMonth] = entity.partitionMonth
          it[coldTableName] = entity.coldTableName
          it[fromBound] = entity.fromBound
          it[toBound] = entity.toBound
          it[rowCount] = entity.rowCount
          it[filePath] = entity.filePath
          it[fileSizeBytes] = entity.fileSizeBytes
          it[fileChecksum] = entity.fileChecksum
          it[status] = entity.status.name
          it[errorMessage] = entity.errorMessage
          it[operatorId] = entity.operatorId
          it[operatorName] = entity.operatorName
          it[startedAt] = entity.startedAt
          it[finishedAt] = entity.finishedAt
          it[createdAt] = entity.createdAt
        }
      }
      entity
    }

  override fun findAll(pageable: Pageable): Page<LogBackupRecordModel> = pageBy(null, pageable)

  override fun findByLogTableOrderByStartedAtDesc(
    logTable: LogTableType,
    pageable: Pageable,
  ): Page<LogBackupRecordModel> = pageBy(logTable, pageable)

  private fun pageBy(
    logTable: LogTableType?,
    pageable: Pageable,
  ): Page<LogBackupRecordModel> {
    val orders =
      pageable.sort
        .mapNotNull { order ->
          sortColumns[order.property]?.let { col ->
            val sortOrder = if (order.isAscending) SortOrder.ASC else SortOrder.DESC
            col to sortOrder
          }
        }.ifEmpty { listOf(LogBackupRecordsTable.startedAt to SortOrder.DESC) }
    val (content, total) =
      transaction(database) {
        val content =
          LogBackupRecordsTable
            .selectAll()
            .apply { if (logTable != null) where { LogBackupRecordsTable.logTable eq logTable.name } }
            // id 倒序破平:同刻记录顺序稳定,分页不重不漏
            .orderBy(*(orders + (LogBackupRecordsTable.id to SortOrder.DESC)).toTypedArray())
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        val total =
          LogBackupRecordsTable
            .selectAll()
            .apply { if (logTable != null) where { LogBackupRecordsTable.logTable eq logTable.name } }
            .count()
        content to total
      }
    return PageImpl(content, pageable, total)
  }

  override fun existsByPartitionNameAndStatusIn(
    partitionName: String,
    statuses: List<LogBackupStatus>,
  ): Boolean =
    transaction(database) {
      LogBackupRecordsTable
        .selectAll()
        .where {
          (LogBackupRecordsTable.partitionName eq partitionName) and
            (LogBackupRecordsTable.status inList statuses.map { it.name })
        }.limit(1)
        .any()
    }

  override fun findFirstByPartitionNameOrderByStartedAtDesc(partitionName: String): LogBackupRecordModel? =
    transaction(database) {
      LogBackupRecordsTable
        .selectAll()
        .where { LogBackupRecordsTable.partitionName eq partitionName }
        .orderBy(LogBackupRecordsTable.startedAt to SortOrder.DESC, LogBackupRecordsTable.id to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)
}
