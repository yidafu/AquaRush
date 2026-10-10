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

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.domain.table.BarrelPurchasesTable
import dev.yidafu.aqua.common.domain.table.activeOnly
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
import java.time.LocalDateTime
import java.util.Optional

/**
 * 水桶购买单仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入;支付入账/取消为原子条件 UPDATE(幂等)。
 */
@Repository
class BarrelPurchaseRepositoryImpl(
  private val database: Database,
) : BarrelPurchaseRepository {
  private fun toModel(row: ResultRow): BarrelPurchaseModel =
    BarrelPurchaseModel(
      id = row[BarrelPurchasesTable.id],
      userId = row[BarrelPurchasesTable.userId],
      orderNo = row[BarrelPurchasesTable.orderNo],
      totalAmountCents = row[BarrelPurchasesTable.totalAmountCents],
      paymentStatus = BarrelPaymentStatus.valueOf(row[BarrelPurchasesTable.paymentStatus]),
      transactionId = row[BarrelPurchasesTable.transactionId],
      paidAt = row[BarrelPurchasesTable.paidAt],
      refundedAt = row[BarrelPurchasesTable.refundedAt],
      refundedBy = row[BarrelPurchasesTable.refundedBy],
      refundRemark = row[BarrelPurchasesTable.refundRemark],
      remark = row[BarrelPurchasesTable.remark],
      createdAt = row[BarrelPurchasesTable.createdAt],
      updatedAt = row[BarrelPurchasesTable.updatedAt],
      deletedAt = row[BarrelPurchasesTable.deletedAt],
      deletedBy = row[BarrelPurchasesTable.deletedBy],
    )

  override fun findByOrderNo(orderNo: String): BarrelPurchaseModel? =
    transaction(database) {
      BarrelPurchasesTable
        .selectAll()
        .andWhere { BarrelPurchasesTable.orderNo eq orderNo }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): BarrelPurchaseModel? =
    transaction(database) {
      BarrelPurchasesTable
        .selectAll()
        .andWhere { BarrelPurchasesTable.userId eq userId }
        .orderBy(BarrelPurchasesTable.createdAt to SortOrder.DESC, BarrelPurchasesTable.id to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsByUserIdAndPaymentStatus(
    userId: Long,
    paymentStatus: BarrelPaymentStatus,
  ): Boolean =
    transaction(database) {
      BarrelPurchasesTable
        .selectAll()
        .andWhere { (BarrelPurchasesTable.userId eq userId) and (BarrelPurchasesTable.paymentStatus eq paymentStatus.name) }
        .limit(1)
        .any()
    }

  override fun findById(id: Long): Optional<BarrelPurchaseModel> =
    Optional.ofNullable(
      transaction(database) {
        BarrelPurchasesTable
          .selectAll()
          .andWhere { BarrelPurchasesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun searchPurchases(
    status: BarrelPaymentStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    pageable: Pageable,
  ): Page<BarrelPurchaseModel> {
    var condition: Op<Boolean> = Op.TRUE
    status?.let { condition = condition and (BarrelPurchasesTable.paymentStatus eq it.name) }
    dateFrom?.let { condition = condition and (BarrelPurchasesTable.paidAt greaterEq it) }
    dateTo?.let { condition = condition and (BarrelPurchasesTable.paidAt lessEq it) }
    val (content, total) =
      transaction(database) {
        val content =
          BarrelPurchasesTable
            .selectAll()
            .andWhere { condition }
            // 固定 createdAt 降序(对齐原 PageRequest Sort),id 破平防跨页重复
            .orderBy(BarrelPurchasesTable.createdAt to SortOrder.DESC, BarrelPurchasesTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to BarrelPurchasesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: BarrelPurchaseModel): BarrelPurchaseModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        BarrelPurchasesTable
          .selectAllIncludingDeleted()
          .where { BarrelPurchasesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        BarrelPurchasesTable.update({ BarrelPurchasesTable.id eq id }) {
          it[userId] = entity.userId
          it[orderNo] = entity.orderNo
          it[totalAmountCents] = entity.totalAmountCents
          it[paymentStatus] = entity.paymentStatus.name
          it[transactionId] = entity.transactionId
          it[paidAt] = entity.paidAt
          it[refundedAt] = entity.refundedAt
          it[refundedBy] = entity.refundedBy
          it[refundRemark] = entity.refundRemark
          it[remark] = entity.remark
          it[updatedAt] = now
        }
      } else {
        BarrelPurchasesTable.insert {
          it[BarrelPurchasesTable.id] = id
          it[userId] = entity.userId
          it[orderNo] = entity.orderNo
          it[totalAmountCents] = entity.totalAmountCents
          it[paymentStatus] = entity.paymentStatus.name
          it[transactionId] = entity.transactionId
          it[paidAt] = entity.paidAt
          it[refundedAt] = entity.refundedAt
          it[refundedBy] = entity.refundedBy
          it[refundRemark] = entity.refundRemark
          it[remark] = entity.remark
          it[BarrelPurchasesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun markPaidAtomically(
    id: Long,
    transactionId: String,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      BarrelPurchasesTable.update({
        BarrelPurchasesTable.activeOnly {
          (BarrelPurchasesTable.id eq id) and
            (BarrelPurchasesTable.paymentStatus eq BarrelPaymentStatus.PENDING.name)
        }
      }) {
        it[BarrelPurchasesTable.paymentStatus] = BarrelPaymentStatus.PAID.name
        it[BarrelPurchasesTable.transactionId] = transactionId
        it[paidAt] = now
        it[updatedAt] = now
      }
    }

  override fun cancelPendingByUser(
    userId: Long,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      BarrelPurchasesTable.update({
        BarrelPurchasesTable.activeOnly {
          (BarrelPurchasesTable.userId eq userId) and
            (BarrelPurchasesTable.paymentStatus eq BarrelPaymentStatus.PENDING.name)
        }
      }) {
        it[BarrelPurchasesTable.paymentStatus] = BarrelPaymentStatus.CANCELLED.name
        it[updatedAt] = now
      }
    }
}
