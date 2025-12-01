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

import dev.yidafu.aqua.common.domain.model.Payment
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment>, PaymentRepositoryCustom {
  fun findByOrderId(orderId: Long): Optional<Payment>

  fun findByTransactionId(transactionId: String): Optional<Payment>

  fun findByPrepayId(prepayId: String): Optional<Payment>

  fun findByUserIdAndStatus(
    userId: Long,
    status: PaymentStatus,
  ): List<Payment>

  fun findByStatus(status: PaymentStatus): List<Payment>

  fun findByStatusIn(statuses: List<PaymentStatus>): List<Payment>

  fun findExpiredPayments(
    status: PaymentStatus,
    now: LocalDateTime,
  ): List<Payment> {
    val specification = PaymentSpecifications.expiredBefore(now)
    return findAll(specification)
  }

  fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<Payment> {
    val specification = PaymentSpecifications.createdAtBetween(startDate, endDate)
    return findAll(specification)
  }

  fun countByStatusAndCreatedAtBetween(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    val specification = PaymentSpecifications.byStatusAndCreatedAtBetween(status, startDate, endDate)
    return count(specification)
  }

  fun sumAmountByStatusAndCreatedAtBetween(
    status: PaymentStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): BigDecimal {
    val specification = PaymentSpecifications.byStatusAndCreatedAtBetween(status, startDate, endDate)
    val payments = findAll(specification)
    return payments.fold(BigDecimal.ZERO) { sum, payment -> sum + payment.amount }
  }
}