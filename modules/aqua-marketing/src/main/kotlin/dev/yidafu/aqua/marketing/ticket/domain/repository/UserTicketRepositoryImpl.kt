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

import com.querydsl.core.types.Ops
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.DateTimeExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QUserTicketModel.Companion.userTicketModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 用户水票实例仓库 QueryDSL 实现。
 * 核销/退回用原子条件 UPDATE(防并发重复核销,核销幂等 R-MKT-08)。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行。
 */
@Repository
class UserTicketRepositoryImpl : UserTicketRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  /**
   * 按日汇总使用情况投影行(Projections.constructor 目标)。
   */
  class TicketUsageDayRow(
    private val day: LocalDate,
    private val redeemedCount: Long,
    private val returnedCount: Long,
    private val expiredCount: Long,
  ) : UserTicketRepositoryCustom.TicketUsageDay {
    override fun getDay(): LocalDate = day

    override fun getRedeemedCount(): Long = redeemedCount

    override fun getReturnedCount(): Long = returnedCount

    override fun getExpiredCount(): Long = expiredCount
  }

  override fun findUsageGroupedByDay(
    ticketTypeId: Long,
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<UserTicketRepositoryCustom.TicketUsageDay> {
    // 时间戳 = coalesce(usedAt, returnedAt, updatedAt),与状态对应
    val tsExpr: DateTimeExpression<LocalDateTime> =
      userTicketModel.usedAt.coalesce(userTicketModel.returnedAt, userTicketModel.updatedAt)
    val dayExpr = Expressions.dateTemplate(LocalDate::class.java, "cast({0} as date)", tsExpr)
    val redeemedCount: NumberExpression<Long> = statusCountSum(UserTicketStatus.USED)
    val returnedCount: NumberExpression<Long> = statusCountSum(UserTicketStatus.RETURNED)
    val expiredCount: NumberExpression<Long> = statusCountSum(UserTicketStatus.EXPIRED)

    return queryFactory
      .select(
        Projections.constructor(
          TicketUsageDayRow::class.java,
          dayExpr,
          redeemedCount,
          returnedCount,
          expiredCount,
        ),
      ).from(userTicketModel)
      .where(
        userTicketModel.ticketTypeId.eq(ticketTypeId),
        userTicketModel.status.`in`(
          UserTicketStatus.USED,
          UserTicketStatus.RETURNED,
          UserTicketStatus.EXPIRED,
        ),
        tsExpr.goe(from),
        tsExpr.lt(to),
      ).groupBy(dayExpr)
      .orderBy(dayExpr.asc())
      .fetch()
  }

  /**
   * sum(case when status = [status] then 1 else 0 end)
   */
  private fun statusCountSum(status: UserTicketStatus): NumberExpression<Long> =
    Expressions
      .numberOperation(
        Long::class.javaObjectType,
        Ops.AggOps.SUM_AGG,
        Expressions
          .cases()
          .`when`(userTicketModel.status.eq(status))
          .then(1L)
          .otherwise(0L),
      )

  @Transactional
  override fun redeemAtomically(
    id: Long,
    userId: Long,
    orderId: Long,
    now: LocalDateTime,
  ): Int =
    queryFactory
      .update(userTicketModel)
      .set(userTicketModel.status, UserTicketStatus.USED)
      .set(userTicketModel.usedOrderId, orderId)
      .set(userTicketModel.usedAt, now)
      .set(userTicketModel.updatedAt, now)
      .where(
        userTicketModel.id.eq(id),
        userTicketModel.userId.eq(userId),
        userTicketModel.status.`in`(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED),
        userTicketModel.expireTime.gt(now),
      ).execute()
      .toInt()

  @Transactional
  override fun returnByOrder(
    orderId: Long,
    now: LocalDateTime,
  ): Int =
    queryFactory
      .update(userTicketModel)
      .set(
        userTicketModel.status,
        Expressions
          .cases()
          .`when`(userTicketModel.expireTime.gt(now))
          .then(UserTicketStatus.RETURNED)
          .otherwise(UserTicketStatus.EXPIRED),
      ).setNull(userTicketModel.usedOrderId)
      .setNull(userTicketModel.usedAt)
      .set(userTicketModel.returnedAt, now)
      .set(userTicketModel.updatedAt, now)
      .where(
        userTicketModel.usedOrderId.eq(orderId),
        userTicketModel.status.eq(UserTicketStatus.USED),
      ).execute()
      .toInt()
}
