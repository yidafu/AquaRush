/**
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
 * 商品收藏仓储(JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,
 * 仅保留实际被调用的方法;查询过滤 is_deleted)。
 */
interface ProductFavoriteRepository {
  fun findByUserIdAndProductId(
    userId: Long,
    productId: Long,
  ): ProductFavoriteModel?

  fun findByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<ProductFavoriteModel>

  fun findByProductId(productId: Long): List<ProductFavoriteModel>

  fun count(): Long

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: ProductFavoriteModel): ProductFavoriteModel

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
  fun findFavoriteIdsByUserId(
    userId: Long,
    pageable: Pageable,
  ): Page<Long>

  /**
   * Check if a favorite exists by user ID and product ID
   * @param userId the user ID
   * @param productId the product ID
   * @return true if exists, false otherwise
   */
  fun existsByUserIdAndProductId(
    userId: Long,
    productId: Long,
  ): Boolean

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
  fun updateEnableStatus(
    userId: Long,
    productId: Long,
    enable: Boolean,
  ): Int

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
    val favoriteCount: Long,
    /** 该商品最后一次被收藏的时间(SQL `max(created_at)`,避免逐商品再查明细)。 */
    val lastFavoritedAt: LocalDateTime? = null,
  )

  /**
   * 按收藏数分页查询(SQL 侧 group by + having + limit/offset;收藏数降序,商品 id 升序破平)。
   * @param pageable 分页参数
   * @param minFavorites 最少收藏数过滤(null=不过滤,下推到 HAVING)
   * @return 当前页 + 分组总数(禁止在内存里切页)
   */
  fun findMostFavoritedProducts(
    pageable: Pageable,
    minFavorites: Long? = null,
  ): Page<ProductFavoriteCount>

  /**
   * 取收藏数最高的 [limit] 个商品(SQL 侧 limit,统计展示用)
   */
  fun findTopFavoritedProducts(limit: Int): List<ProductFavoriteCount>

  /** 有收藏记录的商品数(SQL `count(distinct product_id)`) */
  fun countFavoritedProducts(): Long

  /** 收藏总条数(SQL `count(*)`) */
  fun countTotalFavorites(): Long

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
    dateTo: LocalDateTime?,
    afterId: Long?,
    limit: Int,
  ): List<ProductFavoriteModel>

  // Statistics for specific periods

  /**
   * Get average favorites per user
   * @return average favorites per user
   */
  fun getAverageFavoritesPerUser(): Double
}
