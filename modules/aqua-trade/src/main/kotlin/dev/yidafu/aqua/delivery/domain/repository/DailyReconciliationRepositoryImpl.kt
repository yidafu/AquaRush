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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.table.DailyReconciliationsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 每日对账记录仓储实现(JPA→Exposed 迁移 P4)。
 * 表为裸 Table 但带 is_deleted 列(类级 @SoftDelete)——每个查询显式过滤
 * `is_deleted = false`;deleteAll 为 `UPDATE is_deleted = true`(对齐 @SoftDelete 改写);
 * report_data 为 text 列,JSON 结构化转换由本层负责(Jackson,
 * 解析先例见 analytics ExposedReconciliationRepositories 的 parseJsonMap)。
 */
@Repository
class DailyReconciliationRepositoryImpl(
  private val database: Database,
  private val objectMapper: ObjectMapper,
) : DailyReconciliationRepository {
  private fun toModel(row: ResultRow): DailyReconciliationModel =
    DailyReconciliationModel(
      id = row[DailyReconciliationsTable.id],
      reconciliationDate = row[DailyReconciliationsTable.reconciliationDate],
      totalCashAmountCents = row[DailyReconciliationsTable.totalCashAmountCents],
      totalWaterTicketCount = row[DailyReconciliationsTable.totalWaterTicketCount],
      totalQrCodeAmountCents = row[DailyReconciliationsTable.totalQrCodeAmountCents],
      totalOrderCount = row[DailyReconciliationsTable.totalOrderCount],
      totalOrderAmountCents = row[DailyReconciliationsTable.totalOrderAmountCents],
      totalCollectionAmountCents = row[DailyReconciliationsTable.totalCollectionAmountCents],
      discrepancyAmountCents = row[DailyReconciliationsTable.discrepancyAmountCents],
      status = DailyReconciliationStatus.valueOf(row[DailyReconciliationsTable.status]),
      reportData = row[DailyReconciliationsTable.reportData]?.let { parseJsonMap(it) },
      reviewedAt = row[DailyReconciliationsTable.reviewedAt],
      reviewedBy = row[DailyReconciliationsTable.reviewedBy],
      reviewNotes = row[DailyReconciliationsTable.reviewNotes],
      createdAt = row[DailyReconciliationsTable.createdAt],
      updatedAt = row[DailyReconciliationsTable.updatedAt],
    )

  /** report_data 文本 → 结构化 Map;按需剥掉历史上被双重编码的一层 JSON string(幂等,PG 下正常单层 JSON 不受影响)。 */
  private fun parseJsonMap(json: String): Map<String, Any> {
    val trimmed = json.trim()
    val effective =
      if (trimmed.startsWith("\"")) objectMapper.readValue(trimmed, String::class.java) else trimmed
    return objectMapper.readValue(
      effective,
      object : tools.jackson.core.type.TypeReference<Map<String, Any>>() {},
    )
  }

  private fun paged(
    extra: Op<Boolean>,
    pageable: Pageable,
  ): Page<DailyReconciliationModel> {
    val (content, total) =
      transaction(database) {
        val content =
          DailyReconciliationsTable
            .selectAll()
            .andWhere { extra }
            // 原 JPA 无序,id 升序破平防跨页重复
            .orderBy(DailyReconciliationsTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to DailyReconciliationsTable.selectAll().andWhere { extra }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findAll(pageable: Pageable): Page<DailyReconciliationModel> = paged(Op.TRUE, pageable)

  override fun findById(id: Long): Optional<DailyReconciliationModel> =
    Optional.ofNullable(
      transaction(database) {
        DailyReconciliationsTable
          .selectAll()
          .andWhere { DailyReconciliationsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: DailyReconciliationModel): DailyReconciliationModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val reportDataJson = entity.reportData?.let { objectMapper.writeValueAsString(it) }
      val exists =
        DailyReconciliationsTable
          .selectAllIncludingDeleted()
          .where { DailyReconciliationsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        DailyReconciliationsTable.update({ DailyReconciliationsTable.id eq id }) {
          it[reconciliationDate] = entity.reconciliationDate
          it[totalCashAmountCents] = entity.totalCashAmountCents
          it[totalWaterTicketCount] = entity.totalWaterTicketCount
          it[totalQrCodeAmountCents] = entity.totalQrCodeAmountCents
          it[totalOrderCount] = entity.totalOrderCount
          it[totalOrderAmountCents] = entity.totalOrderAmountCents
          it[totalCollectionAmountCents] = entity.totalCollectionAmountCents
          it[discrepancyAmountCents] = entity.discrepancyAmountCents
          it[status] = entity.status.name
          it[reportData] = reportDataJson
          it[reviewedAt] = entity.reviewedAt
          it[reviewedBy] = entity.reviewedBy
          it[reviewNotes] = entity.reviewNotes
          it[updatedAt] = now
        }
      } else {
        DailyReconciliationsTable.insert {
          it[DailyReconciliationsTable.id] = id
          it[reconciliationDate] = entity.reconciliationDate
          it[totalCashAmountCents] = entity.totalCashAmountCents
          it[totalWaterTicketCount] = entity.totalWaterTicketCount
          it[totalQrCodeAmountCents] = entity.totalQrCodeAmountCents
          it[totalOrderCount] = entity.totalOrderCount
          it[totalOrderAmountCents] = entity.totalOrderAmountCents
          it[totalCollectionAmountCents] = entity.totalCollectionAmountCents
          it[discrepancyAmountCents] = entity.discrepancyAmountCents
          it[status] = entity.status.name
          it[reportData] = reportDataJson
          it[reviewedAt] = entity.reviewedAt
          it[reviewedBy] = entity.reviewedBy
          it[reviewNotes] = entity.reviewNotes
          it[DailyReconciliationsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun deleteAll() {
    transaction(database) {
      // 对齐 JPA @SoftDelete 的 deleteAll 改写:物理行保留,仅置软删标记
      DailyReconciliationsTable.softDelete { Op.TRUE }
    }
  }

  override fun findByReconciliationDate(reconciliationDate: LocalDate): DailyReconciliationModel? =
    transaction(database) {
      DailyReconciliationsTable
        .selectAll()
        .andWhere { DailyReconciliationsTable.reconciliationDate eq reconciliationDate }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByReconciliationDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyReconciliationModel> =
    transaction(database) {
      DailyReconciliationsTable
        .selectAll()
        .andWhere {
          (DailyReconciliationsTable.reconciliationDate greaterEq startDate) and
            (DailyReconciliationsTable.reconciliationDate lessEq endDate)
        }.orderBy(DailyReconciliationsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
}
