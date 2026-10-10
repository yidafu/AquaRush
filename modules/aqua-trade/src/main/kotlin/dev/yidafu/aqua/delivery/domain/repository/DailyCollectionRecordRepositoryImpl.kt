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

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.table.DailyCollectionRecordsTable
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 每日收款记录仓储实现(JPA→Exposed 迁移 P4)。
 * 表为裸 Table 但带 is_deleted 列(类级 @SoftDelete)——每个查询显式过滤
 * `is_deleted = false`;deleteAll 为 `UPDATE is_deleted = true`(对齐 @SoftDelete 改写);
 * 分页 count 与 content 在同一个 [transaction] 内执行。
 */
@Repository
class DailyCollectionRecordRepositoryImpl(
  private val database: Database,
) : DailyCollectionRecordRepository {
  private fun toModel(row: ResultRow): DailyCollectionRecordModel =
    DailyCollectionRecordModel(
      id = row[DailyCollectionRecordsTable.id],
      deliveryWorkerId = row[DailyCollectionRecordsTable.deliveryWorkerId],
      collectionDate = row[DailyCollectionRecordsTable.collectionDate],
      cashAmountCents = row[DailyCollectionRecordsTable.cashAmountCents],
      waterTicketCount = row[DailyCollectionRecordsTable.waterTicketCount],
      qrCodeAmountCents = row[DailyCollectionRecordsTable.qrCodeAmountCents],
      orderCount = row[DailyCollectionRecordsTable.orderCount] ?: 0,
      orderAmountCents = row[DailyCollectionRecordsTable.orderAmountCents] ?: 0L,
      differenceAmountCents = row[DailyCollectionRecordsTable.differenceAmountCents] ?: 0L,
      status = DailyCollectionStatus.valueOf(row[DailyCollectionRecordsTable.status]),
      confirmedAt = row[DailyCollectionRecordsTable.confirmedAt],
      confirmedBy = row[DailyCollectionRecordsTable.confirmedBy],
      notes = row[DailyCollectionRecordsTable.notes],
      calculatedCashAmountCents = row[DailyCollectionRecordsTable.calculatedCashAmountCents],
      calculatedQrCodeAmountCents = row[DailyCollectionRecordsTable.calculatedQrCodeAmountCents],
      calculatedWaterTicketCount = row[DailyCollectionRecordsTable.calculatedWaterTicketCount],
      calculatedOrderAmountCents = row[DailyCollectionRecordsTable.calculatedOrderAmountCents],
      rejectedAt = row[DailyCollectionRecordsTable.rejectedAt],
      rejectedBy = row[DailyCollectionRecordsTable.rejectedBy],
      rejectReason = row[DailyCollectionRecordsTable.rejectReason],
      createdAt = row[DailyCollectionRecordsTable.createdAt],
      updatedAt = row[DailyCollectionRecordsTable.updatedAt],
    )

  /** 软删过滤条件:`is_deleted = false AND extra`。 */
  private fun activeWhere(extra: Op<Boolean> = Op.TRUE): Op<Boolean> =
    DailyCollectionRecordsTable.isDeleted.eq(false).let { if (extra == Op.TRUE) it else it and extra }

  private fun paged(
    extra: Op<Boolean>,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel> {
    val (content, total) =
      transaction(database) {
        val content =
          DailyCollectionRecordsTable
            .selectAll()
            .where { activeWhere(extra) }
            // 原 JPA 无序,id 升序破平防跨页重复
            .orderBy(DailyCollectionRecordsTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to DailyCollectionRecordsTable.selectAll().where { activeWhere(extra) }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findAll(pageable: Pageable): Page<DailyCollectionRecordModel> = paged(Op.TRUE, pageable)

  override fun findById(id: Long): Optional<DailyCollectionRecordModel> =
    Optional.ofNullable(
      transaction(database) {
        DailyCollectionRecordsTable
          .selectAll()
          .where { activeWhere(DailyCollectionRecordsTable.id eq id) }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: DailyCollectionRecordModel): DailyCollectionRecordModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        DailyCollectionRecordsTable
          .selectAll()
          .where { DailyCollectionRecordsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        DailyCollectionRecordsTable.update({ DailyCollectionRecordsTable.id eq id }) {
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[collectionDate] = entity.collectionDate
          it[cashAmountCents] = entity.cashAmountCents
          it[waterTicketCount] = entity.waterTicketCount
          it[qrCodeAmountCents] = entity.qrCodeAmountCents
          it[orderCount] = entity.orderCount
          it[orderAmountCents] = entity.orderAmountCents
          it[differenceAmountCents] = entity.differenceAmountCents
          it[calculatedCashAmountCents] = entity.calculatedCashAmountCents
          it[calculatedQrCodeAmountCents] = entity.calculatedQrCodeAmountCents
          it[calculatedWaterTicketCount] = entity.calculatedWaterTicketCount
          it[calculatedOrderAmountCents] = entity.calculatedOrderAmountCents
          it[status] = entity.status.name
          it[confirmedAt] = entity.confirmedAt
          it[confirmedBy] = entity.confirmedBy
          it[notes] = entity.notes
          it[rejectedAt] = entity.rejectedAt
          it[rejectedBy] = entity.rejectedBy
          it[rejectReason] = entity.rejectReason
          it[updatedAt] = now
        }
      } else {
        DailyCollectionRecordsTable.insert {
          it[DailyCollectionRecordsTable.id] = id
          it[deliveryWorkerId] = entity.deliveryWorkerId
          it[collectionDate] = entity.collectionDate
          it[cashAmountCents] = entity.cashAmountCents
          it[waterTicketCount] = entity.waterTicketCount
          it[qrCodeAmountCents] = entity.qrCodeAmountCents
          it[orderCount] = entity.orderCount
          it[orderAmountCents] = entity.orderAmountCents
          it[differenceAmountCents] = entity.differenceAmountCents
          it[calculatedCashAmountCents] = entity.calculatedCashAmountCents
          it[calculatedQrCodeAmountCents] = entity.calculatedQrCodeAmountCents
          it[calculatedWaterTicketCount] = entity.calculatedWaterTicketCount
          it[calculatedOrderAmountCents] = entity.calculatedOrderAmountCents
          it[status] = entity.status.name
          it[confirmedAt] = entity.confirmedAt
          it[confirmedBy] = entity.confirmedBy
          it[notes] = entity.notes
          it[rejectedAt] = entity.rejectedAt
          it[rejectedBy] = entity.rejectedBy
          it[rejectReason] = entity.rejectReason
          it[DailyCollectionRecordsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun deleteAll() {
    transaction(database) {
      // 对齐 JPA @SoftDelete 的 deleteAll 改写:物理行保留,仅置软删标记
      DailyCollectionRecordsTable.update({ DailyCollectionRecordsTable.isDeleted eq false }) {
        it[isDeleted] = true
      }
    }
  }

  override fun findByCollectionDate(collectionDate: LocalDate): List<DailyCollectionRecordModel> =
    transaction(database) {
      DailyCollectionRecordsTable
        .selectAll()
        .where { activeWhere(DailyCollectionRecordsTable.collectionDate eq collectionDate) }
        // 原 JPA 无序,固定 id 升序取稳定插入序
        .orderBy(DailyCollectionRecordsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByCollectionDate(
    collectionDate: LocalDate,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel> = paged(DailyCollectionRecordsTable.collectionDate eq collectionDate, pageable)

  override fun findByCollectionDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyCollectionRecordModel> =
    transaction(database) {
      DailyCollectionRecordsTable
        .selectAll()
        .where {
          activeWhere(
            (DailyCollectionRecordsTable.collectionDate greaterEq startDate) and
              (DailyCollectionRecordsTable.collectionDate lessEq endDate),
          )
        }.orderBy(DailyCollectionRecordsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByDeliveryWorkerId(
    deliveryWorkerId: Long,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel> = paged(DailyCollectionRecordsTable.deliveryWorkerId eq deliveryWorkerId, pageable)

  override fun findByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
  ): DailyCollectionRecordModel? =
    transaction(database) {
      DailyCollectionRecordsTable
        .selectAll()
        .where {
          activeWhere(
            (DailyCollectionRecordsTable.deliveryWorkerId eq deliveryWorkerId) and
              (DailyCollectionRecordsTable.collectionDate eq collectionDate),
          )
        }.limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByDeliveryWorkerIdAndCollectionDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
    pageable: Pageable,
  ): Page<DailyCollectionRecordModel> =
    paged(
      (DailyCollectionRecordsTable.deliveryWorkerId eq deliveryWorkerId) and
        (DailyCollectionRecordsTable.collectionDate eq collectionDate),
      pageable,
    )
}
