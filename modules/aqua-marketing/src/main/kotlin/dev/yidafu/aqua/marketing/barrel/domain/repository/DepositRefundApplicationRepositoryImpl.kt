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

import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.domain.table.DepositRefundApplicationsTable
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
 * 退押申请仓储实现(R-PRO-04 状态机,状态谓词条件更新防并发重复流转)。
 */
@Repository
class DepositRefundApplicationRepositoryImpl(
  private val database: Database,
) : DepositRefundApplicationRepository {
  private fun toModel(row: ResultRow): DepositRefundApplicationModel =
    DepositRefundApplicationModel(
      id = row[DepositRefundApplicationsTable.id],
      userId = row[DepositRefundApplicationsTable.userId],
      specification = row[DepositRefundApplicationsTable.specification],
      bucketCount = row[DepositRefundApplicationsTable.bucketCount],
      depositCents = row[DepositRefundApplicationsTable.depositCents],
      receiverName = row[DepositRefundApplicationsTable.receiverName],
      receiverPhone = row[DepositRefundApplicationsTable.receiverPhone],
      receiverDetail = row[DepositRefundApplicationsTable.receiverDetail],
      remark = row[DepositRefundApplicationsTable.remark],
      status = DepositRefundApplicationStatus.fromValue(row[DepositRefundApplicationsTable.status]),
      recoveredAt = row[DepositRefundApplicationsTable.recoveredAt],
      refundedAt = row[DepositRefundApplicationsTable.refundedAt],
      rejectedAt = row[DepositRefundApplicationsTable.rejectedAt],
      handlerAdminId = row[DepositRefundApplicationsTable.handlerAdminId],
      rejectReason = row[DepositRefundApplicationsTable.rejectReason],
      createdAt = row[DepositRefundApplicationsTable.createdAt],
      updatedAt = row[DepositRefundApplicationsTable.updatedAt],
      deletedAt = row[DepositRefundApplicationsTable.deletedAt],
      deletedBy = row[DepositRefundApplicationsTable.deletedBy],
    )

  override fun findById(id: Long): DepositRefundApplicationModel? =
    transaction(database) {
      DepositRefundApplicationsTable
        .selectAll()
        .andWhere { DepositRefundApplicationsTable.id eq id }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByIdAndUserId(
    id: Long,
    userId: Long,
  ): DepositRefundApplicationModel? =
    transaction(database) {
      DepositRefundApplicationsTable
        .selectAll()
        .andWhere {
          (DepositRefundApplicationsTable.id eq id) and
            (DepositRefundApplicationsTable.userId eq userId)
        }.limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findActiveByUserAndSpec(
    userId: Long,
    specification: String,
  ): DepositRefundApplicationModel? =
    transaction(database) {
      DepositRefundApplicationsTable
        .selectAll()
        .andWhere {
          (DepositRefundApplicationsTable.userId eq userId) and
            (DepositRefundApplicationsTable.specification eq specification) and
            (
              DepositRefundApplicationsTable.status inList
                listOf(
                  DepositRefundApplicationStatus.PENDING.name,
                  DepositRefundApplicationStatus.RECOVERED.name,
                )
            )
        }.orderBy(DepositRefundApplicationsTable.createdAt to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun search(
    userId: Long?,
    status: DepositRefundApplicationStatus?,
    pageable: Pageable,
  ): Page<DepositRefundApplicationModel> =
    transaction(database) {
      var condition: Op<Boolean> = Op.TRUE
      userId?.let { condition = condition and (DepositRefundApplicationsTable.userId eq it) }
      status?.let { condition = condition and (DepositRefundApplicationsTable.status eq it.name) }
      val content =
        DepositRefundApplicationsTable
          .selectAll()
          .andWhere { condition }
          .orderBy(
            DepositRefundApplicationsTable.createdAt to SortOrder.DESC,
            DepositRefundApplicationsTable.id to SortOrder.DESC,
          ).offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      PageImpl(content, pageable, DepositRefundApplicationsTable.selectAll().andWhere { condition }.count())
    }

  override fun save(entity: DepositRefundApplicationModel): DepositRefundApplicationModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        DepositRefundApplicationsTable
          .selectAllIncludingDeleted()
          .where { DepositRefundApplicationsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        DepositRefundApplicationsTable.update({ DepositRefundApplicationsTable.id eq id }) {
          it[userId] = entity.userId
          it[specification] = entity.specification
          it[bucketCount] = entity.bucketCount
          it[depositCents] = entity.depositCents
          it[receiverName] = entity.receiverName
          it[receiverPhone] = entity.receiverPhone
          it[receiverDetail] = entity.receiverDetail
          it[remark] = entity.remark
          it[status] = entity.status.name
          it[recoveredAt] = entity.recoveredAt
          it[refundedAt] = entity.refundedAt
          it[rejectedAt] = entity.rejectedAt
          it[handlerAdminId] = entity.handlerAdminId
          it[rejectReason] = entity.rejectReason
          it[updatedAt] = now
        }
      } else {
        DepositRefundApplicationsTable.insert {
          it[DepositRefundApplicationsTable.id] = id
          it[userId] = entity.userId
          it[specification] = entity.specification
          it[bucketCount] = entity.bucketCount
          it[depositCents] = entity.depositCents
          it[receiverName] = entity.receiverName
          it[receiverPhone] = entity.receiverPhone
          it[receiverDetail] = entity.receiverDetail
          it[remark] = entity.remark
          it[status] = entity.status.name
          it[recoveredAt] = entity.recoveredAt
          it[refundedAt] = entity.refundedAt
          it[rejectedAt] = entity.rejectedAt
          it[handlerAdminId] = entity.handlerAdminId
          it[rejectReason] = entity.rejectReason
          it[DepositRefundApplicationsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun transition(
    id: Long,
    fromStatuses: List<DepositRefundApplicationStatus>,
    to: DepositRefundApplicationStatus,
    handlerAdminId: Long?,
    rejectReason: String?,
  ): Boolean {
    if (fromStatuses.isEmpty()) return false
    val now = LocalDateTime.now()
    return transaction(database) {
      val affected =
        DepositRefundApplicationsTable.update(
          {
            (DepositRefundApplicationsTable.id eq id) and
              (DepositRefundApplicationsTable.status inList fromStatuses.map { it.name })
          },
        ) {
          it[status] = to.name
          it[DepositRefundApplicationsTable.handlerAdminId] = handlerAdminId
          when (to) {
            DepositRefundApplicationStatus.RECOVERED -> it[recoveredAt] = now
            DepositRefundApplicationStatus.REFUNDED -> it[refundedAt] = now
            DepositRefundApplicationStatus.REJECTED -> {
              it[rejectedAt] = now
              it[DepositRefundApplicationsTable.rejectReason] = rejectReason
            }
            DepositRefundApplicationStatus.PENDING -> Unit
          }
          it[updatedAt] = now
        }
      affected > 0
    }
  }
}
