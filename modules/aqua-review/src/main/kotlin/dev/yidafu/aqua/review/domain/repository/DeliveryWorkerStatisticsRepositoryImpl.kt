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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.QDeliveryWorkerStatisticsModel.deliveryWorkerStatisticsModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Custom repository implementation for DeliveryWorkerStatistics entity using QueryDSL
 */
@Repository
class DeliveryWorkerStatisticsRepositoryImpl : DeliveryWorkerStatisticsRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findDeliveryWorkersRanking(
    sortBy: String,
    minReviews: Int,
    pageable: Pageable,
  ): Page<DeliveryWorkerStatisticsModel> {
    // Count query
    val total = queryFactory.query()
      .from(deliveryWorkerStatisticsModel)
      .where(deliveryWorkerStatisticsModel.totalReviews.goe(minReviews))
      .fetchCount()

    // Main query with dynamic sorting
    val primarySort = when (sortBy.lowercase()) {
      "reviews" -> deliveryWorkerStatisticsModel.totalReviews.desc()
      else -> deliveryWorkerStatisticsModel.averageRating.desc() // default to rating
    }

    val results = queryFactory.selectFrom(deliveryWorkerStatisticsModel)
      .where(deliveryWorkerStatisticsModel.totalReviews.goe(minReviews))
      .orderBy(primarySort, deliveryWorkerStatisticsModel.lastUpdated.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    return PageImpl(results, pageable, total)
  }

  override fun findDeliveryWorkersByRatingRange(
    minRating: Double,
    maxRating: Double,
    minReviews: Int,
    maxReviews: Int?,
    sortBy: String,
    pageable: Pageable,
  ): Page<DeliveryWorkerStatisticsModel> {
    val minRatingBg = BigDecimal.valueOf(minRating)
    val maxRatingBg = BigDecimal.valueOf(maxRating)

    // Build predicates
    var basePredicate = deliveryWorkerStatisticsModel.averageRating.goe(minRatingBg)
      .and(deliveryWorkerStatisticsModel.averageRating.loe(maxRatingBg))
      .and(deliveryWorkerStatisticsModel.totalReviews.goe(minReviews))

    maxReviews?.let {
      basePredicate = basePredicate.and(deliveryWorkerStatisticsModel.totalReviews.loe(it))
    }

    // Count query
    val total = queryFactory.query()
      .from(deliveryWorkerStatisticsModel)
      .where(basePredicate)
      .fetchCount()

    // Main query with dynamic sorting
    val primarySort = when (sortBy.lowercase()) {
      "reviews" -> deliveryWorkerStatisticsModel.totalReviews.desc()
      else -> deliveryWorkerStatisticsModel.averageRating.desc()
    }

    val results = queryFactory.selectFrom(deliveryWorkerStatisticsModel)
      .where(basePredicate)
      .orderBy(primarySort, deliveryWorkerStatisticsModel.lastUpdated.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    return PageImpl(results, pageable, total)
  }

  override fun countDeliveryWorkersByMinRating(
    minRating: Double,
    minReviews: Int,
  ): Long {
    val minRatingBg = BigDecimal.valueOf(minRating)

    return queryFactory.query()
      .from(deliveryWorkerStatisticsModel)
      .where(
        deliveryWorkerStatisticsModel.averageRating.goe(minRatingBg)
          .and(deliveryWorkerStatisticsModel.totalReviews.goe(minReviews))
      )
      .fetchCount()
  }

  override fun findOverallAverageRating(minReviews: Int): Double? {
    return queryFactory.query()
      .from(deliveryWorkerStatisticsModel)
      .where(deliveryWorkerStatisticsModel.totalReviews.goe(minReviews))
      .select(deliveryWorkerStatisticsModel.averageRating.avg())
      .fetchOne()
      ?.toDouble()
  }
}
