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

package dev.yidafu.aqua.analytics.reconciliation.domain.repository

import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.domain.table.ReconciliationDiscrepanciesTable
import dev.yidafu.aqua.common.domain.table.ReconciliationReportsTable
import dev.yidafu.aqua.common.domain.table.ReconciliationTasksTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.domain.table.softDeleteWhere
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

/**
 * 对账域三仓储的 Exposed 实现(JPA→Exposed 迁移 P3)。
 * 表均带 @SoftDelete:查询经 [activeOnly] 过滤;清理类删除用 [softDeleteWhere]
 * (语义决策:原 QueryDSL bulk delete 在 Hibernate @SoftDelete 下同样被改写为软删 UPDATE)。
 * save 为 upsert:id 非空且行存在 → 全可变列 update(对齐 JPA merge),否则 INSERT(雪花 id)。
 * report_data/record_details 的 JSON 结构化转换由本层负责(Jackson)。
 */
@Repository
class ReconciliationTaskRepositoryImpl(
  private val database: Database,
) : ReconciliationTaskRepository {
  override fun findByTaskId(taskId: String): ReconciliationTaskModel? =
    transaction(database) {
      ReconciliationTasksTable
        .selectAll()
        .where { ReconciliationTasksTable.activeOnly { ReconciliationTasksTable.taskId eq taskId } }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByStatus(status: ReconciliationTaskStatus): List<ReconciliationTaskModel> =
    findWhere { ReconciliationTasksTable.status eq status.name }

  override fun findByTaskTypeAndStatus(
    taskType: ReconciliationTaskType,
    status: ReconciliationTaskStatus,
  ): List<ReconciliationTaskModel> =
    findWhere {
      (ReconciliationTasksTable.taskType eq taskType.name) and
        (ReconciliationTasksTable.status eq status.name)
    }

  override fun findByStatusIn(statuses: List<ReconciliationTaskStatus>): List<ReconciliationTaskModel> =
    findWhere { ReconciliationTasksTable.status inList statuses.map { it.name } }

  override fun findFirstByOrderByTaskDateDesc(): ReconciliationTaskModel? =
    transaction(database) {
      ReconciliationTasksTable
        .selectAll()
        .where { ReconciliationTasksTable.activeOnly() }
        .orderBy(ReconciliationTasksTable.taskDate to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsByStatus(status: ReconciliationTaskStatus): Boolean =
    transaction(database) {
      ReconciliationTasksTable
        .selectAll()
        .where { ReconciliationTasksTable.activeOnly { ReconciliationTasksTable.status eq status.name } }
        .limit(1)
        .any()
    }

  override fun findAll(): List<ReconciliationTaskModel> = findWhere { Op.TRUE }

  override fun findAll(pageable: Pageable): Page<ReconciliationTaskModel> {
    val content =
      transaction(database) {
        ReconciliationTasksTable
          .selectAll()
          .where { ReconciliationTasksTable.activeOnly() }
          .orderBy(ReconciliationTasksTable.taskDate to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
      }.map(::toModel)

    val total =
      transaction(database) {
        ReconciliationTasksTable
          .selectAll()
          .where { ReconciliationTasksTable.activeOnly() }
          .count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun deleteAll() =
    transaction(database) {
      ReconciliationTasksTable.softDeleteWhere { Op.TRUE }
    }

  override fun save(entity: ReconciliationTaskModel): ReconciliationTaskModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        ReconciliationTasksTable
          .selectAll()
          .where { ReconciliationTasksTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ReconciliationTasksTable.update({ ReconciliationTasksTable.id eq id }) {
          it[taskId] = entity.taskId
          it[taskType] = entity.taskType.name
          it[status] = entity.status.name
          it[taskDate] = entity.taskDate!!
          it[startTime] = entity.startTime
          it[endTime] = entity.endTime
          it[totalRecords] = entity.totalRecords
          it[matchedRecords] = entity.matchedRecords
          it[unmatchedRecords] = entity.unmatchedRecords
          it[errorMessage] = entity.errorMessage
          it[updatedAt] = now
        }
      } else {
        ReconciliationTasksTable.insert {
          it[ReconciliationTasksTable.id] = id
          it[taskId] = entity.taskId
          it[taskType] = entity.taskType.name
          it[status] = entity.status.name
          it[taskDate] = entity.taskDate!!
          it[startTime] = entity.startTime
          it[endTime] = entity.endTime
          it[totalRecords] = entity.totalRecords
          it[matchedRecords] = entity.matchedRecords
          it[unmatchedRecords] = entity.unmatchedRecords
          it[errorMessage] = entity.errorMessage
          it[createdAt] = now
          it[updatedAt] = now
        }
      }
      entity
    }

  override fun findByTaskDateBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationTaskModel> =
    findWhere {
      (ReconciliationTasksTable.taskDate greaterEq startDate) and
        (ReconciliationTasksTable.taskDate lessEq endDate)
    }.sortedByDescending { it.taskDate }

  override fun findByTaskTypeAndTaskDateBetween(
    taskType: ReconciliationTaskType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationTaskModel> =
    findWhere {
      (ReconciliationTasksTable.taskType eq taskType.name) and
        (ReconciliationTasksTable.taskDate greaterEq startDate) and
        (ReconciliationTasksTable.taskDate lessEq endDate)
    }.sortedByDescending { it.taskDate }

  override fun countByTaskTypeAndStatus(
    taskType: ReconciliationTaskType,
    status: ReconciliationTaskStatus,
  ): Long =
    transaction(database) {
      ReconciliationTasksTable
        .selectAll()
        .where {
          ReconciliationTasksTable.activeOnly {
            (ReconciliationTasksTable.taskType eq taskType.name) and
              (ReconciliationTasksTable.status eq status.name)
          }
        }.count()
    }

  override fun findTasksPaged(
    taskType: ReconciliationTaskType?,
    status: ReconciliationTaskStatus?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<ReconciliationTaskModel> {
    val filter = buildFilter(taskType, status, startDate, endDate)
    val content =
      transaction(database) {
        ReconciliationTasksTable
          .selectAll()
          .where { ReconciliationTasksTable.activeOnly({ filter }) }
          .orderBy(ReconciliationTasksTable.taskDate to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
      }.map(::toModel)

    val total =
      transaction(database) {
        ReconciliationTasksTable
          .selectAll()
          .where { ReconciliationTasksTable.activeOnly({ filter }) }
          .count()
      }
    return PageImpl(content, pageable, total)
  }

  private fun buildFilter(
    taskType: ReconciliationTaskType?,
    status: ReconciliationTaskStatus?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): Op<Boolean> {
    var op: Op<Boolean> = Op.TRUE
    taskType?.let { op = op and (ReconciliationTasksTable.taskType eq it.name) }
    status?.let { op = op and (ReconciliationTasksTable.status eq it.name) }
    startDate?.let { op = op and (ReconciliationTasksTable.taskDate greaterEq it) }
    endDate?.let { op = op and (ReconciliationTasksTable.taskDate lessEq it) }
    return op
  }

  private fun findWhere(where: () -> Op<Boolean>): List<ReconciliationTaskModel> =
    transaction(database) {
      ReconciliationTasksTable
        .selectAll()
        .where { ReconciliationTasksTable.activeOnly(where) }
        .toList()
    }.map(::toModel)

  private fun toModel(row: ResultRow): ReconciliationTaskModel =
    ReconciliationTaskModel().apply {
      id = row[ReconciliationTasksTable.id]
      taskId = row[ReconciliationTasksTable.taskId]
      status = ReconciliationTaskStatus.valueOf(row[ReconciliationTasksTable.status])
      taskType = ReconciliationTaskType.valueOf(row[ReconciliationTasksTable.taskType])
      taskDate = row[ReconciliationTasksTable.taskDate]
      startTime = row[ReconciliationTasksTable.startTime]
      endTime = row[ReconciliationTasksTable.endTime]
      totalRecords = row[ReconciliationTasksTable.totalRecords]
      matchedRecords = row[ReconciliationTasksTable.matchedRecords]
      unmatchedRecords = row[ReconciliationTasksTable.unmatchedRecords]
      errorMessage = row[ReconciliationTasksTable.errorMessage]
      createdAt = row[ReconciliationTasksTable.createdAt]
      updatedAt = row[ReconciliationTasksTable.updatedAt]
    }
}

@Repository
class ReconciliationReportRepositoryImpl(
  private val database: Database,
  private val objectMapper: ObjectMapper,
) : ReconciliationReportRepository {
  override fun findByTaskId(taskId: String): List<ReconciliationReportModel> = findWhere { ReconciliationReportsTable.taskId eq taskId }

  override fun findByTaskIdAndReportType(
    taskId: String,
    reportType: String,
  ): ReconciliationReportModel? =
    transaction(database) {
      ReconciliationReportsTable
        .selectAll()
        .where {
          ReconciliationReportsTable.activeOnly {
            (ReconciliationReportsTable.taskId eq taskId) and
              (ReconciliationReportsTable.reportType eq reportType)
          }
        }.limit(1)
        .firstOrNull()
    }?.let { toModel(it) }

  override fun findByReportType(reportType: String): List<ReconciliationReportModel> =
    findWhere { ReconciliationReportsTable.reportType eq reportType }

  override fun findFirstByOrderByGeneratedAtDesc(): ReconciliationReportModel? =
    transaction(database) {
      ReconciliationReportsTable
        .selectAll()
        .where { ReconciliationReportsTable.activeOnly() }
        .orderBy(ReconciliationReportsTable.generatedAt to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByGeneratedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationReportModel> =
    transaction(database) {
      ReconciliationReportsTable
        .selectAll()
        .where {
          ReconciliationReportsTable.activeOnly {
            (ReconciliationReportsTable.generatedAt greaterEq startDate) and
              (ReconciliationReportsTable.generatedAt lessEq endDate)
          }
        }.orderBy(ReconciliationReportsTable.generatedAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun deleteAll(): Int =
    transaction(database) {
      ReconciliationReportsTable.softDeleteWhere { Op.TRUE }
    }

  override fun deleteReportsBefore(beforeDate: LocalDateTime): Int =
    transaction(database) {
      ReconciliationReportsTable.softDeleteWhere { ReconciliationReportsTable.generatedAt less beforeDate }
    }

  override fun countByTaskIdAndReportType(
    taskId: String,
    reportType: String,
  ): Long =
    transaction(database) {
      ReconciliationReportsTable
        .selectAll()
        .where {
          ReconciliationReportsTable.activeOnly {
            (ReconciliationReportsTable.taskId eq taskId) and
              (ReconciliationReportsTable.reportType eq reportType)
          }
        }.count()
    }

  override fun save(entity: ReconciliationReportModel): ReconciliationReportModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val reportDataJson = objectMapper.writeValueAsString(entity.reportData)
      val exists =
        ReconciliationReportsTable
          .selectAll()
          .where { ReconciliationReportsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ReconciliationReportsTable.update({ ReconciliationReportsTable.id eq id }) {
          it[taskId] = entity.taskId
          it[reportType] = entity.reportType
          it[reportData] = reportDataJson
          it[filePath] = entity.filePath
          it[fileName] = entity.fileName
          it[fileSize] = entity.fileSize
          it[generatedAt] = entity.generatedAt
          it[updatedAt] = now
        }
      } else {
        ReconciliationReportsTable.insert {
          it[ReconciliationReportsTable.id] = id
          it[taskId] = entity.taskId
          it[reportType] = entity.reportType
          it[reportData] = reportDataJson
          it[filePath] = entity.filePath
          it[fileName] = entity.fileName
          it[fileSize] = entity.fileSize
          it[generatedAt] = entity.generatedAt
          it[createdAt] = now
          it[updatedAt] = now
        }
      }
      entity
    }

  private fun findWhere(where: () -> Op<Boolean>): List<ReconciliationReportModel> =
    transaction(database) {
      ReconciliationReportsTable
        .selectAll()
        .where { ReconciliationReportsTable.activeOnly(where) }
        .toList()
    }.map(::toModel)

  private fun toModel(row: ResultRow): ReconciliationReportModel =
    ReconciliationReportModel().apply {
      id = row[ReconciliationReportsTable.id]
      taskId = row[ReconciliationReportsTable.taskId]
      reportType = row[ReconciliationReportsTable.reportType]
      reportData = parseJsonMap(objectMapper, row[ReconciliationReportsTable.reportData])
      filePath = row[ReconciliationReportsTable.filePath]
      fileName = row[ReconciliationReportsTable.fileName]
      fileSize = row[ReconciliationReportsTable.fileSize]
      generatedAt = row[ReconciliationReportsTable.generatedAt]
    }
}

@Repository
class ReconciliationDiscrepancyRepositoryImpl(
  private val database: Database,
  private val objectMapper: ObjectMapper,
) : ReconciliationDiscrepancyRepository {
  override fun findByTaskId(taskId: String): List<ReconciliationDiscrepancyModel> =
    findWhere { ReconciliationDiscrepanciesTable.taskId eq taskId }

  override fun findByTaskIdAndStatus(
    taskId: String,
    status: String,
  ): List<ReconciliationDiscrepancyModel> =
    findWhere {
      (ReconciliationDiscrepanciesTable.taskId eq taskId) and
        (ReconciliationDiscrepanciesTable.status eq status)
    }

  override fun findByDiscrepancyType(discrepancyType: DiscrepancyType): List<ReconciliationDiscrepancyModel> =
    findWhere { ReconciliationDiscrepanciesTable.discrepancyType eq discrepancyType.name }

  override fun findBySourceSystem(sourceSystem: SourceSystem): List<ReconciliationDiscrepancyModel> =
    findWhere { ReconciliationDiscrepanciesTable.sourceSystem eq sourceSystem.name }

  override fun findByTaskIdAndDiscrepancyType(
    taskId: String,
    discrepancyType: DiscrepancyType,
  ): List<ReconciliationDiscrepancyModel> =
    findWhere {
      (ReconciliationDiscrepanciesTable.taskId eq taskId) and
        (ReconciliationDiscrepanciesTable.discrepancyType eq discrepancyType.name)
    }

  override fun findByStatus(status: String): List<ReconciliationDiscrepancyModel> =
    findWhere { ReconciliationDiscrepanciesTable.status eq status }

  override fun findById(id: Long): java.util.Optional<ReconciliationDiscrepancyModel> =
    transaction(database) {
      ReconciliationDiscrepanciesTable
        .selectAll()
        .where { ReconciliationDiscrepanciesTable.activeOnly { ReconciliationDiscrepanciesTable.id eq id } }
        .limit(1)
        .firstOrNull()
    }.let { java.util.Optional.ofNullable(it?.let(::toModel)) }

  override fun save(entity: ReconciliationDiscrepancyModel): ReconciliationDiscrepancyModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val detailsJson = entity.recordDetails?.let { objectMapper.writeValueAsString(it) }
      val exists =
        ReconciliationDiscrepanciesTable
          .selectAll()
          .where { ReconciliationDiscrepanciesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ReconciliationDiscrepanciesTable.update({ ReconciliationDiscrepanciesTable.id eq id }) {
          it[status] = entity.status.name
          it[resolutionNotes] = entity.resolutionNotes
          it[resolvedBy] = entity.resolvedBy
          it[resolvedAt] = entity.resolvedAt
          it[recordDetails] = detailsJson
          it[updatedAt] = now
        }
      } else {
        ReconciliationDiscrepanciesTable.insert {
          it[ReconciliationDiscrepanciesTable.id] = id
          it[taskId] = entity.taskId
          it[discrepancyType] = entity.discrepancyType.name
          it[sourceSystem] = entity.sourceSystem.name
          it[recordId] = entity.recordId
          it[recordDetails] = detailsJson
          it[status] = entity.status.name
          it[resolutionNotes] = entity.resolutionNotes
          it[resolvedBy] = entity.resolvedBy
          it[resolvedAt] = entity.resolvedAt
          it[createdAt] = now
          it[updatedAt] = now
        }
      }
      entity
    }

  override fun saveAll(entities: List<ReconciliationDiscrepancyModel>): List<ReconciliationDiscrepancyModel> = entities.map(::save)

  override fun countUnresolvedByTaskId(taskId: String): Long =
    transaction(database) {
      ReconciliationDiscrepanciesTable
        .selectAll()
        .where {
          ReconciliationDiscrepanciesTable.activeOnly {
            (ReconciliationDiscrepanciesTable.taskId eq taskId) and
              (ReconciliationDiscrepanciesTable.status eq DiscrepancyStatus.UNRESOLVED.name)
          }
        }.count()
    }

  override fun countByDiscrepancyTypeGroup(taskId: String): List<Pair<String, Long>> =
    transaction(database) {
      ReconciliationDiscrepanciesTable
        .select(ReconciliationDiscrepanciesTable.discrepancyType, ReconciliationDiscrepanciesTable.id.count())
        .where { ReconciliationDiscrepanciesTable.activeOnly { ReconciliationDiscrepanciesTable.taskId eq taskId } }
        .groupBy(ReconciliationDiscrepanciesTable.discrepancyType)
        .toList()
    }.map { row ->
      row[ReconciliationDiscrepanciesTable.discrepancyType] to row[ReconciliationDiscrepanciesTable.id.count()]
    }

  override fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReconciliationDiscrepancyModel> =
    transaction(database) {
      ReconciliationDiscrepanciesTable
        .selectAll()
        .where {
          ReconciliationDiscrepanciesTable.activeOnly {
            (ReconciliationDiscrepanciesTable.createdAt greaterEq startDate) and
              (ReconciliationDiscrepanciesTable.createdAt lessEq endDate)
          }
        }.orderBy(ReconciliationDiscrepanciesTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun deleteAll(): Int =
    transaction(database) {
      ReconciliationDiscrepanciesTable.softDeleteWhere { Op.TRUE }
    }

  override fun deleteResolvedBefore(beforeDate: LocalDateTime): Int =
    transaction(database) {
      ReconciliationDiscrepanciesTable.softDeleteWhere {
        (ReconciliationDiscrepanciesTable.status eq DiscrepancyStatus.RESOLVED.name) and
          (ReconciliationDiscrepanciesTable.resolvedAt less beforeDate)
      }
    }

  override fun findDiscrepanciesPaged(
    taskId: String?,
    status: DiscrepancyStatus?,
    pageable: Pageable,
  ): Page<ReconciliationDiscrepancyModel> {
    val filter = buildFilter(taskId, status)
    val content =
      transaction(database) {
        ReconciliationDiscrepanciesTable
          .selectAll()
          .where { ReconciliationDiscrepanciesTable.activeOnly({ filter }) }
          .orderBy(ReconciliationDiscrepanciesTable.createdAt to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
      }.map(::toModel)

    val total =
      transaction(database) {
        ReconciliationDiscrepanciesTable
          .selectAll()
          .where { ReconciliationDiscrepanciesTable.activeOnly({ filter }) }
          .count()
      }
    return PageImpl(content, pageable, total)
  }

  private fun buildFilter(
    taskId: String?,
    status: DiscrepancyStatus?,
  ): Op<Boolean> {
    var op: Op<Boolean> = Op.TRUE
    taskId?.let { op = op and (ReconciliationDiscrepanciesTable.taskId eq it) }
    status?.let { op = op and (ReconciliationDiscrepanciesTable.status eq it.name) }
    return op
  }

  private fun findWhere(where: () -> Op<Boolean>): List<ReconciliationDiscrepancyModel> =
    transaction(database) {
      ReconciliationDiscrepanciesTable
        .selectAll()
        .where { ReconciliationDiscrepanciesTable.activeOnly(where) }
        .toList()
    }.map(::toModel)

  private fun toModel(row: ResultRow): ReconciliationDiscrepancyModel =
    ReconciliationDiscrepancyModel().apply {
      id = row[ReconciliationDiscrepanciesTable.id]
      taskId = row[ReconciliationDiscrepanciesTable.taskId]
      discrepancyType = DiscrepancyType.valueOf(row[ReconciliationDiscrepanciesTable.discrepancyType])
      sourceSystem = SourceSystem.valueOf(row[ReconciliationDiscrepanciesTable.sourceSystem])
      recordId = row[ReconciliationDiscrepanciesTable.recordId]
      recordDetails = row[ReconciliationDiscrepanciesTable.recordDetails]?.let { parseJsonMap(objectMapper, it) }
      status = DiscrepancyStatus.valueOf(row[ReconciliationDiscrepanciesTable.status])
      resolutionNotes = row[ReconciliationDiscrepanciesTable.resolutionNotes]
      resolvedBy = row[ReconciliationDiscrepanciesTable.resolvedBy]
      resolvedAt = row[ReconciliationDiscrepanciesTable.resolvedAt]
      createdAt = row[ReconciliationDiscrepanciesTable.createdAt]
      updatedAt = row[ReconciliationDiscrepanciesTable.updatedAt]
    }
}

/**
 * 读回 json 列时的**兼容性容错**:历史上 H2(测试栈已移除,现为 Testcontainers 真实 PG)
 * 的 json 列 `setString` 会把文本再包一层 JSON string。真实 PG 不会这样,但仍按需剥一层
 * (幂等,对正常单层 JSON 无影响),以免历史上被双重编码的存量数据读不出来。
 */
private fun parseJsonMap(
  objectMapper: ObjectMapper,
  json: String,
): Map<String, Any> {
  val trimmed = json.trim()
  val effective =
    if (trimmed.startsWith("\"")) objectMapper.readValue(trimmed, String::class.java) else trimmed
  return objectMapper.readValue(
    effective,
    object : tools.jackson.core.type.TypeReference<Map<String, Any>>() {},
  )
}
