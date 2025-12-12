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

import dev.yidafu.aqua.order.domain.model.DomainEventModel
import dev.yidafu.aqua.order.domain.model.EventStatusModel
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Enhanced DomainEventRepository implementation with modern Spring Data JPA 3.0+ features
 * Handles event-driven architecture with optimized query patterns and proper locking
 */
@Repository
@Transactional
class DomainEventRepositoryImpl(
    @PersistenceContext private val entityManager: EntityManager
) {

    /**
     * Find next pending event for processing with pessimistic locking
     * Uses native query for optimal performance with row-level locking
     */
    fun findNextPendingEventForUpdateEnhanced(
        status: EventStatusModel,
        now: LocalDateTime
    ): DomainEventModel? {
        // Native query with explicit pessimistic locking
        val query = entityManager.createQuery(
            """
            SELECT de FROM DomainEventModel de
            WHERE de.status = :status
            AND (de.nextRunAt <= :now OR de.nextRunAt IS NULL)
            ORDER BY de.createdAt ASC
            """.trimIndent(),
          DomainEventModel::class.java
        )

        query.setParameter("status", status)
        query.setParameter("now", now)
        query.maxResults = 1
        query.lockMode = LockModeType.PESSIMISTIC_WRITE

        return query.singleResult
    }

    /**
     * Find pending events with flexible filtering using Criteria API
     * More flexible than simple status-based queries
     */
    fun findPendingEventsWithFilters(
        status: EventStatusModel,
        now: LocalDateTime,
        eventType: String? = null,
        maxRetries: Int? = null,
        batchSize: Int = 100
    ): List<DomainEventModel> {
        // TODO: Fix Criteria API usage
        return emptyList()
    }

    /**
     * Batch update events with retry increment and status change
     * Efficient bulk operation for event processing
     */
    fun batchUpdateEvents(
        eventIds: List<Long>,
        newStatus: EventStatusModel,
        incrementRetry: Boolean = false,
        nextRunAt: LocalDateTime? = null
    ): Int {
        val cb = entityManager.criteriaBuilder
        val update = cb.createCriteriaUpdate(DomainEventModel::class.java)
        val root = update.from(DomainEventModel::class.java)

        val idPredicate = root.get<Long>("id").`in`(eventIds)
        update.set(root.get<EventStatusModel>("status"), newStatus)

        if (incrementRetry) {
            // Add 1 to existing retry count
            update.set(
                root.get<Int>("retryCount"),
                cb.sum(root.get<Int>("retryCount"), 1)
            )
        }

        nextRunAt?.let {
            update.set(root.get<LocalDateTime>("nextRunAt"), it)
        }

        update.set(root.get<LocalDateTime>("updatedAt"), LocalDateTime.now())
        update.where(idPredicate)

        return entityManager.createQuery(update).executeUpdate()
    }

    /**
     * Find events created within a time range with specific filters
     * Demonstrates complex Criteria API usage
     */
    fun findEventsInTimeRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        eventTypes: List<String>? = null,
        statuses: List<EventStatusModel>? = null
    ): List<DomainEventModel> {
        // TODO: Fix Criteria API usage
        return emptyList()
    }

    /**
     * Count events by type and status for monitoring and reporting
     */
    fun countEventsByTypeAndStatus(
        eventType: String,
        status: EventStatusModel,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): Long {
        // TODO: Fix Criteria API usage
        return 0L
    }

    /**
     * Native query for complex event analytics and monitoring
     */
    fun getEventProcessingAnalytics(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventAnalyticsRow> {
        val query = entityManager.createNativeQuery(
            """
            SELECT
                DATE(de.created_at) as event_date,
                de.event_type,
                de.status,
                COUNT(*) as event_count,
                AVG(de.retry_count) as avg_retries,
                MAX(de.retry_count) as max_retries,
                MIN(de.next_run_at) as earliest_next_run,
                COUNT(CASE WHEN de.status = 'PROCESSED' THEN 1 END) as processed_count
            FROM domain_events de
            WHERE de.created_at BETWEEN :startDate AND :endDate
            GROUP BY DATE(de.created_at), de.event_type, de.status
            ORDER BY event_date DESC, de.event_type, de.status
            """.trimIndent()
        )

        query.setParameter("startDate", startDate)
        query.setParameter("endDate", endDate)

        val results = query.resultList as Array<Array<Any>>
        return results.map { row ->
            EventAnalyticsRow(
                eventDate = row[0] as java.time.LocalDate,
                eventType = row[1] as String,
                status = EventStatusModel.valueOf(row[2] as String),
                eventCount = (row[3] as Number).toLong(),
                averageRetries = (row[4] as Number).toDouble(),
                maxRetries = (row[5] as Number).toInt(),
                earliestNextRun = row[6] as LocalDateTime?,
                processedCount = (row[7] as Number).toLong()
            )
        }
    }

    /**
     * Clean up processed events older than specified time
     * Maintenance operation for event table
     */
    fun cleanupProcessedEvents(olderThan: LocalDateTime): Int {
        val cb = entityManager.criteriaBuilder
        val delete = cb.createCriteriaDelete(DomainEventModel::class.java)
        val root = delete.from(DomainEventModel::class.java)

        delete.where(
            cb.and(
                cb.equal(root.get<EventStatusModel>("status"), EventStatusModel.COMPLETED),
                cb.lessThan(root.get<LocalDateTime>("createdAt"), olderThan)
            )
        )

        return entityManager.createQuery(delete).executeUpdate()
    }
}

/**
 * Data class for event analytics results
 */
data class EventAnalyticsRow(
    val eventDate: java.time.LocalDate,
    val eventType: String,
    val status: EventStatusModel,
    val eventCount: Long,
    val averageRetries: Double,
    val maxRetries: Int,
    val earliestNextRun: LocalDateTime?,
    val processedCount: Long
)
