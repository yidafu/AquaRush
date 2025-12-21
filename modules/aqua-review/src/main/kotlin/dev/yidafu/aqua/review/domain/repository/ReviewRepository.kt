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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ReviewRepository : JpaRepository<ReviewModel, Long>, ReviewRepositoryCustom {
  fun findByOrderId(orderId: Long): ReviewModel?

  fun existsByOrderId(orderId: Long): Boolean

  fun findByUserId(userId: Long): List<ReviewModel>

  fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<ReviewModel>

  fun findByUserIdOrderByCreatedAtDesc(
    userId: Long,
    pageable: Pageable,
  ): Page<ReviewModel>

  fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<ReviewModel>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(
    deliveryWorkerId: Long,
    pageable: Pageable,
  ): Page<ReviewModel>

  fun findByDeliveryWorkerIdAndRatingOrderByCreatedAtDesc(
    deliveryWorkerId: Long,
    rating: Int,
    pageable: Pageable,
  ): Page<ReviewModel>

  fun findByDeliveryWorkerIdAndCreatedAtBetween(
    deliveryWorkerId: Long,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReviewModel>

  fun countByDeliveryWorkerId(deliveryWorkerId: Long): Long

  fun countByDeliveryWorkerIdAndCreatedAtBetween(
    deliveryWorkerId: Long,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long
}
