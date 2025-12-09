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

package dev.yidafu.aqua.common.service

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.repository.PaymentRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Enhanced payment service demonstrating modern Spring Data JPA 3.0+ query techniques
 * This service showcases the migration from Specification pattern to more efficient, type-safe queries
 * Note: @Service annotation temporarily removed to avoid dependency injection issues
 */
// @Service  // Temporarily disabled to avoid PaymentRepository dependency issues
@Transactional(readOnly = true)
class EnhancedPaymentService(
    private val paymentRepository: PaymentRepository,
    @PersistenceContext private val entityManager: EntityManager
) {

    /**
     * Find pending payments that are expired
     * Uses database-level aggregation for better performance
     */
    fun findExpiredPendingPayments(now: LocalDateTime): List<PaymentModel> {
        // Using the enhanced repository method with Criteria API
        return paymentRepository.findExpiredPaymentsEnhanced(now)
    }

    /**
     * Get payment statistics for a specific status within a date range
     * Leverages database aggregation for optimal performance
     */
    fun getPaymentStatistics(
        status: PaymentStatus,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): PaymentStatistics {
        val count = paymentRepository.countByStatusAndCreatedAtBetweenEnhanced(status, startDate, endDate)
        val totalAmount = paymentRepository.sumAmountByStatusAndCreatedAtBetweenEnhanced(status, startDate, endDate)

        return PaymentStatistics(
            count = count,
            totalAmount = totalAmount,
            averageAmount = if (count > 0) totalAmount.divide(BigDecimal.valueOf(count), 2) else BigDecimal.ZERO
        )
    }

    /**
     * Find payments with flexible filtering criteria
     * Demonstrates complex query building with Spring Data JPA 3.0+
     */
    fun findPaymentsWithFlexibleFilters(
        userId: Long? = null,
        status: PaymentStatus? = null,
        transactionId: String? = null,
        dateRange: DateRange? = null,
        amountRange: AmountRange? = null
    ): List<PaymentModel> {
        return paymentRepository.findPaymentsWithFilters(
            userId = userId,
            status = status,
            transactionId = transactionId,
            startDate = dateRange?.start,
            endDate = dateRange?.end,
            minAmount = amountRange?.min,
            maxAmount = amountRange?.max
        )
    }

    /**
     * Batch update payment status for multiple payments
     * Shows efficient bulk operations with Spring Data JPA
     */
    @Transactional
    fun batchUpdatePaymentStatus(paymentIds: List<Long>, newStatus: PaymentStatus): Int {
        val cb = entityManager.criteriaBuilder
        val update = cb.createCriteriaUpdate(PaymentModel::class.java)
        val root = update.root

        val idPredicate = root.get<Long>("id").`in`(paymentIds)
        update.set(root.get<PaymentStatus>("status"), newStatus)
        update.where(idPredicate)

        return entityManager.createQuery(update).executeUpdate()
    }

    /**
     * Native query for complex reporting when Criteria API is not sufficient
     */
    fun getPaymentReportByMonth(year: Int, month: Int): List<PaymentReportRow> {
        val query = entityManager.createNativeQuery(
            """
            SELECT
                EXTRACT(MONTH FROM p.created_at) as month,
                p.status,
                COUNT(*) as payment_count,
                SUM(p.amount) as total_amount,
                AVG(p.amount) as average_amount
            FROM payments p
            WHERE EXTRACT(YEAR FROM p.created_at) = :year
                AND EXTRACT(MONTH FROM p.created_at) = :month
            GROUP BY EXTRACT(MONTH FROM p.created_at), p.status
            ORDER BY month, p.status
            """.trimIndent()
        )

        query.setParameter("year", year)
        query.setParameter("month", month)

        val results = query.resultList as Array<Array<Any>>
        return results.map { row ->
            PaymentReportRow(
                month = (row[0] as Number).toInt(),
                status = PaymentStatus.valueOf(row[1] as String),
                count = (row[2] as Number).toLong(),
                totalAmount = BigDecimal.valueOf((row[3] as Number).toDouble()),
                averageAmount = BigDecimal.valueOf((row[4] as Number).toDouble())
            )
        }
    }
}

/**
 * Data classes for enhanced query results
 */
data class PaymentStatistics(
    val count: Long,
    val totalAmount: BigDecimal,
    val averageAmount: BigDecimal
)

data class DateRange(
    val start: LocalDateTime,
    val end: LocalDateTime
)

data class AmountRange(
    val min: BigDecimal,
    val max: BigDecimal
)

data class PaymentReportRow(
    val month: Int,
    val status: PaymentStatus,
    val count: Long,
    val totalAmount: BigDecimal,
    val averageAmount: BigDecimal
)
