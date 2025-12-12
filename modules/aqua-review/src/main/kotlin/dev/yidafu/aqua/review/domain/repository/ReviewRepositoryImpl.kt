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

import dev.yidafu.aqua.review.domain.model.ReviewModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for Review entity using JPA Criteria API
 */
@Repository
class ReviewRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager
) : ReviewRepositoryCustom {

  override fun findReviewsWithFilters(
    deliveryWorkerId: Long?,
    minRating: Int?,
    maxRating: Int?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    userId: Long?,
    pageable: Pageable
  ): Page<ReviewModel> {
    val cb = entityManager.criteriaBuilder
    val countQuery = cb.createQuery(Long::class.java)
    val rootCount = countQuery.from(ReviewModel::class.java)

    // Build dynamic predicates list for count query
    val countPredicates = mutableListOf<Predicate>()

    deliveryWorkerId?.let {
      countPredicates.add(cb.equal(rootCount.get<Long>("deliveryWorkerId"), it))
    }

    minRating?.let {
      countPredicates.add(cb.greaterThanOrEqualTo(rootCount.get<Int>("rating"), it))
    }

    maxRating?.let {
      countPredicates.add(cb.lessThanOrEqualTo(rootCount.get<Int>("rating"), it))
    }

    dateFrom?.let {
      countPredicates.add(cb.greaterThanOrEqualTo(rootCount.get<LocalDateTime>("createdAt"), it))
    }

    dateTo?.let {
      countPredicates.add(cb.lessThanOrEqualTo(rootCount.get<LocalDateTime>("createdAt"), it))
    }

    userId?.let {
      countPredicates.add(cb.equal(rootCount.get<Long>("userId"), it))
    }

    // Apply where clause if predicates exist for count query
    if (countPredicates.isNotEmpty()) {
      countQuery.where(*countPredicates.toTypedArray())
    }

    // Execute count query
    countQuery.select(cb.count(rootCount))
    val totalCount = entityManager.createQuery(countQuery).singleResult

    // Create main query for results
    val query = cb.createQuery(ReviewModel::class.java)
    val root = query.from(ReviewModel::class.java)

    // Rebuild predicates for main query
    val predicates = mutableListOf<Predicate>()

    deliveryWorkerId?.let {
      predicates.add(cb.equal(root.get<Long>("deliveryWorkerId"), it))
    }

    minRating?.let {
      predicates.add(cb.greaterThanOrEqualTo(root.get<Int>("rating"), it))
    }

    maxRating?.let {
      predicates.add(cb.lessThanOrEqualTo(root.get<Int>("rating"), it))
    }

    dateFrom?.let {
      predicates.add(cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), it))
    }

    dateTo?.let {
      predicates.add(cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), it))
    }

    userId?.let {
      predicates.add(cb.equal(root.get<Long>("userId"), it))
    }

    // Apply where clause if predicates exist for main query
    if (predicates.isNotEmpty()) {
      query.where(*predicates.toTypedArray())
    }

    // Apply sorting - default order by createdAt DESC
    val orders = mutableListOf<jakarta.persistence.criteria.Order>()

    if (pageable.sort.isSorted) {
      pageable.sort.forEach { sort ->
        if (sort.isAscending) {
          orders.add(cb.asc(root.get<Any>(sort.property)))
        } else {
          orders.add(cb.desc(root.get<Any>(sort.property)))
        }
      }
    }

    if (orders.isNotEmpty()) {
      query.orderBy(*orders.toTypedArray())
    } else {
      // Default order by createdAt DESC
      query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))
    }

    // Apply pagination
    val typedQuery = entityManager.createQuery(query)
    typedQuery.firstResult = pageable.offset.toInt()
    typedQuery.maxResults = pageable.pageSize

    // Execute query and get results
    val results = typedQuery.resultList

    // Return Page object
    return PageImpl(results, pageable, totalCount)
  }

  override fun countByDeliveryWorkerIdAndRating(deliveryWorkerId: Long, rating: Int): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ReviewModel::class.java)

    // Create count query
    query.select(cb.count(root))

    // Create predicates for deliveryWorkerId and rating
    val predicates = mutableListOf<Predicate>()

    // deliveryWorkerId = :deliveryWorkerId predicate
    predicates.add(cb.equal(root.get<Long>("deliveryWorkerId"), deliveryWorkerId))

    // rating = :rating predicate
    predicates.add(cb.equal(root.get<Int>("rating"), rating))

    // Apply where clause with AND condition
    query.where(*predicates.toTypedArray())

    // Execute count query and return result
    return entityManager.createQuery(query).singleResult
  }

  override fun findAverageRatingByDeliveryWorkerId(deliveryWorkerId: Long): Double? {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Double::class.java)
    val root = query.from(ReviewModel::class.java)

    // Create AVG query for rating
    query.select(cb.avg(root.get<Int>("rating")))

    // Create predicate for deliveryWorkerId
    query.where(cb.equal(root.get<Long>("deliveryWorkerId"), deliveryWorkerId))

    // Execute query and return result (can be null if no reviews found)
    return entityManager.createQuery(query).singleResult
  }
}