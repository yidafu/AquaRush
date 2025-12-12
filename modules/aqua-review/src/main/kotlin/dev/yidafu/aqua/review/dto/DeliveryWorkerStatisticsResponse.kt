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

package dev.yidafu.aqua.review.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 配送员评分统计响应DTO
 */
data class DeliveryWorkerStatisticsResponse(
  @field:JsonProperty("deliveryWorkerId")
  val deliveryWorkerId: Long,
  @field:JsonProperty("workerName")
  val workerName: String? = null,
  @field:JsonProperty("averageRating")
  val averageRating: BigDecimal,
  @field:JsonProperty("totalReviews")
  val totalReviews: Int,
  @field:JsonProperty("oneStarReviews")
  val oneStarReviews: Int,
  @field:JsonProperty("twoStarReviews")
  val twoStarReviews: Int,
  @field:JsonProperty("threeStarReviews")
  val threeStarReviews: Int,
  @field:JsonProperty("fourStarReviews")
  val fourStarReviews: Int,
  @field:JsonProperty("fiveStarReviews")
  val fiveStarReviews: Int,
  @field:JsonProperty("ratingDistribution")
  val ratingDistribution: Map<Int, Int>,
  @field:JsonProperty("lastUpdated")
  val lastUpdated: LocalDateTime,
) {
  /**
   * 获取好评率 (4星及以上)
   */
  @get:JsonProperty("positiveRatingPercentage")
  val positiveRatingPercentage: Double
    get() =
      if (totalReviews > 0) {
        ((fourStarReviews + fiveStarReviews).toDouble() / totalReviews * 100)
      } else {
        0.0
      }

  /**
   * 获取五星好评率
   */
  @get:JsonProperty("fiveStarRatingPercentage")
  val fiveStarRatingPercentage: Double
    get() =
      if (totalReviews > 0) {
        (fiveStarReviews.toDouble() / totalReviews * 100)
      } else {
        0.0
      }
}
