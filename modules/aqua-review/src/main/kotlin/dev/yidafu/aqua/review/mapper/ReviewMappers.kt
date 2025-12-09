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

package dev.yidafu.aqua.review.mapper

import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatistics
import dev.yidafu.aqua.review.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.review.domain.model.ReviewModel
import tech.mappie.api.ObjectMappie

/**
 * Mapper for converting Review domain entity to GraphQL Review type
 */
object ReviewMapper : ObjectMappie<ReviewModel, dev.yidafu.aqua.common.graphql.generated.Review>() {
    override fun map(from: ReviewModel): dev.yidafu.aqua.common.graphql.generated.Review {
        return dev.yidafu.aqua.common.graphql.generated.Review(
            reviewId = from.id!!,
            orderId = from.orderId,
            userId = if (from.isAnonymous) null else from.userId,
            deliveryWorkerId = from.deliveryWorkerId,
            deliveryWorkerName = null, // This would need to be fetched from delivery worker repository
            rating = from.rating,
            comment = from.comment,
            isAnonymous = from.isAnonymous,
            createdAt = from.createdAt
        )
    }
}

/**
 * Mapper for converting DeliveryWorkerStatistics domain entity to GraphQL DeliveryWorkerStatistics type
 * Note: This mapper will be used when the GraphQL schema is regenerated to include DeliveryWorkerStatistics type
 */
object DeliveryWorkerStatisticsMapper : ObjectMappie<DeliveryWorkerStatisticsModel, DeliveryWorkerStatistics>() {
    override fun map(from: DeliveryWorkerStatisticsModel): DeliveryWorkerStatistics = mapping {  }
}
