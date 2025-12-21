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

package dev.yidafu.aqua.product.service

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.ProductFavoriteModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.domain.repository.ProductFavoriteRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserFavoriteService(
  private val productFavoriteRepository: ProductFavoriteRepository,
  private val productRepository: ProductRepository,
) {

  /**
   * Add product to user's favorites
   */
  @CacheEvict(value = ["user_favorites", "user_favorites_count"], key = "#userId")
  fun addToFavorites(userId: Long, productId: Long): ProductFavoriteModel {
    // Validate product exists and is active
    val product = productRepository.findById(productId)
      .orElseThrow { NotFoundException("Product not found") }

    if (product.status != ProductStatus.ACTIVE && product.status != ProductStatus.ONLINE) {
      throw BadRequestException("Product is not available for favorites")
    }

    // Check if already favorited
    productFavoriteRepository.findByUserIdAndProductId(userId, productId)?.let {
      throw BadRequestException("Product already in favorites")
    }

    val favorite = ProductFavoriteModel(
      userId = userId,
      productId = productId
    )

    return productFavoriteRepository.save(favorite)
  }

  
  /**
   * Get user's favorite products with pagination
   */
  @Cacheable(value = ["user_favorites"], key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
  fun getFavoriteProducts(userId: Long, pageable: Pageable): Page<ProductModel> {
    val favoriteIds = productFavoriteRepository.findFavoriteProductIdsByUserId(userId)

    if (favoriteIds.isEmpty()) {
      return Page.empty(pageable)
    }

    // Get products that are still active/online
    val allProducts = productRepository.findAllById(favoriteIds)
    val activeProducts = allProducts.filter {
      it.status == ProductStatus.ACTIVE || it.status == ProductStatus.ONLINE
    }

    // Manual pagination since we need to filter by product status
    val start = pageable.offset.toInt()
    val end = (start + pageable.pageSize).coerceAtMost(activeProducts.size)

    return if (start >= activeProducts.size) {
      Page.empty(pageable)
    } else {
      PageImpl(activeProducts.subList(start, end), pageable, activeProducts.size.toLong())
    }
  }

  /**
   * Check if product is favorited by user
   */
  @Cacheable(value = ["user_product_favorite"], key = "#userId + '_' + #productId")
  fun isProductFavorited(userId: Long, productId: Long): Boolean {
    return productFavoriteRepository.existsByUserIdAndProductId(userId, productId)
  }

  /**
   * Get total count of user's favorites
   */
  @Cacheable(value = ["user_favorites_count"], key = "#userId")
  fun getFavoritesCount(userId: Long): Long {
    return productFavoriteRepository.countByUserId(userId)
  }

  /**
   * Get user's favorite product IDs only (for internal use)
   */
  fun getFavoriteProductIds(userId: Long): List<Long> {
    return productFavoriteRepository.findFavoriteProductIdsByUserId(userId)
  }

  /**
   * Toggle favorite status (add if not favorited, remove if favorited)
   * Note: Removal functionality has been disabled - users can only add favorites
   */
  @CacheEvict(value = ["user_favorites", "user_favorites_count", "user_product_favorite"], allEntries = true)
  fun toggleFavorite(userId: Long, productId: Long): Boolean {
    return if (isProductFavorited(userId, productId)) {
      // Cannot remove favorites anymore - just return false indicating no change
      false
    } else {
      addToFavorites(userId, productId)
      true
    }
  }

  
  /**
   * Get user's favorite entities (not products) for internal operations
   */
  fun getUserFavoriteEntities(userId: Long, pageable: Pageable): Page<ProductFavoriteModel> {
    return productFavoriteRepository.findByUserId(userId, pageable)
  }
}
