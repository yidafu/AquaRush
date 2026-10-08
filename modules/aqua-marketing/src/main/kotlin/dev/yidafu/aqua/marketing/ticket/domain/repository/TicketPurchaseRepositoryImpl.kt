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

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.DateTimeExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QTicketPurchaseModel.Companion.ticketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 购票单仓库 QueryDSL 实现。
 * 支付入账原子流转(PENDING→PAID,幂等)。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行。
 */
@Repository
class TicketPurchaseRepositoryImpl : TicketPurchaseRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  /**
   * 按日汇总已支付购票投影行(Projections.constructor 目标)。
   */
  class TicketPaidSalesDayRow(
    private val day: LocalDate,
    private val purchaseCount: Long,
    private val ticketCount: Long,
    private val revenueCents: Long,
  ) : TicketPurchaseRepositoryCustom.TicketPaidSalesDay {
    override fun getDay(): LocalDate = day

    override fun getPurchaseCount(): Long = purchaseCount

    override fun getTicketCount(): Long = ticketCount

    override fun getRevenueCents(): Long = revenueCents
  }

  override fun findPaidSalesGroupedByDay(
    ticketTypeId: Long,
    from: LocalDateTime,
    to: LocalDateTime,
  ): List<TicketPurchaseRepositoryCustom.TicketPaidSalesDay> {
    // 时间戳 = coalesce(paidAt, createdAt),与原 JPQL 一致
    val tsExpr: DateTimeExpression<LocalDateTime> =
      ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt)
    val dayExpr = Expressions.dateTemplate(LocalDate::class.java, "cast({0} as date)", tsExpr)

    return queryFactory
      .select(
        Projections.constructor(
          TicketPaidSalesDayRow::class.java,
          dayExpr,
          ticketPurchaseModel.id.count(),
          ticketPurchaseModel.totalCount.sumLong(),
          ticketPurchaseModel.packagePriceCents.sumLong(),
        ),
      ).from(ticketPurchaseModel)
      .where(
        ticketPurchaseModel.ticketTypeId.eq(ticketTypeId),
        ticketPurchaseModel.paymentStatus.eq(TicketPurchaseStatus.PAID),
        tsExpr.goe(from),
        tsExpr.lt(to),
      ).groupBy(dayExpr)
      .orderBy(dayExpr.asc())
      .fetch()
  }

  @Transactional
  override fun markPaidAtomically(
    id: Long,
    transactionId: String,
    now: LocalDateTime,
  ): Int =
    queryFactory
      .update(ticketPurchaseModel)
      .set(ticketPurchaseModel.paymentStatus, TicketPurchaseStatus.PAID)
      .set(ticketPurchaseModel.transactionId, transactionId)
      .set(ticketPurchaseModel.paidAt, now)
      .set(ticketPurchaseModel.updatedAt, now)
      .where(
        ticketPurchaseModel.id.eq(id),
        ticketPurchaseModel.paymentStatus.eq(TicketPurchaseStatus.PENDING),
      ).execute()
      .toInt()
}
