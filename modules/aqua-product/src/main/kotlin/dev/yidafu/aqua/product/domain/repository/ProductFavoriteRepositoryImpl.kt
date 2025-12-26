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

package dev.yidafu.aqua.product.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.QProductFavoriteModel.productFavoriteModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Custom repository implementation for ProductFavorite using type-safe QueryDSL
 */
@Repository
class ProductFavoriteRepositoryImpl : ProductFavoriteRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findFavoriteProductIdsByUserId(userId: Long): List<Long> {
    return queryFactory.select(productFavoriteModel.productId)
      .from(productFavoriteModel)
      .where(productFavoriteModel.userId.eq(userId))
      .where(productFavoriteModel.enable.eq(true))
      .fetch()
  }

  override fun findFavoriteIdsByUserId(userId: Long, pageable: Pageable): Page<Long> {
    // Get total count
    val total = queryFactory.query()
      .from(productFavoriteModel)
      .where(productFavoriteModel.userId.eq(userId))
      .where(productFavoriteModel.enable.eq(true))
      .fetchCount()

    // Get paginated product IDs
    val results = queryFactory.select(productFavoriteModel.productId)
      .from(productFavoriteModel)
      .where(productFavoriteModel.userId.eq(userId))
      .where(productFavoriteModel.enable.eq(true))
      .orderBy(productFavoriteModel.createdAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    return PageImpl(results, pageable, total)
  }

  override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean {
    val count = queryFactory.selectFrom(productFavoriteModel)
      .where(productFavoriteModel.userId.eq(userId))
      .where(productFavoriteModel.productId.eq(productId))
      .where(productFavoriteModel.enable.eq(true))
      .fetchCount()
    return count > 0
  }

  override fun countByUserId(userId: Long): Long {
    return queryFactory.query()
      .from(productFavoriteModel)
      .where(productFavoriteModel.userId.eq(userId))
      .where(productFavoriteModel.enable.eq(true))
      .fetchCount()
  }

  @Transactional
  override fun updateEnableStatus(userId: Long, productId: Long, enable: Boolean): Int {
    return queryFactory.update(productFavoriteModel)
      .set(productFavoriteModel.enable, enable)
      .where(productFavoriteModel.userId.eq(userId))
      .where(productFavoriteModel.productId.eq(productId))
      .execute()
      .toInt()
  }

  // Admin analytics methods

  override fun countDistinctUsersWithFavorites(): Long {
    return queryFactory.query()
      .from(productFavoriteModel)
      .select(productFavoriteModel.userId.countDistinct())
      .fetchCount()
  }

  override fun countFavoritesSince(startDate: LocalDateTime): Long {
    return queryFactory.query()
      .from(productFavoriteModel)
      .where(productFavoriteModel.createdAt.goe(startDate))
      .fetchCount()
  }

  override fun findMostFavoritedProducts(): List<ProductFavoriteRepositoryCustom.ProductFavoriteCount> {
    val results = queryFactory.select(
      Projections.constructor(
        ProductFavoriteRepositoryCustom.ProductFavoriteCount::class.java,
        productFavoriteModel.productId,
        productFavoriteModel.count()
      )
    )
    .from(productFavoriteModel)
    .groupBy(productFavoriteModel.productId)
    .orderBy(productFavoriteModel.count().desc())
    .fetch()

    return results
  }

  // Export methods

  override fun findFavoritesForExport(
    userId: Long?,
    productIds: List<Long>?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?
  ): List<ProductFavoriteModel> {
    val builder = BooleanBuilder()

    userId?.let { builder.and(productFavoriteModel.userId.eq(it)) }
    productIds?.let {
      if (it.isNotEmpty()) {
        builder.and(productFavoriteModel.productId.`in`(it))
      }
    }
    dateFrom?.let { builder.and(productFavoriteModel.createdAt.goe(it)) }
    dateTo?.let { builder.and(productFavoriteModel.createdAt.loe(it)) }

    return queryFactory.selectFrom(productFavoriteModel)
      .where(builder)
      .fetch()
  }

  // Statistics for specific periods

  override fun getAverageFavoritesPerUser(): Double {
    val totalFavorites = queryFactory.query()
      .from(productFavoriteModel)
      .fetchCount()

    val distinctUsers = queryFactory.query()
      .from(productFavoriteModel)
      .select(productFavoriteModel.userId.countDistinct())
      .fetchCount()

    return if (distinctUsers > 0) totalFavorites.toDouble() / distinctUsers else 0.0
  }
}
