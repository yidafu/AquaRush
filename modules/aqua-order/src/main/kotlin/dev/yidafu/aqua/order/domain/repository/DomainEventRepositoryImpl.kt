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

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.DomainEventModel
import dev.yidafu.aqua.common.domain.model.QDomainEventModel.domainEventModel
import dev.yidafu.aqua.common.domain.model.enums.EventStatusModel
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Enhanced DomainEventRepository implementation using QueryDSL
 * Handles event-driven architecture with optimized query patterns and proper locking
 */
@Repository
@Transactional
class DomainEventRepositoryImpl : DomainEventRepositoryCustom {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val queryFactory: JPAQueryFactory by lazy {
        JPAQueryFactory(entityManager)
    }

    override fun findNextPendingEventForUpdateEnhanced(
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

    override fun findPendingEventsWithFilters(
        status: EventStatusModel,
        now: LocalDateTime,
        eventType: String?,
        maxRetries: Int?,
        batchSize: Int
    ): List<DomainEventModel> {
        val builder = BooleanBuilder()

        builder.and(domainEventModel.status.eq(status))
        builder.and(
            domainEventModel.nextRunAt.loe(now)
                .or(domainEventModel.nextRunAt.isNull)
        )

        eventType?.let { builder.and(domainEventModel.eventType.eq(it)) }
        maxRetries?.let { builder.and(domainEventModel.retryCount.loe(it)) }

        return queryFactory.selectFrom(domainEventModel)
            .where(builder)
            .orderBy(domainEventModel.createdAt.asc())
            .limit(batchSize.toLong())
            .fetch()
    }

    override fun batchUpdateEvents(
        eventIds: List<Long>,
        newStatus: EventStatusModel,
        incrementRetry: Boolean,
        nextRunAt: LocalDateTime?
    ): Int {
        var update = queryFactory.update(domainEventModel)
            .set(domainEventModel.status, newStatus)
            .set(domainEventModel.updatedAt, LocalDateTime.now())
            .where(domainEventModel.id.`in`(eventIds))

        if (incrementRetry) {
            // Note: QueryDSL doesn't support increment expressions directly
            // We need to fetch current values, increment, and update
            val events = queryFactory.selectFrom(domainEventModel)
                .where(domainEventModel.id.`in`(eventIds))
                .fetch()

            events.forEach { event ->
                queryFactory.update(domainEventModel)
                    .set(domainEventModel.retryCount, event.retryCount + 1)
                    .where(domainEventModel.id.eq(event.id))
                    .execute()
            }
        }

        nextRunAt?.let {
            update = update.set(domainEventModel.nextRunAt, it)
        }

        return update.execute().toInt()
    }

    override fun findEventsInTimeRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        eventTypes: List<String>?,
        statuses: List<EventStatusModel>?
    ): List<DomainEventModel> {
        val builder = BooleanBuilder()

        builder.and(domainEventModel.createdAt.between(startDate, endDate))
        eventTypes?.let { builder.and(domainEventModel.eventType.`in`(it)) }
        statuses?.let { builder.and(domainEventModel.status.`in`(it)) }

        return queryFactory.selectFrom(domainEventModel)
            .where(builder)
            .orderBy(domainEventModel.createdAt.desc())
            .fetch()
    }

    override fun countEventsByTypeAndStatus(
        eventType: String,
        status: EventStatusModel,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Long {
        var predicate = domainEventModel.eventType.eq(eventType)
            .and(domainEventModel.status.eq(status))

        startDate?.let { start ->
            endDate?.let { end ->
                predicate = predicate.and(domainEventModel.createdAt.between(start, end))
            }
        }

        return queryFactory.query()
            .from(domainEventModel)
            .where(predicate)
            .fetchCount()
    }

    override fun getEventProcessingAnalytics(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventAnalyticsRow> {
        // Create date expression for PostgreSQL DATE() function
        val dateExpr = Expressions.dateTemplate(
            java.time.LocalDate::class.java,
            "DATE({0})",
            domainEventModel.createdAt
        )

        // Conditional aggregation for processed count
        val processedCase = CaseBuilder()
            .`when`(domainEventModel.status.eq(EventStatusModel.COMPLETED))
            .then(1L)
            .otherwise(0L)

        val results = queryFactory
            .select(
                dateExpr,
                domainEventModel.eventType,
                domainEventModel.status,
                domainEventModel.count(),
                domainEventModel.retryCount.avg(),
                domainEventModel.retryCount.max(),
                domainEventModel.nextRunAt.min(),
                processedCase.sum()
            )
            .from(domainEventModel)
            .where(domainEventModel.createdAt.between(startDate, endDate))
            .groupBy(dateExpr, domainEventModel.eventType, domainEventModel.status)
            .orderBy(dateExpr.desc(), domainEventModel.eventType.asc(), domainEventModel.status.asc())
            .fetch()

        return results.map { tuple ->
            EventAnalyticsRow(
                eventDate = tuple.get(dateExpr) ?: java.time.LocalDate.now(),
                eventType = tuple.get(domainEventModel.eventType) ?: "",
                status = tuple.get(domainEventModel.status) ?: EventStatusModel.PENDING,
                eventCount = tuple.get(domainEventModel.count()) ?: 0L,
                averageRetries = tuple.get(domainEventModel.retryCount.avg()) ?: 0.0,
                maxRetries = tuple.get(domainEventModel.retryCount.max()) ?: 0,
                earliestNextRun = tuple.get(domainEventModel.nextRunAt.min()),
                processedCount = tuple.get(processedCase.sum()) ?: 0L
            )
        }
    }

    override fun cleanupProcessedEvents(olderThan: LocalDateTime): Int {
        return queryFactory.delete(domainEventModel)
            .where(
                domainEventModel.status.eq(EventStatusModel.COMPLETED)
                    .and(domainEventModel.createdAt.lt(olderThan))
            )
            .execute()
            .toInt()
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

interface DomainEventRepositoryCustom {
    fun findNextPendingEventForUpdateEnhanced(
        status: EventStatusModel,
        now: LocalDateTime
    ): DomainEventModel?

    fun findPendingEventsWithFilters(
        status: EventStatusModel,
        now: LocalDateTime,
        eventType: String?,
        maxRetries: Int?,
        batchSize: Int
    ): List<DomainEventModel>

    fun batchUpdateEvents(
        eventIds: List<Long>,
        newStatus: EventStatusModel,
        incrementRetry: Boolean,
        nextRunAt: LocalDateTime?
    ): Int

    fun findEventsInTimeRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        eventTypes: List<String>?,
        statuses: List<EventStatusModel>?
    ): List<DomainEventModel>

    fun countEventsByTypeAndStatus(
        eventType: String,
        status: EventStatusModel,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Long

    fun getEventProcessingAnalytics(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<EventAnalyticsRow>

    fun cleanupProcessedEvents(olderThan: LocalDateTime): Int
}
