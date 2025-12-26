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
import java.time.LocalDateTime

/**
 * Custom repository interface for ProductFavorite with QueryDSL implementations
 */
interface ProductFavoriteRepositoryCustom {

  /**
   * Find all favorite product IDs for a user
   * @param userId the user ID
   * @return list of product IDs
   */
  fun findFavoriteProductIdsByUserId(userId: Long): List<Long>

  /**
   * Find favorite product IDs for a user with pagination
   * @param userId the user ID
   * @param pageable pagination information
   * @return page of product favorite IDs
   */
  fun findFavoriteIdsByUserId(userId: Long, pageable: Pageable): Page<Long>

  /**
   * Check if a favorite exists by user ID and product ID
   * @param userId the user ID
   * @param productId the product ID
   * @return true if exists, false otherwise
   */
  fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean

  /**
   * Count favorites by user ID
   * @param userId the user ID
   * @return count of favorites
   */
  fun countByUserId(userId: Long): Long

  /**
   * Update enable status
   * @param userId the user ID
   * @param productId the product ID
   * @param enable the enable status
   * @return number of updated records
   */
  fun updateEnableStatus(userId: Long, productId: Long, enable: Boolean): Int

  // Admin analytics methods

  /**
   * Count distinct users with favorites
   * @return count of distinct users
   */
  fun countDistinctUsersWithFavorites(): Long

  /**
   * Count favorites since a date
   * @param startDate the start date
   * @return count of favorites
   */
  fun countFavoritesSince(startDate: LocalDateTime): Long

  /**
   * Result data class for product favorite count
   */
  data class ProductFavoriteCount(
    val productId: Long,
    val favoriteCount: Long
  )

  /**
   * Find most favorited products
   * @return list of ProductFavoriteCount containing [productId, favoriteCount]
   */
  fun findMostFavoritedProducts(): List<ProductFavoriteCount>

  // Export methods

  /**
   * Find favorites for export with filters
   * @param userId optional user ID filter
   * @param productIds optional list of product IDs filter
   * @param dateFrom optional date from filter
   * @param dateTo optional date to filter
   * @return list of favorites
   */
  fun findFavoritesForExport(
    userId: Long?,
    productIds: List<Long>?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?
  ): List<ProductFavoriteModel>

  // Statistics for specific periods

  /**
   * Get average favorites per user
   * @return average favorites per user
   */
  fun getAverageFavoritesPerUser(): Double
}
