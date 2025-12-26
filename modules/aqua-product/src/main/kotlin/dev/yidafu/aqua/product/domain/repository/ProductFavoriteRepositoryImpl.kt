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

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.QProductFavoriteModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Custom repository implementation for ProductFavorite using JPA Criteria API
 */
@Repository
class ProductFavoriteRepositoryImpl : ProductFavoriteRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

/**
 * Result data class for favorites trend
 */
data class FavoriteTrendRow(
  val date: LocalDate,
  val totalCount: Long,
  val distinctUsers: Long
)

/**
 * Result data class for daily active users
 */
data class DailyActiveUsersRow(
  val date: LocalDate,
  val activeUserCount: Long
)

  override fun findFavoriteProductIdsByUserId(userId: Long): List<Long> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(root.get<Long>("productId"))
    query.where(
      cb.equal(root.get<Long>("userId"), userId),
      cb.equal(root.get<Boolean>("enable"), true)
    )

    return entityManager.createQuery(query).resultList
  }

  override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.count(root))
    query.where(
      cb.equal(root.get<Long>("userId"), userId),
      cb.equal(root.get<Long>("productId"), productId),
      cb.equal(root.get<Boolean>("enable"), true)
    )

    val count = entityManager.createQuery(query).singleResult ?: 0L
    return count > 0
  }

  override fun countByUserId(userId: Long): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.count(root))
    query.where(
      cb.equal(root.get<Long>("userId"), userId),
      cb.equal(root.get<Boolean>("enable"), true)
    )

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  @Transactional
  override fun updateEnableStatus(userId: Long, productId: Long, enable: Boolean): Int {
    val cb = entityManager.criteriaBuilder
    val update = cb.createCriteriaUpdate(ProductFavoriteModel::class.java)
    val root = update.from(ProductFavoriteModel::class.java)

    update.set("enable", enable)
    update.where(
      cb.equal(root.get<Long>("userId"), userId),
      cb.equal(root.get<Long>("productId"), productId)
    )

    return entityManager.createQuery(update).executeUpdate()
  }

  // Admin analytics methods

  override fun countDistinctUsersWithFavorites(): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.countDistinct(root.get<Long>("userId")))

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  override fun countFavoritesSince(startDate: LocalDateTime): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.count(root))
    query.where(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate))

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  override fun countFavoritesBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.count(root))
    query.where(
      cb.greaterThanOrEqualTo(root.get("createdAt"), startDate),
      cb.lessThanOrEqualTo(root.get("createdAt"), endDate)
    )

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  override fun findMostFavoritedProducts(): List<Array<Any>> {
    // Use JPQL for complex joins
    val jpql = """
      SELECT p, COUNT(pf) as favoriteCount
      FROM ProductFavoriteModel pf
      JOIN pf.product p
      GROUP BY p.id
      ORDER BY favoriteCount DESC
    """

    val query = entityManager.createQuery(jpql)
    return query.resultList as List<Array<Any>>
  }

  override fun findMostFavoritedProductsSince(startDate: LocalDateTime): List<Array<Any>> {
    val jpql = """
      SELECT p, COUNT(pf) as favoriteCount
      FROM ProductFavoriteModel pf
      JOIN pf.product p
      WHERE pf.createdAt >= :startDate
      GROUP BY p.id
      ORDER BY favoriteCount DESC
    """

    val query = entityManager.createQuery(jpql)
    query.setParameter("startDate", startDate)
    return query.resultList as List<Array<Any>>
  }

  override fun getFavoritesTrend(startDate: LocalDateTime): List<FavoriteTrendRow> {
    val qProductFavorite = QProductFavoriteModel.productFavoriteModel

    // Create date expression for PostgreSQL DATE() function
    val dateExpr = Expressions.dateTemplate(
      LocalDate::class.java,
      "DATE({0})",
      qProductFavorite.createdAt
    )

    return queryFactory
      .select(
        Projections.constructor(
          FavoriteTrendRow::class.java,
          dateExpr,
          qProductFavorite.count(),
          qProductFavorite.userId.countDistinct()
        )
      )
      .from(qProductFavorite)
      .where(qProductFavorite.createdAt.goe(startDate))
      .groupBy(dateExpr)
      .orderBy(dateExpr.asc())
      .fetch()
  }

  override fun getUserFavoriteSummaries(): List<Array<Any>> {
    val jpql = """
      SELECT pf.userId, COUNT(pf) as favoriteCount, MAX(pf.createdAt) as lastCreatedAt
      FROM ProductFavoriteModel pf
      GROUP BY pf.userId
      ORDER BY favoriteCount DESC
    """

    val query = entityManager.createQuery(jpql)
    return query.resultList as List<Array<Any>>
  }

  override fun countByProductId(productId: Long): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.count(root))
    query.where(cb.equal(root.get<Long>("productId"), productId))

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  override fun findFavoritesWithFilters(
    userId: Long?,
    productId: Long?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    pageable: Pageable
  ): Page<ProductFavoriteModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ProductFavoriteModel::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    val predicates = mutableListOf<Predicate>()

    userId?.let { predicates.add(cb.equal(root.get<Long>("userId"), it)) }
    productId?.let { predicates.add(cb.equal(root.get<Long>("productId"), it)) }
    dateFrom?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it)) }
    dateTo?.let { predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it)) }

    query.where(*predicates.toTypedArray())

    // Get total count
    val countQuery = cb.createQuery(Long::class.java)
    val countRoot = countQuery.from(ProductFavoriteModel::class.java)
    val countPredicates = mutableListOf<Predicate>()

    userId?.let { countPredicates.add(cb.equal(countRoot.get<Long>("userId"), it)) }
    productId?.let { countPredicates.add(cb.equal(countRoot.get<Long>("productId"), it)) }
    dateFrom?.let { countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("createdAt"), it)) }
    dateTo?.let { countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("createdAt"), it)) }

    countQuery.select(cb.count(countRoot)).where(*countPredicates.toTypedArray())
    val total = entityManager.createQuery(countQuery).singleResult ?: 0L

    // Apply pagination
    query.orderBy(cb.desc(root.get<Long>("id")))
    val typedQuery = entityManager.createQuery(query)
    typedQuery.firstResult = pageable.offset.toInt()
    typedQuery.maxResults = pageable.pageSize

    val results = typedQuery.resultList

    return PageImpl(results, pageable, total)
  }

  override fun findByUserIdsAndProductIds(userIds: List<Long>, productIds: List<Long>): List<ProductFavoriteModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ProductFavoriteModel::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.where(
      cb.isTrue(root.get<Long>("userId").`in`(userIds)),
      cb.isTrue(root.get<Long>("productId").`in`(productIds))
    )

    return entityManager.createQuery(query).resultList
  }

  // Product analytics

  override fun getProductFavoriteStatsSince(startDate: LocalDateTime): List<Array<Any>> {
    val jpql = """
      SELECT pf.productId, COUNT(pf) as favoriteCount, AVG(p.price) as averagePrice
      FROM ProductFavoriteModel pf
      JOIN pf.product p
      WHERE pf.createdAt >= :startDate
      GROUP BY pf.productId
      ORDER BY favoriteCount DESC
    """

    val query = entityManager.createQuery(jpql)
    query.setParameter("startDate", startDate)
    return query.resultList as List<Array<Any>>
  }

  override fun getUserEngagementStats(): List<Array<Any>> {
    val jpql = """
      SELECT pf.userId, COUNT(pf) as favoriteCount, AVG(p.price) as averagePrice
      FROM ProductFavoriteModel pf
      JOIN pf.product p
      GROUP BY pf.userId
      HAVING COUNT(pf) > 0
    """

    val query = entityManager.createQuery(jpql)
    return query.resultList as List<Array<Any>>
  }

  // Export methods

  override fun findFavoritesForExport(
    userId: Long?,
    productIds: List<Long>?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?
  ): List<ProductFavoriteModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ProductFavoriteModel::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    val predicates = mutableListOf<Predicate>()

    userId?.let { predicates.add(cb.equal(root.get<Long>("userId"), it)) }
    productIds?.let { predicates.add(cb.isTrue(root.get<Long>("productId").`in`(it))) }
    dateFrom?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it)) }
    dateTo?.let { predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it)) }

    query.where(*predicates.toTypedArray())

    return entityManager.createQuery(query).resultList
  }

  // Statistics for specific periods

  override fun countActiveUsersSince(startDate: LocalDateTime): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(ProductFavoriteModel::class.java)

    query.select(cb.countDistinct(root.get<Long>("userId")))
    query.where(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate))

    return entityManager.createQuery(query).singleResult ?: 0L
  }

  override fun getAverageFavoritesPerUser(): Double {
    // Using a subquery approach
    val totalFavorites = entityManager.createQuery(
      "SELECT COUNT(pf) FROM ProductFavoriteModel pf",
      Long::class.java
    ).singleResult ?: 0L

    val distinctUsers = entityManager.createQuery(
      "SELECT COUNT(DISTINCT pf.userId) FROM ProductFavoriteModel pf",
      Long::class.java
    ).singleResult ?: 0L

    return if (distinctUsers > 0) totalFavorites.toDouble() / distinctUsers else 0.0
  }

  override fun getDailyActiveUsers(startDate: LocalDateTime): List<DailyActiveUsersRow> {
    val qProductFavorite = QProductFavoriteModel.productFavoriteModel

    // Create date expression for PostgreSQL DATE() function
    val dateExpr = Expressions.dateTemplate(
      LocalDate::class.java,
      "DATE({0})",
      qProductFavorite.createdAt
    )

    return queryFactory
      .select(
        Projections.constructor(
          DailyActiveUsersRow::class.java,
          dateExpr,
          qProductFavorite.userId.countDistinct()
        )
      )
      .from(qProductFavorite)
      .where(qProductFavorite.createdAt.goe(startDate))
      .groupBy(dateExpr)
      .orderBy(dateExpr.desc())
      .fetch()
  }
}
