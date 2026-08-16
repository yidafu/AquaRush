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

package dev.yidafu.aqua.payment.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QPaymentRefundModel.Companion.paymentRefundModel
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 退款记录聚合查询实现(QueryDSL,H2(PostgreSQL 模式)/PostgreSQL 兼容)。
 * 口径 = status=COMPLETED 按退款成功时间(processed_at)归属窗口(@SoftDelete 自动过滤已删)。
 */
@Repository
class PaymentRefundRepositoryImpl : PaymentRefundRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun sumRefundedAmountByProcessedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long =
    queryFactory
      .select(paymentRefundModel.refundAmount.sumLong().coalesce(0L))
      .from(paymentRefundModel)
      .where(buildWhereClause(startDate, endDate))
      .fetchOne() ?: 0L

  override fun sumRefundedAmountGroupByDay(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Map<LocalDate, Long> {
    // 按退款成功时间(processed_at)逐日聚合(DATE_TRUNC('day') 返回 TIMESTAMP,查询侧转 LocalDate)
    val dayExpr = Expressions.dateTemplate(LocalDateTime::class.java, "DATE_TRUNC('day', {0})", paymentRefundModel.refundedAt)
    return queryFactory
      .select(dayExpr, paymentRefundModel.refundAmount.sumLong().coalesce(0L))
      .from(paymentRefundModel)
      .where(buildWhereClause(startDate, endDate))
      .groupBy(dayExpr)
      .fetch()
      .associate { row ->
        row.get(dayExpr)!!.toLocalDate() to (row.get(paymentRefundModel.refundAmount.sumLong().coalesce(0L)) ?: 0L)
      }
  }

  private fun buildWhereClause(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): BooleanBuilder =
    BooleanBuilder(
      paymentRefundModel.refundedAt
        .goe(startDate)
        .and(paymentRefundModel.refundedAt.loe(endDate))
        .and(paymentRefundModel.status.eq(RefundStatus.COMPLETED)),
    )
}
