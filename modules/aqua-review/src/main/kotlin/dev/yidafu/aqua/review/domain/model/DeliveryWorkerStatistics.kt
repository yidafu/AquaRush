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

package dev.yidafu.aqua.review.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
  name = "delivery_worker_statistics",
  uniqueConstraints = [
    UniqueConstraint(name = "uk_delivery_worker_id", columnNames = ["delivery_worker_id"]),
  ],
)
data class DeliveryWorkerStatisticsModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "delivery_worker_id", nullable = false, unique = true)
  val deliveryWorkerId: Long,
  @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
  var averageRating: BigDecimal = BigDecimal.ZERO,
  @Column(name = "total_reviews", nullable = false)
  var totalReviews: Int = 0,
  @Column(name = "one_star_reviews", nullable = false)
  var oneStarReviews: Int = 0,
  @Column(name = "two_star_reviews", nullable = false)
  var twoStarReviews: Int = 0,
  @Column(name = "three_star_reviews", nullable = false)
  var threeStarReviews: Int = 0,
  @Column(name = "four_star_reviews", nullable = false)
  var fourStarReviews: Int = 0,
  @Column(name = "five_star_reviews", nullable = false)
  var fiveStarReviews: Int = 0,
  @Column(name = "last_updated", nullable = false)
  var lastUpdated: LocalDateTime = LocalDateTime.now(),
) {
  @PreUpdate
  fun preUpdate() {
    lastUpdated = LocalDateTime.now()
  }

  fun updateStatistics(newRating: Int) {
    totalReviews++

    when (newRating) {
      1 -> oneStarReviews++
      2 -> twoStarReviews++
      3 -> threeStarReviews++
      4 -> fourStarReviews++
      5 -> fiveStarReviews++
    }

    // Recalculate average rating
    val totalRatingPoints =
      oneStarReviews +
        (twoStarReviews * 2) +
        (threeStarReviews * 3) +
        (fourStarReviews * 4) +
        (fiveStarReviews * 5)

    averageRating =
      if (totalReviews > 0) {
        BigDecimal(totalRatingPoints).divide(BigDecimal(totalReviews), 2, java.math.RoundingMode.HALF_UP)
      } else {
        BigDecimal.ZERO
      }
  }

  fun getRatingDistribution(): Map<Int, Int> {
    return mapOf(
      1 to oneStarReviews,
      2 to twoStarReviews,
      3 to threeStarReviews,
      4 to fourStarReviews,
      5 to fiveStarReviews,
    )
  }
}
