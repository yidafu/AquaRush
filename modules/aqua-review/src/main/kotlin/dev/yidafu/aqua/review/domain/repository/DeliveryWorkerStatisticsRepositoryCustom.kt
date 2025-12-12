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

import dev.yidafu.aqua.review.domain.model.DeliveryWorkerStatisticsModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Custom repository interface for DeliveryWorkerStatistics entity with QueryDSL implementations
 */
interface DeliveryWorkerStatisticsRepositoryCustom {
  /**
   * Find delivery workers ranking with dynamic sorting
   * @param sortBy the sorting criteria ('rating' or 'reviews')
   * @param minReviews minimum number of reviews required
   * @param pageable pagination information
   * @return paginated list of delivery workers sorted by criteria
   */
  fun findDeliveryWorkersRanking(
    sortBy: String = "rating",
    minReviews: Int = 1,
    pageable: Pageable
  ): Page<DeliveryWorkerStatisticsModel>

  /**
   * Find delivery workers within a specific rating range
   * @param minRating minimum rating
   * @param maxRating maximum rating
   * @param minReviews minimum number of reviews required
   * @param maxReviews maximum number of reviews (optional)
   * @param sortBy the sorting criteria ('rating' or 'reviews')
   * @param pageable pagination information
   * @return paginated list of delivery workers within rating range
   */
  fun findDeliveryWorkersByRatingRange(
    minRating: Double,
    maxRating: Double,
    minReviews: Int = 1,
    maxReviews: Int? = null,
    sortBy: String = "rating",
    pageable: Pageable
  ): Page<DeliveryWorkerStatisticsModel>

  /**
   * Count delivery workers with minimum rating
   * @param minRating minimum rating
   * @param minReviews minimum number of reviews required
   * @return count of delivery workers meeting criteria
   */
  fun countDeliveryWorkersByMinRating(
    minRating: Double,
    minReviews: Int = 1
  ): Long

  /**
   * Find overall average rating across all delivery workers
   * @param minReviews minimum number of reviews required to be included
   * @return average rating or null if no workers meet criteria
   */
  fun findOverallAverageRating(minReviews: Int = 1): Double?
}