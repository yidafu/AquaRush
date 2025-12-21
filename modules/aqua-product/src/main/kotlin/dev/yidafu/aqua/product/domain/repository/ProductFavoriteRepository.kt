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

import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface ProductFavoriteRepository : JpaRepository<ProductFavoriteModel, Long>, JpaSpecificationExecutor<ProductFavoriteModel> {

  // Existing methods
  fun findByUserIdAndProductId(userId: Long, productId: Long): ProductFavoriteModel?

  fun findByUserId(userId: Long, pageable: Pageable): Page<ProductFavoriteModel>

  @Query("SELECT uf.productId FROM ProductFavoriteModel uf WHERE uf.userId = :userId")
  fun findFavoriteProductIdsByUserId(@Param("userId") userId: Long): List<Long>

  
  @Query("SELECT COUNT(uf) > 0 FROM ProductFavoriteModel uf WHERE uf.userId = :userId AND uf.productId = :productId")
  fun existsByUserIdAndProductId(@Param("userId") userId: Long, @Param("productId") productId: Long): Boolean

  fun countByUserId(userId: Long): Long

  
  // Admin analytics methods
  @Query("SELECT COUNT(DISTINCT uf.userId) FROM ProductFavoriteModel uf")
  fun countDistinctUsersWithFavorites(): Long

  @Query("SELECT COUNT(uf) FROM ProductFavoriteModel uf WHERE uf.createdAt >= :startDate")
  fun countFavoritesSince(@Param("startDate") startDate: LocalDateTime): Long

  @Query("SELECT COUNT(uf) FROM ProductFavoriteModel uf WHERE uf.createdAt >= :startDate AND uf.createdAt <= :endDate")
  fun countFavoritesBetween(@Param("startDate") startDate: LocalDateTime, @Param("endDate") endDate: LocalDateTime): Long

  @Query("SELECT p, COUNT(uf) as favoriteCount FROM ProductFavoriteModel uf JOIN uf.product p GROUP BY p.id ORDER BY favoriteCount DESC")
  fun findMostFavoritedProducts(): List<Array<Any>>

  @Query("SELECT p, COUNT(uf) as favoriteCount FROM ProductFavoriteModel uf JOIN uf.product p WHERE uf.createdAt >= :startDate GROUP BY p.id ORDER BY favoriteCount DESC")
  fun findMostFavoritedProductsSince(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>

  @Query("SELECT DATE(uf.createdAt), COUNT(uf), COUNT(DISTINCT uf.userId) FROM ProductFavoriteModel uf WHERE uf.createdAt >= :startDate GROUP BY DATE(uf.createdAt) ORDER BY DATE(uf.createdAt)")
  fun getFavoritesTrend(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>

  @Query("SELECT uf.userId, COUNT(uf) as favoriteCount, MAX(uf.createdAt) FROM ProductFavoriteModel uf GROUP BY uf.userId ORDER BY favoriteCount DESC")
  fun getUserFavoriteSummaries(): List<Array<Any>>

  @Query("SELECT COUNT(uf) FROM ProductFavoriteModel uf WHERE uf.productId = :productId")
  fun countByProductId(@Param("productId") productId: Long): Long

  @Query("SELECT uf FROM ProductFavoriteModel uf WHERE (:userId IS NULL OR uf.userId = :userId) AND (:productId IS NULL OR uf.productId = :productId) AND (:dateFrom IS NULL OR uf.createdAt >= :dateFrom) AND (:dateTo IS NULL OR uf.createdAt <= :dateTo)")
  fun findFavoritesWithFilters(
    @Param("userId") userId: Long?,
    @Param("productId") productId: Long?,
    @Param("dateFrom") dateFrom: LocalDateTime?,
    @Param("dateTo") dateTo: LocalDateTime?,
    pageable: Pageable
  ): Page<ProductFavoriteModel>

  @Query("SELECT uf FROM ProductFavoriteModel uf WHERE uf.userId IN :userIds AND uf.productId IN :productIds")
  fun findByUserIdsAndProductIds(@Param("userIds") userIds: List<Long>, @Param("productIds") productIds: List<Long>): List<ProductFavoriteModel>

  
  // Product analytics
  @Query("SELECT uf.productId, COUNT(uf) as favoriteCount, AVG(p.price) as averagePrice FROM ProductFavoriteModel uf JOIN uf.product p WHERE uf.createdAt >= :startDate GROUP BY uf.productId ORDER BY favoriteCount DESC")
  fun getProductFavoriteStatsSince(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>

  @Query("SELECT uf.userId, COUNT(uf) as favoriteCount, AVG(p.price) as averagePrice FROM ProductFavoriteModel uf JOIN uf.product p GROUP BY uf.userId HAVING COUNT(uf) > 0")
  fun getUserEngagementStats(): List<Array<Any>>

  // Export methods
  @Query("SELECT uf FROM ProductFavoriteModel uf WHERE (:userId IS NULL OR uf.userId = :userId) AND (:productIds IS NULL OR uf.productId IN :productIds) AND (:dateFrom IS NULL OR uf.createdAt >= :dateFrom) AND (:dateTo IS NULL OR uf.createdAt <= :dateTo)")
  fun findFavoritesForExport(
    @Param("userId") userId: Long?,
    @Param("productIds") productIds: List<Long>?,
    @Param("dateFrom") dateFrom: LocalDateTime?,
    @Param("dateTo") dateTo: LocalDateTime?
  ): List<ProductFavoriteModel>

  // Statistics for specific periods
  @Query("SELECT COUNT(DISTINCT uf.userId) FROM ProductFavoriteModel uf WHERE uf.createdAt >= :startDate")
  fun countActiveUsersSince(@Param("startDate") startDate: LocalDateTime): Long

  @Query("SELECT AVG(favoriteCount) FROM (SELECT COUNT(uf) as favoriteCount FROM ProductFavoriteModel uf GROUP BY uf.userId) AS userCounts")
  fun getAverageFavoritesPerUser(): Double

  @Query("SELECT DATE(uf.createdAt), COUNT(DISTINCT uf.userId) FROM ProductFavoriteModel uf WHERE uf.createdAt >= :startDate GROUP BY DATE(uf.createdAt) ORDER BY DATE(uf.createdAt) DESC")
  fun getDailyActiveUsers(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
}
