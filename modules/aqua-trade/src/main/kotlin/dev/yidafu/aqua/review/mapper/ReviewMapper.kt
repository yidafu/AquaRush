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

package dev.yidafu.aqua.review.mapper

import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.graphql.generated.Review

/**
 * ReviewModel → GraphQL Review(原 ReviewMapper ObjectMappie 迁移,F-ORD-007 一单一评)。
 * deliveryWorkerName 不在 ReviewModel 上,恒为 null,由联查展示填充。
 */
fun ReviewModel.toGraphQL(): Review =
  Review(
    reviewId = id ?: -1L,
    orderId = orderId,
    userId = userId,
    deliveryWorkerId = deliveryWorkerId,
    rating = rating,
    comment = comment,
    isAnonymous = isAnonymous,
    createdAt = createdAt,
    // Not available in ReviewModel
    deliveryWorkerName = null,
  )
