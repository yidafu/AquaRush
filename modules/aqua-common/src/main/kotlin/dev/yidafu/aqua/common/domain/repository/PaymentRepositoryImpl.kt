/*
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

package dev.yidafu.aqua.common.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.QPaymentModel.paymentModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Enhanced PaymentRepository implementation using QueryDSL
 */
@Repository
class PaymentRepositoryImpl : PaymentRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findByUserIdAndStatusEnhanced(
    userId: Long,
    status: PaymentStatus,
  ): List<PaymentModel> {
    return queryFactory.selectFrom(paymentModel)
      .where(
        paymentModel.userId.eq(userId)
          .and(paymentModel.status.eq(status))
      )
      .fetch()
  }

  override fun findExpiredPaymentsEnhanced(now: LocalDateTime): List<PaymentModel> {
    return queryFactory.selectFrom(paymentModel)
      .where(
        paymentModel.status.eq(PaymentStatus.PENDING)
          .and(paymentModel.expiredAt.lt(now))
      )
      .fetch()
  }

  override fun findByCreatedAtBetweenEnhanced(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel> {
    return queryFactory.selectFrom(paymentModel)
      .where(paymentModel.createdAt.between(startDate, endDate))
      .fetch()
  }

  override fun countByStatusAndCreatedAtBetweenEnhanced(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    return queryFactory.query()
      .from(paymentModel)
      .where(
        paymentModel.status.eq(status)
          .and(paymentModel.createdAt.between(startDate, endDate))
      )
      .fetchCount()
  }

  override fun sumAmountByStatusAndCreatedAtBetweenEnhanced(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    return queryFactory.query()
      .from(paymentModel)
      .where(
        paymentModel.status.eq(status)
          .and(paymentModel.createdAt.between(startDate, endDate))
      )
      .select(paymentModel.amount.sum())
      .fetchOne() ?: 0L
  }

  override fun findPaymentsWithFilters(
    userId: Long?,
    status: PaymentStatus?,
    transactionId: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    minAmount: Long?,
    maxAmount: Long?,
  ): List<PaymentModel> {
    val builder = BooleanBuilder()

    userId?.let { builder.and(paymentModel.userId.eq(it)) }
    status?.let { builder.and(paymentModel.status.eq(it)) }
    transactionId?.let { builder.and(paymentModel.transactionId.eq(it)) }
    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(paymentModel.createdAt.between(start, end))
      }
    }
    minAmount?.let { builder.and(paymentModel.amount.goe(it)) }
    maxAmount?.let { builder.and(paymentModel.amount.loe(it)) }

    return queryFactory.selectFrom(paymentModel)
      .where(builder)
      .orderBy(paymentModel.createdAt.desc())
      .fetch()
  }
}
