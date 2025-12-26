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

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QReviewModel.reviewModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for Review entity using QueryDSL
 */
@Repository
class ReviewRepositoryImpl : ReviewRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findReviewsWithFilters(
    deliveryWorkerId: Long?,
    minRating: Int?,
    maxRating: Int?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    userId: Long?,
    pageable: Pageable,
  ): Page<ReviewModel> {
    val builder = BooleanBuilder()

    deliveryWorkerId?.let { builder.and(reviewModel.deliveryWorkerId.eq(it)) }
    minRating?.let { builder.and(reviewModel.rating.goe(it)) }
    maxRating?.let { builder.and(reviewModel.rating.loe(it)) }
    dateFrom?.let { builder.and(reviewModel.createdAt.goe(it)) }
    dateTo?.let { builder.and(reviewModel.createdAt.loe(it)) }
    userId?.let { builder.and(reviewModel.userId.eq(it)) }

    // Count query
    val totalCount = queryFactory.query()
      .from(reviewModel)
      .where(builder)
      .fetchCount()

    // Main query with pagination
    val results = queryFactory.selectFrom(reviewModel)
      .where(builder)
      .orderBy(reviewModel.createdAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    return PageImpl(results, pageable, totalCount)
  }

  override fun countByDeliveryWorkerIdAndRating(
    deliveryWorkerId: Long,
    rating: Int,
  ): Long {
    return queryFactory.query()
      .from(reviewModel)
      .where(
        reviewModel.deliveryWorkerId.eq(deliveryWorkerId)
          .and(reviewModel.rating.eq(rating))
      )
      .fetchCount()
  }

  override fun findAverageRatingByDeliveryWorkerId(deliveryWorkerId: Long): Double? {
    return queryFactory.query()
      .from(reviewModel)
      .where(reviewModel.deliveryWorkerId.eq(deliveryWorkerId))
      .select(reviewModel.rating.avg())
      .fetchOne()
  }
}
