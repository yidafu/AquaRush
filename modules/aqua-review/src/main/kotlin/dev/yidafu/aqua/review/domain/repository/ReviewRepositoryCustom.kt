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

import dev.yidafu.aqua.common.domain.model.ReviewModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * Custom repository interface for Review entity with QueryDSL implementations
 */
interface ReviewRepositoryCustom {
  /**
   * Find reviews with multiple filters
   * @param deliveryWorkerId the delivery worker ID (optional)
   * @param minRating the minimum rating (optional)
   * @param maxRating the maximum rating (optional)
   * @param dateFrom the start date (optional)
   * @param dateTo the end date (optional)
   * @param userId the user ID (optional)
   * @param pageable pagination information
   * @return page of reviews matching the filters
   */
  fun findReviewsWithFilters(
    deliveryWorkerId: Long? = null,
    minRating: Int? = null,
    maxRating: Int? = null,
    dateFrom: LocalDateTime? = null,
    dateTo: LocalDateTime? = null,
    userId: Long? = null,
    pageable: Pageable,
  ): Page<ReviewModel>

  /**
   * Count reviews by delivery worker ID and rating
   * @param deliveryWorkerId the delivery worker ID
   * @param rating the rating
   * @return number of reviews with the specified rating
   */
  fun countByDeliveryWorkerIdAndRating(
    deliveryWorkerId: Long,
    rating: Int,
  ): Long

  /**
   * Find average rating for a delivery worker
   * @param deliveryWorkerId the delivery worker ID
   * @return average rating or null if no reviews found
   */
  fun findAverageRatingByDeliveryWorkerId(deliveryWorkerId: Long): Double?
}
