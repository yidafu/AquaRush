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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.common.domain.table.DepositRecordsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 桶押金记录仓储实现(JPA→Exposed 迁移 P4,历史数据只读)。
 * 查询过滤 is_deleted;saveAll=批量 upsert(单事务原子)。
 */
@Repository
class DepositRecordRepositoryImpl(
  private val database: Database,
) : DepositRecordRepository {
  private fun toModel(row: ResultRow): DepositRecordModel =
    DepositRecordModel(
      id = row[DepositRecordsTable.id],
      orderId = row[DepositRecordsTable.orderId],
      userId = row[DepositRecordsTable.userId],
      depositAmountCents = row[DepositRecordsTable.depositAmountCents],
      status = DepositStatus.valueOf(row[DepositRecordsTable.status]),
      collectedAt = row[DepositRecordsTable.collectedAt],
      refundedAt = row[DepositRecordsTable.refundedAt],
      refundedBy = row[DepositRecordsTable.refundedBy],
      refundRemark = row[DepositRecordsTable.refundRemark],
      createdAt = row[DepositRecordsTable.createdAt],
      updatedAt = row[DepositRecordsTable.updatedAt],
      deletedAt = row[DepositRecordsTable.deletedAt],
      deletedBy = row[DepositRecordsTable.deletedBy],
    )

  override fun findByOrderIdAndStatus(
    orderId: Long,
    status: DepositStatus,
  ): List<DepositRecordModel> =
    transaction(database) {
      DepositRecordsTable
        .selectAll()
        .where {
          DepositRecordsTable.activeOnly {
            (DepositRecordsTable.orderId eq orderId) and (DepositRecordsTable.status eq status.name)
          }
        }.orderBy(DepositRecordsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun saveAll(entities: List<DepositRecordModel>): List<DepositRecordModel> =
    transaction(database) {
      entities.map { entity ->
        val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
        val now = LocalDateTime.now()
        val exists =
          DepositRecordsTable
            .selectAll()
            .where { DepositRecordsTable.id eq id }
            .limit(1)
            .any()
        if (exists) {
          DepositRecordsTable.update({ DepositRecordsTable.id eq id }) {
            it[orderId] = entity.orderId
            it[userId] = entity.userId
            it[depositAmountCents] = entity.depositAmountCents
            it[status] = entity.status.name
            it[collectedAt] = entity.collectedAt
            it[refundedAt] = entity.refundedAt
            it[refundedBy] = entity.refundedBy
            it[refundRemark] = entity.refundRemark
            it[updatedAt] = now
          }
        } else {
          DepositRecordsTable.insert {
            it[DepositRecordsTable.id] = id
            it[orderId] = entity.orderId
            it[userId] = entity.userId
            it[depositAmountCents] = entity.depositAmountCents
            it[status] = entity.status.name
            it[collectedAt] = entity.collectedAt
            it[refundedAt] = entity.refundedAt
            it[refundedBy] = entity.refundedBy
            it[refundRemark] = entity.refundRemark
            it[DepositRecordsTable.createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
          }
        }
        entity
      }
    }
}
