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
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
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
import java.time.LocalDateTime

/**
 * 水桶押金记录仓储实现(R-MON-03)。
 * 查询过滤 is_deleted;状态流转 markHeldIfPending/markRefundedIfHeld 为状态谓词条件更新,
 * 受影响行数=0 即已被并发流转(幂等失败),退押防重复退还。
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
      specification = row[DepositRecordsTable.specification],
      unitDepositCents = row[DepositRecordsTable.unitDepositCents],
      bucketCount = row[DepositRecordsTable.bucketCount],
      status = DepositStatus.fromValue(row[DepositRecordsTable.status]),
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
        .andWhere { (DepositRecordsTable.orderId eq orderId) and (DepositRecordsTable.status eq status.name) }
        .orderBy(
          DepositRecordsTable.id to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByUserIdAndSpecification(
    userId: Long,
    specification: String,
  ): List<DepositRecordModel> =
    transaction(database) {
      DepositRecordsTable
        .selectAll()
        .andWhere {
          (DepositRecordsTable.userId eq userId) and
            (DepositRecordsTable.specification eq specification)
        }.orderBy(DepositRecordsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun pageByUserId(
    userId: Long,
    status: DepositStatus?,
    pageable: Pageable,
  ): Page<DepositRecordModel> =
    transaction(database) {
      val condition =
        status?.let {
          (DepositRecordsTable.userId eq userId) and (DepositRecordsTable.status eq it.name)
        } ?: (DepositRecordsTable.userId eq userId)
      val content =
        DepositRecordsTable
          .selectAll()
          .andWhere { condition }
          .orderBy(DepositRecordsTable.createdAt to SortOrder.DESC, DepositRecordsTable.id to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      PageImpl(content, pageable, DepositRecordsTable.selectAll().andWhere { condition }.count())
    }

  override fun search(
    userId: Long?,
    status: DepositStatus?,
    specification: String?,
    pageable: Pageable,
  ): Page<DepositRecordModel> =
    transaction(database) {
      var condition: Op<Boolean> = Op.TRUE
      userId?.let { condition = condition and (DepositRecordsTable.userId eq it) }
      status?.let { condition = condition and (DepositRecordsTable.status eq it.name) }
      specification?.takeIf { it.isNotBlank() }?.let {
        condition = condition and (DepositRecordsTable.specification eq it)
      }
      val content =
        DepositRecordsTable
          .selectAll()
          .andWhere { condition }
          .orderBy(DepositRecordsTable.createdAt to SortOrder.DESC, DepositRecordsTable.id to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      PageImpl(content, pageable, DepositRecordsTable.selectAll().andWhere { condition }.count())
    }

  override fun markHeldIfPending(ids: List<Long>): Int {
    if (ids.isEmpty()) return 0
    val now = LocalDateTime.now()
    return transaction(database) {
      DepositRecordsTable.update(
        {
          DepositRecordsTable.id inList ids and
            (DepositRecordsTable.status eq DepositStatus.PENDING.name)
        },
      ) {
        it[status] = DepositStatus.HELD.name
        it[collectedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun markRefundedIfHeld(
    ids: List<Long>,
    refundedBy: Long?,
    remark: String?,
  ): Int {
    if (ids.isEmpty()) return 0
    val now = LocalDateTime.now()
    return transaction(database) {
      DepositRecordsTable.update(
        {
          DepositRecordsTable.id inList ids and
            (DepositRecordsTable.status eq DepositStatus.HELD.name)
        },
      ) {
        it[status] = DepositStatus.REFUNDED.name
        it[refundedAt] = now
        it[DepositRecordsTable.refundedBy] = refundedBy
        it[refundRemark] = remark
        it[updatedAt] = now
      }
    }
  }

  override fun softDeletePendingByOrderId(orderId: Long): Int =
    transaction(database) {
      DepositRecordsTable.softDelete {
        (DepositRecordsTable.orderId eq orderId) and
          (DepositRecordsTable.status eq DepositStatus.PENDING.name)
      }
    }

  override fun saveAll(entities: List<DepositRecordModel>): List<DepositRecordModel> =
    transaction(database) {
      entities.map { entity ->
        val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
        val now = LocalDateTime.now()
        val exists =
          DepositRecordsTable
            .selectAllIncludingDeleted()
            .where { DepositRecordsTable.id eq id }
            .limit(1)
            .any()
        if (exists) {
          DepositRecordsTable.update({ DepositRecordsTable.id eq id }) {
            it[orderId] = entity.orderId
            it[userId] = entity.userId
            it[depositAmountCents] = entity.depositAmountCents
            it[specification] = entity.specification
            it[unitDepositCents] = entity.unitDepositCents
            it[bucketCount] = entity.bucketCount
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
            it[specification] = entity.specification
            it[unitDepositCents] = entity.unitDepositCents
            it[bucketCount] = entity.bucketCount
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
