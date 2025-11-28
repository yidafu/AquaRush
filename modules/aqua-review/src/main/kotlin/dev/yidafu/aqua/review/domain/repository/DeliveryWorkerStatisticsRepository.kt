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

import dev.yidafu.aqua.review.domain.model.DeliveryWorkerStatistics
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DeliveryWorkerStatisticsRepository : JpaRepository<DeliveryWorkerStatistics, Long> {

    fun findByDeliveryWorkerId(deliveryWorkerId: Long): DeliveryWorkerStatistics?

    fun existsByDeliveryWorkerId(deliveryWorkerId: Long): Boolean

    @Query("""
        SELECT dws
        FROM DeliveryWorkerStatistics dws
        WHERE dws.totalReviews >= :minReviews
        ORDER BY
        CASE
            WHEN :sortBy = 'rating' THEN dws.averageRating
            WHEN :sortBy = 'reviews' THEN dws.totalReviews
        END DESC,
        dws.lastUpdated DESC
    """)
    fun findDeliveryWorkersRanking(
        @Param("sortBy") sortBy: String = "rating",
        @Param("minReviews") minReviews: Int = 1,
        pageable: Pageable
    ): Page<DeliveryWorkerStatistics>

    @Query("""
        SELECT dws
        FROM DeliveryWorkerStatistics dws
        WHERE dws.averageRating >= :minRating
        AND dws.averageRating <= :maxRating
        AND dws.totalReviews >= :minReviews
        AND (:maxReviews IS NULL OR dws.totalReviews <= :maxReviews)
        ORDER BY
        CASE
            WHEN :sortBy = 'rating' THEN dws.averageRating
            WHEN :sortBy = 'reviews' THEN dws.totalReviews
        END DESC,
        dws.lastUpdated DESC
    """)
    fun findDeliveryWorkersByRatingRange(
        @Param("minRating") minRating: Double,
        @Param("maxRating") maxRating: Double,
        @Param("minReviews") minReviews: Int = 1,
        @Param("maxReviews") maxReviews: Int? = null,
        @Param("sortBy") sortBy: String = "rating",
        pageable: Pageable
    ): Page<DeliveryWorkerStatistics>

    @Query("""
        SELECT COUNT(dws)
        FROM DeliveryWorkerStatistics dws
        WHERE dws.averageRating >= :minRating
        AND dws.totalReviews >= :minReviews
    """)
    fun countDeliveryWorkersByMinRating(
        @Param("minRating") minRating: Double,
        @Param("minReviews") minReviews: Int = 1
    ): Long

    @Query("SELECT AVG(dws.averageRating) FROM DeliveryWorkerStatistics dws WHERE dws.totalReviews >= :minReviews")
    fun findOverallAverageRating(@Param("minReviews") minReviews: Int = 1): Double?

    fun findByTotalReviewsGreaterThanEqual(minReviews: Int, pageable: Pageable): Page<DeliveryWorkerStatistics>
}
