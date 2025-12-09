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

package dev.yidafu.aqua.review.domain.repository

import dev.yidafu.aqua.review.domain.model.DeliveryWorkerStatisticsModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Subquery
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Custom repository implementation for DeliveryWorkerStatistics entity using type-safe queries
 */
@Repository
class DeliveryWorkerStatisticsRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : DeliveryWorkerStatisticsRepositoryCustom {

  override fun findDeliveryWorkersRanking(
    sortBy: String,
    minReviews: Int,
    pageable: Pageable
  ): Page<DeliveryWorkerStatisticsModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(DeliveryWorkerStatisticsModel::class.java)
    val root = query.from(DeliveryWorkerStatisticsModel::class.java)

    // Create predicate for minimum reviews
    val predicates = mutableListOf<Predicate>()
    predicates.add(cb.greaterThanOrEqualTo(root.get<Int>("totalReviews"), minReviews))

    // Apply where clause
    query.where(*predicates.toTypedArray())

    // Create dynamic sorting
    val orders = mutableListOf<Order>()

    // Add primary sorting based on sortBy parameter
    @Suppress("UNCHECKED_CAST")
    val sortExpression = when (sortBy.lowercase()) {
      "reviews" -> root.get<Int>("totalReviews")
      "rating" -> root.get<BigDecimal>("averageRating")
      else -> root.get<BigDecimal>("averageRating") // default to rating
    } as jakarta.persistence.criteria.Expression<Any>
    orders.add(cb.desc(sortExpression))

    // Add secondary sorting by lastUpdated
    orders.add(cb.desc(root.get<LocalDateTime>("lastUpdated")))

    // Apply ordering
    query.orderBy(orders)

    // Create count query for pagination
    val countQuery = cb.createQuery(Long::class.java)
    val countRoot = countQuery.from(DeliveryWorkerStatisticsModel::class.java)
    countQuery.select(cb.count(countRoot))
    countQuery.where(cb.greaterThanOrEqualTo(countRoot.get<Int>("totalReviews"), minReviews))

    // Execute queries
    val typedQuery = entityManager.createQuery(query)
    val countTypedQuery = entityManager.createQuery(countQuery)

    // Apply pagination
    val total = countTypedQuery.singleResult
    typedQuery.firstResult = pageable.pageNumber * pageable.pageSize
    typedQuery.maxResults = pageable.pageSize
    val results = typedQuery.resultList

    return PageImpl(results, pageable, total)
  }

  override fun findDeliveryWorkersByRatingRange(
    minRating: Double,
    maxRating: Double,
    minReviews: Int,
    maxReviews: Int?,
    sortBy: String,
    pageable: Pageable
  ): Page<DeliveryWorkerStatisticsModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(DeliveryWorkerStatisticsModel::class.java)
    val root = query.from(DeliveryWorkerStatisticsModel::class.java)

    // Create predicates for rating range and review count
    val predicates = mutableListOf<Predicate>()

    // Rating range predicates
    predicates.add(cb.greaterThanOrEqualTo(root.get<BigDecimal>("averageRating"), BigDecimal.valueOf(minRating)))
    predicates.add(cb.lessThanOrEqualTo(root.get<BigDecimal>("averageRating"), BigDecimal.valueOf(maxRating)))

    // Review count predicates
    predicates.add(cb.greaterThanOrEqualTo(root.get<Int>("totalReviews"), minReviews))

    // Optional maximum reviews
    maxReviews?.let {
      predicates.add(cb.lessThanOrEqualTo(root.get<Int>("totalReviews"), it))
    }

    // Apply where clause
    query.where(*predicates.toTypedArray())

    // Create dynamic sorting (same as ranking)
    val orders = mutableListOf<Order>()
    @Suppress("UNCHECKED_CAST")
    val sortExpression = when (sortBy.lowercase()) {
      "reviews" -> root.get<Int>("totalReviews")
      "rating" -> root.get<BigDecimal>("averageRating")
      else -> root.get<BigDecimal>("averageRating")
    } as jakarta.persistence.criteria.Expression<Any>
    orders.add(cb.desc(sortExpression))
    orders.add(cb.desc(root.get<LocalDateTime>("lastUpdated")))
    query.orderBy(orders)

    // Create count query for pagination
    val countQuery = cb.createQuery(Long::class.java)
    val countRoot = countQuery.from(DeliveryWorkerStatisticsModel::class.java)
    countQuery.select(cb.count(countRoot))

    val countPredicates = mutableListOf<Predicate>()
    countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get<BigDecimal>("averageRating"), BigDecimal.valueOf(minRating)))
    countPredicates.add(cb.lessThanOrEqualTo(countRoot.get<BigDecimal>("averageRating"), BigDecimal.valueOf(maxRating)))
    countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get<Int>("totalReviews"), minReviews))
    maxReviews?.let {
      countPredicates.add(cb.lessThanOrEqualTo(countRoot.get<Int>("totalReviews"), it))
    }

    countQuery.where(*countPredicates.toTypedArray())

    // Execute queries
    val typedQuery = entityManager.createQuery(query)
    val countTypedQuery = entityManager.createQuery(countQuery)

    // Apply pagination
    val total = countTypedQuery.singleResult
    typedQuery.firstResult = pageable.pageNumber * pageable.pageSize
    typedQuery.maxResults = pageable.pageSize
    val results = typedQuery.resultList

    return PageImpl(results, pageable, total)
  }

  override fun countDeliveryWorkersByMinRating(
    minRating: Double,
    minReviews: Int
  ): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(DeliveryWorkerStatisticsModel::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates
    val predicates = mutableListOf<Predicate>()
    predicates.add(cb.greaterThanOrEqualTo(root.get<BigDecimal>("averageRating"), BigDecimal.valueOf(minRating)))
    predicates.add(cb.greaterThanOrEqualTo(root.get<Int>("totalReviews"), minReviews))

    // Apply where clause
    query.where(*predicates.toTypedArray())

    // Execute count query
    return entityManager.createQuery(query).singleResult
  }

  override fun findOverallAverageRating(minReviews: Int): Double? {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Double::class.java)
    val root = query.from(DeliveryWorkerStatisticsModel::class.java)

    // Create average query
    query.select(cb.avg(root.get<BigDecimal>("averageRating")))

    // Filter by minimum reviews
    query.where(cb.greaterThanOrEqualTo(root.get<Int>("totalReviews"), minReviews))

    // Execute average query
    val result = entityManager.createQuery(query).singleResult
    return result?.let { it.toDouble() }
  }
}