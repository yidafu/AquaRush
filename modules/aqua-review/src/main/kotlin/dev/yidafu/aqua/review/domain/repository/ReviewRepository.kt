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

import dev.yidafu.aqua.review.domain.model.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ReviewRepository : JpaRepository<Review, Long> {

    fun findByOrderId(orderId: Long): Review?

    fun existsByOrderId(orderId: Long): Boolean

    fun findByUserId(userId: Long): List<Review>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Review>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Review>

    fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<Review>

    fun findByDeliveryWorkerIdOrderByCreatedAtDesc(
        deliveryWorkerId: Long,
        pageable: Pageable
    ): Page<Review>

    fun findByDeliveryWorkerIdAndRatingOrderByCreatedAtDesc(
        deliveryWorkerId: Long,
        rating: Int,
        pageable: Pageable
    ): Page<Review>

    fun findByDeliveryWorkerIdAndCreatedAtBetween(
        deliveryWorkerId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Review>

    @Query("""
        SELECT r
        FROM Review r
        WHERE (:deliveryWorkerId IS NULL OR r.deliveryWorkerId = :deliveryWorkerId)
        AND (:minRating IS NULL OR r.rating >= :minRating)
        AND (:maxRating IS NULL OR r.rating <= :maxRating)
        AND (:dateFrom IS NULL OR r.createdAt >= :dateFrom)
        AND (:dateTo IS NULL OR r.createdAt <= :dateTo)
        AND (:userId IS NULL OR r.userId = :userId)
        ORDER BY r.createdAt DESC
    """)
    fun findReviewsWithFilters(
        @Param("deliveryWorkerId") deliveryWorkerId: Long? = null,
        @Param("minRating") minRating: Int? = null,
        @Param("maxRating") maxRating: Int? = null,
        @Param("dateFrom") dateFrom: LocalDateTime? = null,
        @Param("dateTo") dateTo: LocalDateTime? = null,
        @Param("userId") userId: Long? = null,
        pageable: Pageable
    ): Page<Review>

    @Query("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.deliveryWorkerId = :deliveryWorkerId
        AND r.rating = :rating
    """)
    fun countByDeliveryWorkerIdAndRating(
        @Param("deliveryWorkerId") deliveryWorkerId: Long,
        @Param("rating") rating: Int
    ): Long

    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.deliveryWorkerId = :deliveryWorkerId
    """)
    fun findAverageRatingByDeliveryWorkerId(@Param("deliveryWorkerId") deliveryWorkerId: Long): Double?

    fun countByDeliveryWorkerId(deliveryWorkerId: Long): Long

    fun countByDeliveryWorkerIdAndCreatedAtBetween(
        deliveryWorkerId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long
}
