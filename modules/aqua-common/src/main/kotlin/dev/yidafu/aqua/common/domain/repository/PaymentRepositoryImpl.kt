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

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.utils.MoneyUtils
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Enhanced PaymentRepository implementation with modern Spring Data JPA 3.0+ features
 * This implements PaymentRepositoryCustom for custom query methods
 */
@Repository
class PaymentRepositoryImpl(
  private val entityManager: EntityManager,
) : PaymentRepositoryCustom {
  /**
   * Find payments by user ID and status with type-safe query
   */
  override fun findByUserIdAndStatusEnhanced(
    userId: Long,
    status: PaymentStatus,
  ): List<PaymentModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(PaymentModel::class.java)
    val root = query.from(PaymentModel::class.java)

    val userIdPredicate = cb.equal(root.get<Long>("userId"), userId)
    val statusPredicate = cb.equal(root.get<PaymentStatus>("status"), status)

    query.where(cb.and(userIdPredicate, statusPredicate))
    return entityManager.createQuery(query).resultList
  }

  /**
   * Find expired payments before specified time
   */
  override fun findExpiredPaymentsEnhanced(now: LocalDateTime): List<PaymentModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(PaymentModel::class.java)
    val root = query.from(PaymentModel::class.java)

    val statusPredicate = cb.equal(root.get<PaymentStatus>("status"), PaymentStatus.PENDING)
    val expiredPredicate = cb.lessThan(root.get<LocalDateTime>("expiredAt"), now)

    query.where(cb.and(statusPredicate, expiredPredicate))
    return entityManager.createQuery(query).resultList
  }

  /**
   * Find payments created within a date range
   */
  override fun findByCreatedAtBetweenEnhanced(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(PaymentModel::class.java)
    val root = query.from(PaymentModel::class.java)

    val startPredicate = cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), startDate)
    val endPredicate = cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), endDate)

    query.where(cb.and(startPredicate, endPredicate))
    return entityManager.createQuery(query).resultList
  }

  /**
   * Count payments by status and creation date range
   */
  override fun countByStatusAndCreatedAtBetweenEnhanced(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(PaymentModel::class.java)

    val statusPredicate = cb.equal(root.get<PaymentStatus>("status"), status)
    val startPredicate = cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), startDate)
    val endPredicate = cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), endDate)

    query.select(cb.count(root))
    query.where(cb.and(statusPredicate, startPredicate, endPredicate))
    return entityManager.createQuery(query).singleResult ?: 0L
  }

  /**
   * Sum payment amounts by status and creation date range using database-level aggregation
   * This is much more efficient than in-memory aggregation
   * Returns result in cents (Long) since amount is stored as cents in the database
   */
  override fun sumAmountByStatusAndCreatedAtBetweenEnhanced(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery()
    val root = query.from(PaymentModel::class.java)

    val statusPredicate = cb.equal(root.get<PaymentStatus>("status"), status)
    val startPredicate = cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), startDate)
    val endPredicate = cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), endDate)

    query.select(cb.sum(root.get<Long>("amount")))
    query.where(cb.and(statusPredicate, startPredicate, endPredicate))

    val result = entityManager.createQuery(query).singleResult
    return (result as? Number)?.toLong() ?: 0L
  }

  /**
   * Complex query: find payments with multiple criteria
   */
  override fun findPaymentsWithFilters(
    userId: Long?,
    status: PaymentStatus?,
    transactionId: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    minAmount: Long?,
    maxAmount: Long?,
  ): List<PaymentModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(PaymentModel::class.java)
    val root = query.from(PaymentModel::class.java)

    val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

    userId?.let {
      predicates.add(cb.equal(root.get<Long>("userId"), it))
    }

    status?.let {
      predicates.add(cb.equal(root.get<PaymentStatus>("status"), it))
    }

    transactionId?.let {
      predicates.add(cb.equal(root.get<String>("transactionId"), it))
    }

    startDate?.let { start ->
      endDate?.let { end ->
        predicates.add(cb.between(root.get<LocalDateTime>("createdAt"), start, end))
      }
    }

    minAmount?.let { min ->
      predicates.add(cb.greaterThanOrEqualTo(root.get<Long>("amount"), min))
    }

    maxAmount?.let { max ->
      predicates.add(cb.lessThanOrEqualTo(root.get<Long>("amount"), max))
    }

    if (predicates.isNotEmpty()) {
      query.where(cb.and(*predicates.toTypedArray()))
    }

    query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))
    return entityManager.createQuery(query).resultList
  }
}
