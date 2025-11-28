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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.order.domain.model.DomainEvent
import dev.yidafu.aqua.order.domain.model.EventStatus
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime

class DomainEventSpecifications {
    companion object {
        fun byEventTypeAndStatus(eventType: String, status: EventStatus): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                val eventTypePredicate = cb.equal(root.get<String>("eventType"), eventType)
                val statusPredicate = cb.equal(root.get<Enum<*>>("status"), status)
                cb.and(eventTypePredicate, statusPredicate)
            }
        }

        fun byStatus(status: EventStatus): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                cb.equal(root.get<Enum<*>>("status"), status)
            }
        }

        fun byStatuses(statuses: List<EventStatus>): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                root.get<Enum<*>>("status").`in`(statuses)
            }
        }

        fun nextRunAtBeforeOrIsNull(dateTime: LocalDateTime): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                cb.or(
                    cb.isNull(root.get<LocalDateTime>("nextRunAt")),
                    cb.lessThanOrEqualTo(root.get<LocalDateTime>("nextRunAt"), dateTime)
                )
            }
        }

        fun retryCountGreaterThanOrEqualTo(minRetries: Int): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                cb.greaterThanOrEqualTo(root.get<Int>("retryCount"), minRetries)
            }
        }

        fun createdAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                val startPredicate = cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), startDate)
                val endPredicate = cb.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), endDate)
                cb.and(startPredicate, endPredicate)
            }
        }

        fun byEventType(eventType: String): Specification<DomainEvent> {
            return Specification { root, _, cb ->
                cb.equal(root.get<String>("eventType"), eventType)
            }
        }
    }
}
