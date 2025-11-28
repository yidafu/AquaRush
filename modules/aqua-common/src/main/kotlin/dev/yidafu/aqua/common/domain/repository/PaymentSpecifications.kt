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
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentSpecifications {
    companion object {
        fun byOrderId(orderId: Long): Specification<Payment> {
            return Specification { root, _, cb ->
                cb.equal(root.get<Long>("orderId"), orderId)
            }
        }

        fun byTransactionId(transactionId: String): Specification<Payment> {
            return Specification { root, _, cb ->
                cb.equal(root.get<String>("transactionId"), transactionId)
            }
        }

        fun byPrepayId(prepayId: String): Specification<Payment> {
            return Specification { root, _, cb ->
                cb.equal(root.get<String>("prepayId"), prepayId)
            }
        }

        fun byUserIdAndStatus(userId: Long, status: PaymentStatus): Specification<Payment> {
            return Specification { root, _, cb ->
                val userIdPredicate = cb.equal(root.get<Long>("userId"), userId)
                val statusPredicate = cb.equal(root.get<Enum<*>>("status"), status)
                cb.and(userIdPredicate, statusPredicate)
            }
        }

        fun byStatus(status: PaymentStatus): Specification<Payment> {
            return Specification { root, _, cb ->
                cb.equal(root.get<Enum<*>>("status"), status)
            }
        }

        fun byStatuses(statuses: List<PaymentStatus>): Specification<Payment> {
            return Specification { root, _, cb ->
                root.get<Enum<*>>("status").`in`(statuses)
            }
        }

        fun expiredBefore(now: LocalDateTime): Specification<Payment> {
            return Specification { root, _, cb ->
                val statusPredicate = cb.equal(root.get<Enum<*>>("status"), PaymentStatus.PENDING)
                val expiredPredicate = cb.lessThan(root.get<LocalDateTime>("expiredAt"), now)
                cb.and(statusPredicate, expiredPredicate)
            }
        }

        fun createdAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Specification<Payment> {
            return Specification { root, _, cb ->
                val startPredicate = cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), startDate)
                val endPredicate = cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), endDate)
                cb.and(startPredicate, endPredicate)
            }
        }

        fun byStatusAndCreatedAtBetween(status: PaymentStatus, startDate: LocalDateTime, endDate: LocalDateTime): Specification<Payment> {
            return Specification { root, _, cb ->
                val statusPredicate = cb.equal(root.get<Enum<*>>("status"), status)
                val startPredicate = cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), startDate)
                val endPredicate = cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), endDate)
                cb.and(statusPredicate, startPredicate, endPredicate)
            }
        }
    }
}
