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

package dev.yidafu.aqua.common.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

open class DeliveryWorkerStatisticsModel(
  var id: Long? = null,
  val deliveryWorkerId: Long = -1L,
  var averageRating: BigDecimal = BigDecimal.ZERO,
  var totalReviews: Int = 0,
  var oneStarReviews: Int = 0,
  var twoStarReviews: Int = 0,
  var threeStarReviews: Int = 0,
  var fourStarReviews: Int = 0,
  var fiveStarReviews: Int = 0,
  var lastUpdated: LocalDateTime = LocalDateTime.now(),
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
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

  fun getRatingDistribution(): Map<Int, Int> =
    mapOf(
      1 to oneStarReviews,
      2 to twoStarReviews,
      3 to threeStarReviews,
      4 to fourStarReviews,
      5 to fiveStarReviews,
    )
}
