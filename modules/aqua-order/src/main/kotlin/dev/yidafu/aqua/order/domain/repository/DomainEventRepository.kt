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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import jakarta.persistence.LockModeType
import java.util.*

@Repository
interface DomainEventRepository : JpaRepository<DomainEvent, Long>, JpaSpecificationExecutor<DomainEvent> {
  fun findByEventTypeAndStatus(
    eventType: String,
    status: EventStatus,
  ): List<DomainEvent>

  fun findByStatus(status: EventStatus): List<DomainEvent>

  fun findByStatusIn(statuses: List<EventStatus>): List<DomainEvent>

  fun findPendingEvents(
    status: EventStatus,
    now: LocalDateTime,
  ): List<DomainEvent> {
    val specification = DomainEventSpecifications.byStatus(status)
      .and(DomainEventSpecifications.nextRunAtBeforeOrIsNull(now))
    return findAll(specification, Sort.by(Sort.Direction.ASC, "createdAt"))
  }

  // PESSIMISTIC_SKIP_LOCKED
  @Lock(LockModeType.PESSIMISTIC_READ)
  fun findNextPendingEventForUpdate(
    status: EventStatus,
    now: LocalDateTime,
  ): DomainEvent? {
    val specification = DomainEventSpecifications.byStatus(status)
      .and(DomainEventSpecifications.nextRunAtBeforeOrIsNull(now))
    val pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "createdAt"))
    val page = findAll(specification, pageable)
    return page.content.firstOrNull()
  }

  fun countByEventTypeAndStatusAndCreatedAtBetween(
    eventType: String,
    status: EventStatus,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    val specification = DomainEventSpecifications.byEventTypeAndStatus(eventType, status)
      .and(DomainEventSpecifications.createdAtBetween(startDate, endDate))
    return count(specification)
  }

  fun countDeadLetterEvents(
    maxRetries: Int,
    status: EventStatus,
  ): Long {
    val specification = DomainEventSpecifications.retryCountGreaterThanOrEqualTo(maxRetries)
      .and(DomainEventSpecifications.byStatus(status))
    return count(specification)
  }

  fun deleteByStatusAndCreatedAtBefore(
    status: EventStatus,
    before: LocalDateTime,
  ) {
    val specification = DomainEventSpecifications.byStatus(status)
      .and(Specification { root, _, cb ->
        cb.lessThan(root.get<LocalDateTime>("createdAt"), before)
      })
    val events = findAll(specification)
    deleteAll(events)
  }

    // Enhanced query methods using modern Spring Data JPA 3.0+ features
    fun findNextPendingEventForUpdateEnhanced(
        status: EventStatus,
        now: LocalDateTime
    ): DomainEvent?

    fun findPendingEventsWithFilters(
        status: EventStatus,
        now: LocalDateTime,
        eventType: String? = null,
        maxRetries: Int? = null,
        batchSize: Int = 100
    ): List<DomainEvent>

    fun batchUpdateEvents(
        eventIds: List<Long>,
        newStatus: EventStatus,
        incrementRetry: Boolean = false,
        nextRunAt: LocalDateTime? = null
    ): Int

    fun findEventsInTimeRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        eventTypes: List<String>? = null,
        statuses: List<EventStatus>? = null
    ): List<DomainEvent>

    fun countEventsByTypeAndStatus(
        eventType: String,
        status: EventStatus,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Long

    fun getEventProcessingAnalytics(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventAnalyticsRow>

    fun cleanupProcessedEvents(olderThan: LocalDateTime): Int
}
