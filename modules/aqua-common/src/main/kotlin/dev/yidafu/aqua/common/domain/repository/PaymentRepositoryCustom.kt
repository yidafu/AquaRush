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

package dev.yidafu.aqua.common.domain.repository

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Custom repository interface for enhanced Payment queries
 * This interface defines methods that require custom implementation
 */
interface PaymentRepositoryCustom {
  /**
   * Find payments by user ID and status with enhanced query
   */
  fun findByUserIdAndStatusEnhanced(
    userId: Long,
    status: PaymentStatus,
  ): List<PaymentModel>

  /**
   * Find expired payments before specified time
   */
  fun findExpiredPaymentsEnhanced(now: LocalDateTime): List<PaymentModel>

  /**
   * Find payments created within a date range
   */
  fun findByCreatedAtBetweenEnhanced(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel>

  /**
   * Count payments by status and creation date range
   */
  fun countByStatusAndCreatedAtBetweenEnhanced(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long

  /**
   * Sum payment amounts by status and creation date range
   */
  fun sumAmountByStatusAndCreatedAtBetweenEnhanced(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): BigDecimal

  /**
   * Complex query: find payments with multiple criteria
   */
  fun findPaymentsWithFilters(
    userId: Long? = null,
    status: PaymentStatus? = null,
    transactionId: String? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    minAmount: BigDecimal? = null,
    maxAmount: BigDecimal? = null,
  ): List<PaymentModel>
}
