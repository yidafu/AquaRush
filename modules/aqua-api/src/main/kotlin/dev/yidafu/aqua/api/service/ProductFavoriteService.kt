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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductFavoriteService {
  /**
   * Add product to user's favorites
   */
  fun addToFavorites(userId: Long, productId: Long): ProductFavoriteModel

  /**
   * Get user's favorite products with pagination
   */
  fun getFavoriteProducts(userId: Long, pageable: Pageable): Page<ProductModel>

  /**
   * Check if product is favorited by user
   */
  fun isProductFavorited(userId: Long, productId: Long): Boolean

  /**
   * Get total count of user's favorites
   */
  fun getFavoritesCount(userId: Long): Long

  /**
   * Get user's favorite product IDs only (for internal use)
   */
  fun getFavoriteProductIds(userId: Long): List<Long>

  /**
   * Toggle favorite status (add if not exists, toggle enable field if exists)
   */
  fun toggleFavorite(userId: Long, productId: Long): Boolean

  /**
   * Get user's favorite entities (not products) for internal operations
   */
  fun getUserFavoriteEntities(userId: Long, pageable: Pageable): Page<ProductFavoriteModel>

  // ============== Admin Methods ==============

  /**
   * Get comprehensive analytics about user favorites
   */
  fun getFavoriteAnalytics(): UserFavoriteAnalytics

  /**
   * Get all products favorite statistics
   */
  fun getAllProductsFavoriteStats(): AllProductsFavoriteStats

  /**
   * Get products sorted by favorite count with pagination
   */
  fun getProductsByFavorites(page: Int, size: Int, minFavorites: Int?): ProductFavoritePage

  /**
   * Perform batch operations on user favorites
   */
  fun performBatchOperation(input: BatchFavoriteOperationInput): BatchOperationResult

  /**
   * Export favorites data to CSV/Excel format
   */
  fun exportFavorites(input: ExportFavoritesInput): ExportFavoritesResult
}
