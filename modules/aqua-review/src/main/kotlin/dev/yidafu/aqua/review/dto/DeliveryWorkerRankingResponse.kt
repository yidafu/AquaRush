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

/**
 * 配送员排行榜响应DTO
 */
data class DeliveryWorkerRankingResponse(
    @field:JsonProperty("workerId")
    val workerId: Long,

    @field:JsonProperty("workerName")
    val workerName: String? = null,

    @field:JsonProperty("averageRating")
    val averageRating: BigDecimal,

    @field:JsonProperty("totalReviews")
    val totalReviews: Int,

    @field:JsonProperty("rank")
    val rank: Int? = null,

    @field:JsonProperty("positiveRatingPercentage")
    val positiveRatingPercentage: Double
)
