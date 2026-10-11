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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.table.TicketPurchasesTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 购票单仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;支付入账为原子条件 UPDATE(幂等);
 * 按日销售聚合走原生 SQL(对齐原 QueryDSL coalesce/cast/date-group,AddressRepositoryImpl 同款 exec 模式)。
 */
@Repository
class TicketPurchaseRepositoryImpl(
  private val database: Database,
) : TicketPurchaseRepository {
  private fun toModel(row: ResultRow): TicketPurchaseModel =
    TicketPurchaseModel(
      id = row[TicketPurchasesTable.id],
      userId = row[TicketPurchasesTable.userId],
      ticketTypeId = row[TicketPurchasesTable.ticketTypeId],
      orderNo = row[TicketPurchasesTable.orderNo],
      packageCount = row[TicketPurchasesTable.packageCount],
      giftCount = row[TicketPurchasesTable.giftCount],
      totalCount = row[TicketPurchasesTable.totalCount],
      packagePriceCents = row[TicketPurchasesTable.packagePriceCents],
      paymentStatus = TicketPurchaseStatus.fromValue(row[TicketPurchasesTable.paymentStatus]),
      transactionId = row[TicketPurchasesTable.transactionId],
      paidAt = row[TicketPurchasesTable.paidAt],
      completedOrdersAtPurchase = row[TicketPurchasesTable.completedOrdersAtPurchase],
      createdAt = row[TicketPurchasesTable.createdAt],
      updatedAt = row[TicketPurchasesTable.updatedAt],
      deletedAt = row[TicketPurchasesTable.deletedAt],
      deletedBy = row[TicketPurchasesTable.deletedBy],
    )

  /** 按日汇总投影行。 */
  private data class TicketPaidSalesDayRow(
    private val day: LocalDate,
    private val purchaseCount: Long,
    private val ticketCount: Long,
    private val revenueCents: Long,
  ) : TicketPurchaseRepository.TicketPaidSalesDay {
    override fun getDay(): LocalDate = day

    override fun getPurchaseCount(): Long = purchaseCount

    override fun getTicketCount(): Long = ticketCount

    override fun getRevenueCents(): Long = revenueCents
  }

  override fun findByOrderNo(orderNo: String): TicketPurchaseModel? =
    transaction(database) {
      TicketPurchasesTable
        .selectAll()
        .andWhere { TicketPurchasesTable.orderNo eq orderNo }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun countByUserIdAndTicketTypeIdAndPaymentStatus(
    userId: Long,
    ticketTypeId: Long,
    paymentStatus: TicketPurchaseStatus,
  ): Long =
    transaction(database) {
      TicketPurchasesTable
        .selectAll()
        .andWhere {
          (TicketPurchasesTable.userId eq userId) and
            (TicketPurchasesTable.ticketTypeId eq ticketTypeId) and
            (TicketPurchasesTable.paymentStatus eq paymentStatus.name)
        }.count()
    }

  override fun findById(id: Long): Optional<TicketPurchaseModel> =
    Optional.ofNullable(
      transaction(database) {
        TicketPurchasesTable
          .selectAll()
          .andWhere { TicketPurchasesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByIds(ids: List<Long>): List<TicketPurchaseModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      TicketPurchasesTable
        .selectAll()
        .andWhere { TicketPurchasesTable.id inList ids }
        .orderBy(TicketPurchasesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun save(entity: TicketPurchaseModel): TicketPurchaseModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        TicketPurchasesTable
          .selectAllIncludingDeleted()
          .where { TicketPurchasesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        TicketPurchasesTable.update({ TicketPurchasesTable.id eq id }) {
          it[userId] = entity.userId
          it[ticketTypeId] = entity.ticketTypeId
          it[orderNo] = entity.orderNo
          it[packageCount] = entity.packageCount
          it[giftCount] = entity.giftCount
          it[totalCount] = entity.totalCount
          it[packagePriceCents] = entity.packagePriceCents
          it[paymentStatus] = entity.paymentStatus.name
          it[transactionId] = entity.transactionId
          it[paidAt] = entity.paidAt
          it[completedOrdersAtPurchase] = entity.completedOrdersAtPurchase
          it[updatedAt] = now
        }
      } else {
        TicketPurchasesTable.insert {
          it[TicketPurchasesTable.id] = id
          it[userId] = entity.userId
          it[ticketTypeId] = entity.ticketTypeId
          it[orderNo] = entity.orderNo
          it[packageCount] = entity.packageCount
          it[giftCount] = entity.giftCount
          it[totalCount] = entity.totalCount
          it[packagePriceCents] = entity.packagePriceCents
          it[paymentStatus] = entity.paymentStatus.name
          it[transactionId] = entity.transactionId
          it[paidAt] = entity.paidAt
          it[completedOrdersAtPurchase] = entity.completedOrdersAtPurchase
          it[TicketPurchasesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findPaidSalesGroupedByDay(
    ticketTypeId: Long,
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<TicketPurchaseRepository.TicketPaidSalesDay> {
    // 时间戳 = coalesce(paid_at, created_at),与原 JPQL 一致;[from, to) 左闭右开
    val sql =
      """
      SELECT CAST(COALESCE(paid_at, created_at) AS DATE) AS "day",
             COUNT(*) AS purchase_count,
             SUM(total_count) AS ticket_count,
             SUM(package_price_cents) AS revenue_cents
      FROM ticket_purchases
      WHERE ticket_type_id = ?
        AND payment_status = 'PAID'
        AND is_deleted = false
        AND COALESCE(paid_at, created_at) >= ?
        AND COALESCE(paid_at, created_at) < ?
      GROUP BY CAST(COALESCE(paid_at, created_at) AS DATE)
      ORDER BY "day"
      """.trimIndent()
    val args =
      listOf(
        LongColumnType() to ticketTypeId,
        JavaLocalDateTimeColumnType() to from,
        JavaLocalDateTimeColumnType() to to,
      )
    return transaction(database) {
      exec(sql, args) { rs ->
        val result = mutableListOf<TicketPurchaseRepository.TicketPaidSalesDay>()
        while (rs.next()) {
          result +=
            TicketPaidSalesDayRow(
              day = rs.getDate("day").toLocalDate(),
              purchaseCount = rs.getLong("purchase_count"),
              ticketCount = rs.getLong("ticket_count"),
              revenueCents = rs.getLong("revenue_cents"),
            )
        }
        result
      } ?: emptyList()
    }
  }

  override fun markPaidAtomically(
    id: Long,
    transactionId: String,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      TicketPurchasesTable.update({
        TicketPurchasesTable.activeOnly {
          (TicketPurchasesTable.id eq id) and
            (TicketPurchasesTable.paymentStatus eq TicketPurchaseStatus.PENDING.name)
        }
      }) {
        it[TicketPurchasesTable.paymentStatus] = TicketPurchaseStatus.PAID.name
        it[TicketPurchasesTable.transactionId] = transactionId
        it[paidAt] = now
        it[updatedAt] = now
      }
    }
}
