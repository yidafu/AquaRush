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

import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.table.TicketNoSequencesTable
import dev.yidafu.aqua.common.domain.table.UserTicketsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 用户水票实例仓库实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;核销/退回为原子条件 UPDATE(防并发重复核销,核销幂等 R-MKT-08);
 * 原子条件 UPDATE 的 WHERE 同样带 activeOnly(软删行不可参与核销;JPA 侧 bulk update
 * 不过滤软删为已知差异,业务上不存在软删的用户票)。
 * 票号取号([nextTicketSeq])用 Exposed upsert(PG=ON CONFLICT DO UPDATE / H2=MERGE),
 * upsert 与回读在同一 transaction(database) 内,行锁保证并发不双发。
 */
@Repository
class UserTicketRepositoryImpl(
  private val database: Database,
) : UserTicketRepository {
  private fun toModel(row: ResultRow): UserTicketModel =
    UserTicketModel(
      id = row[UserTicketsTable.id],
      userId = row[UserTicketsTable.userId],
      ticketTypeId = row[UserTicketsTable.ticketTypeId],
      purchaseId = row[UserTicketsTable.purchaseId],
      ticketNo = row[UserTicketsTable.ticketNo],
      status = UserTicketStatus.fromValue(row[UserTicketsTable.status]),
      expireTime = row[UserTicketsTable.expireTime],
      isGift = row[UserTicketsTable.isGift],
      usedOrderId = row[UserTicketsTable.usedOrderId],
      usedAt = row[UserTicketsTable.usedAt],
      returnedAt = row[UserTicketsTable.returnedAt],
      createdAt = row[UserTicketsTable.createdAt],
      updatedAt = row[UserTicketsTable.updatedAt],
      deletedAt = row[UserTicketsTable.deletedAt],
      deletedBy = row[UserTicketsTable.deletedBy],
    )

  /** 按日汇总投影行。 */
  private data class TicketUsageDayRow(
    private val day: LocalDate,
    private val redeemedCount: Long,
    private val returnedCount: Long,
    private val expiredCount: Long,
  ) : UserTicketRepository.TicketUsageDay {
    override fun getDay(): LocalDate = day

    override fun getRedeemedCount(): Long = redeemedCount

    override fun getReturnedCount(): Long = returnedCount

    override fun getExpiredCount(): Long = expiredCount
  }

  override fun findByUserId(userId: Long): List<UserTicketModel> =
    transaction(database) {
      UserTicketsTable
        .selectAll()
        .andWhere { UserTicketsTable.userId eq userId }
        .orderBy(UserTicketsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByUserIdAndStatus(
    userId: Long,
    status: UserTicketStatus,
  ): List<UserTicketModel> =
    transaction(database) {
      UserTicketsTable
        .selectAll()
        .andWhere { (UserTicketsTable.userId eq userId) and (UserTicketsTable.status eq status.name) }
        .orderBy(
          UserTicketsTable.id to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByUserIdAndStatusIn(
    userId: Long,
    statuses: List<UserTicketStatus>,
  ): List<UserTicketModel> {
    if (statuses.isEmpty()) return emptyList()
    return transaction(database) {
      UserTicketsTable
        .selectAll()
        .andWhere {
          (UserTicketsTable.userId eq userId) and
            (UserTicketsTable.status inList statuses.map { it.name })
        }.orderBy(UserTicketsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
    ticketTypeId: Long,
    statuses: List<UserTicketStatus>,
    expireTime: LocalDateTime,
  ): Long {
    if (statuses.isEmpty()) return 0
    return transaction(database) {
      UserTicketsTable
        .selectAll()
        .andWhere {
          (UserTicketsTable.ticketTypeId eq ticketTypeId) and
            (UserTicketsTable.status inList statuses.map { it.name }) and
            (UserTicketsTable.expireTime greater expireTime)
        }.count()
    }
  }

  override fun findById(id: Long): Optional<UserTicketModel> =
    Optional.ofNullable(
      transaction(database) {
        UserTicketsTable
          .selectAll()
          .andWhere { UserTicketsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: UserTicketModel): UserTicketModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        UserTicketsTable
          .selectAllIncludingDeleted()
          .where { UserTicketsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        UserTicketsTable.update({ UserTicketsTable.id eq id }) {
          it[ticketNo] = entity.ticketNo
          it[status] = entity.status.name
          it[expireTime] = entity.expireTime
          it[isGift] = entity.isGift
          it[usedOrderId] = entity.usedOrderId
          it[usedAt] = entity.usedAt
          it[returnedAt] = entity.returnedAt
          it[updatedAt] = now
        }
      } else {
        UserTicketsTable.insert {
          it[UserTicketsTable.id] = id
          it[userId] = entity.userId
          it[ticketTypeId] = entity.ticketTypeId
          it[purchaseId] = entity.purchaseId
          it[ticketNo] = entity.ticketNo
          it[status] = entity.status.name
          it[expireTime] = entity.expireTime
          it[isGift] = entity.isGift
          it[usedOrderId] = entity.usedOrderId
          it[usedAt] = entity.usedAt
          it[returnedAt] = entity.returnedAt
          it[UserTicketsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  // ===== UserTicketSequenceRepository =====

  override fun nextTicketSeq(monthKey: String): Long =
    transaction(database) {
      // 原子取号:冲突时 last_seq+1(取代原生 INSERT ... ON CONFLICT DO UPDATE ... RETURNING)。
      // upsert 立即执行且持有行锁,同一事务内回读读到本次取号值;并发事务在此阻塞至提交,
      // 不会双发;回滚时空号随事务一并回滚(语义对齐原单语句 RETURNING)。
      TicketNoSequencesTable.upsert(
        keys = arrayOf(TicketNoSequencesTable.monthKey),
        onUpdate = { it[TicketNoSequencesTable.lastSeq] = TicketNoSequencesTable.lastSeq + 1L },
      ) {
        it[TicketNoSequencesTable.monthKey] = monthKey
        it[TicketNoSequencesTable.lastSeq] = 1L
      }
      TicketNoSequencesTable
        .selectAll()
        .where { TicketNoSequencesTable.monthKey eq monthKey }
        .single()[TicketNoSequencesTable.lastSeq]
    }

  override fun findUsageGroupedByDay(
    ticketTypeId: Long,
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<UserTicketRepository.TicketUsageDay> {
    // 时间戳 = coalesce(used_at, returned_at, updated_at),与状态对应;[from, to) 左闭右开;
    // 按日聚合走原生 SQL(对齐原 QueryDSL coalesce/cast/sum-case,AddressRepositoryImpl 同款 exec 模式)
    val statuses = listOf("USED", "RETURNED", "EXPIRED").joinToString(",") { "'$it'" }
    val sql =
      """
      SELECT CAST(COALESCE(used_at, returned_at, updated_at) AS DATE) AS "day",
             SUM(CASE WHEN status = 'USED' THEN 1 ELSE 0 END) AS redeemed_count,
             SUM(CASE WHEN status = 'RETURNED' THEN 1 ELSE 0 END) AS returned_count,
             SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) AS expired_count
      FROM user_tickets
      WHERE ticket_type_id = ?
        AND status IN ($statuses)
        AND is_deleted = false
        AND COALESCE(used_at, returned_at, updated_at) >= ?
        AND COALESCE(used_at, returned_at, updated_at) < ?
      GROUP BY CAST(COALESCE(used_at, returned_at, updated_at) AS DATE)
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
        val result = mutableListOf<UserTicketRepository.TicketUsageDay>()
        while (rs.next()) {
          result +=
            TicketUsageDayRow(
              day = rs.getDate("day").toLocalDate(),
              redeemedCount = rs.getLong("redeemed_count"),
              returnedCount = rs.getLong("returned_count"),
              expiredCount = rs.getLong("expired_count"),
            )
        }
        result
      } ?: emptyList()
    }
  }

  override fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      UserTicketsTable.update({
        UserTicketsTable.activeOnly {
          (UserTicketsTable.id eq id) and
            (UserTicketsTable.userId eq userId) and
            (UserTicketsTable.status inList listOf(UserTicketStatus.UNUSED.name, UserTicketStatus.RETURNED.name)) and
            (UserTicketsTable.expireTime greater now)
        }
      }) {
        it[UserTicketsTable.status] = UserTicketStatus.USED.name
        it[usedOrderId] = orderId
        it[usedAt] = now
        it[updatedAt] = now
      }
    }

  override fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      // 与原 CASE WHEN 等价的两段式:两分支条件互补(未过期→RETURNED,剩余 USED→EXPIRED),
      // 同一事务内顺序执行,行集互斥不重叠
      val returned =
        UserTicketsTable.update({
          UserTicketsTable.activeOnly {
            (UserTicketsTable.usedOrderId eq orderId) and
              (UserTicketsTable.status eq UserTicketStatus.USED.name) and
              (UserTicketsTable.expireTime greater now)
          }
        }) {
          it[UserTicketsTable.status] = UserTicketStatus.RETURNED.name
          it[usedOrderId] = null
          it[usedAt] = null
          it[returnedAt] = now
          it[updatedAt] = now
        }
      val expired =
        UserTicketsTable.update({
          UserTicketsTable.activeOnly {
            (UserTicketsTable.usedOrderId eq orderId) and
              (UserTicketsTable.status eq UserTicketStatus.USED.name)
          }
        }) {
          it[UserTicketsTable.status] = UserTicketStatus.EXPIRED.name
          it[usedOrderId] = null
          it[usedAt] = null
          it[returnedAt] = now
          it[updatedAt] = now
        }
      returned + expired
    }
}
