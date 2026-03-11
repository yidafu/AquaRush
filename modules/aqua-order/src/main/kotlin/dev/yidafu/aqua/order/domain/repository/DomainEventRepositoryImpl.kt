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
import dev.yidafu.aqua.common.domain.model.OrderDomainEventModel
import dev.yidafu.aqua.common.domain.model.QOrderDomainEventModel.Companion.orderDomainEventModel
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
    now: LocalDateTime,
  ): OrderDomainEventModel? {
    // Native query with explicit pessimistic locking
    val query =
      entityManager.createQuery(
        """
        SELECT de FROM DomainEventModel de
        WHERE de.status = :status
        AND (de.nextRunAt <= :now OR de.nextRunAt IS NULL)
        ORDER BY de.createdAt ASC
        """.trimIndent(),
        OrderDomainEventModel::class.java,
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
    batchSize: Int,
  ): List<OrderDomainEventModel> {
    val builder = BooleanBuilder()

    builder.and(orderDomainEventModel.status.eq(status))
    builder.and(
      orderDomainEventModel.nextRunAt
        .loe(now)
        .or(orderDomainEventModel.nextRunAt.isNull),
    )

    eventType?.let { builder.and(orderDomainEventModel.eventType.eq(it)) }
    maxRetries?.let { builder.and(orderDomainEventModel.retryCount.loe(it)) }

    return queryFactory
      .selectFrom(orderDomainEventModel)
      .where(builder)
      .orderBy(orderDomainEventModel.createdAt.asc())
      .limit(batchSize.toLong())
      .fetch()
  }

  override fun batchUpdateEvents(
    eventIds: List<Long>,
    newStatus: EventStatusModel,
    incrementRetry: Boolean,
    nextRunAt: LocalDateTime?,
  ): Int {
    var update =
      queryFactory
        .update(orderDomainEventModel)
        .set(orderDomainEventModel.status, newStatus)
        .set(orderDomainEventModel.updatedAt, LocalDateTime.now())
        .where(orderDomainEventModel.id.`in`(eventIds))

    if (incrementRetry) {
      // Note: QueryDSL doesn't support increment expressions directly
      // We need to fetch current values, increment, and update
      val events =
        queryFactory
          .selectFrom(orderDomainEventModel)
          .where(orderDomainEventModel.id.`in`(eventIds))
          .fetch()

      events.forEach { event ->
        queryFactory
          .update(orderDomainEventModel)
          .set(orderDomainEventModel.retryCount, event.retryCount + 1)
          .where(orderDomainEventModel.id.eq(event.id))
          .execute()
      }
    }

    nextRunAt?.let {
      update = update.set(orderDomainEventModel.nextRunAt, it)
    }

    return update.execute().toInt()
  }

  override fun findEventsInTimeRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    eventTypes: List<String>?,
    statuses: List<EventStatusModel>?,
  ): List<OrderDomainEventModel> {
    val builder = BooleanBuilder()

    builder.and(orderDomainEventModel.createdAt.between(startDate, endDate))
    eventTypes?.let { builder.and(orderDomainEventModel.eventType.`in`(it)) }
    statuses?.let { builder.and(orderDomainEventModel.status.`in`(it)) }

    return queryFactory
      .selectFrom(orderDomainEventModel)
      .where(builder)
      .orderBy(orderDomainEventModel.createdAt.desc())
      .fetch()
  }

  override fun countEventsByTypeAndStatus(
    eventType: String,
    status: EventStatusModel,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): Long {
    var predicate =
      orderDomainEventModel.eventType
        .eq(eventType)
        .and(orderDomainEventModel.status.eq(status))

    startDate?.let { start ->
      endDate?.let { end ->
        predicate = predicate.and(orderDomainEventModel.createdAt.between(start, end))
      }
    }

    return queryFactory
      .query()
      .from(orderDomainEventModel)
      .where(predicate)
      .fetchCount()
  }

  override fun getEventProcessingAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<EventAnalyticsRow> {
    // Create date expression for PostgreSQL DATE() function
    val dateExpr =
      Expressions.dateTemplate(
        java.time.LocalDate::class.java,
        "DATE({0})",
        orderDomainEventModel.createdAt,
      )

    // Conditional aggregation for processed count
    val processedCase =
      CaseBuilder()
        .`when`(orderDomainEventModel.status.eq(EventStatusModel.COMPLETED))
        .then(1L)
        .otherwise(0L)

    val results =
      queryFactory
        .select(
          dateExpr,
          orderDomainEventModel.eventType,
          orderDomainEventModel.status,
          orderDomainEventModel.count(),
          orderDomainEventModel.retryCount.avg(),
          orderDomainEventModel.retryCount.max(),
          orderDomainEventModel.nextRunAt.min(),
          processedCase.sumLong(),
        ).from(orderDomainEventModel)
        .where(orderDomainEventModel.createdAt.between(startDate, endDate))
        .groupBy(dateExpr, orderDomainEventModel.eventType, orderDomainEventModel.status)
        .orderBy(dateExpr.desc(), orderDomainEventModel.eventType.asc(), orderDomainEventModel.status.asc())
        .fetch()

    return results.map { tuple ->
      EventAnalyticsRow(
        eventDate = tuple.get(dateExpr) ?: java.time.LocalDate.now(),
        eventType = tuple.get(orderDomainEventModel.eventType) ?: "",
        status = tuple.get(orderDomainEventModel.status) ?: EventStatusModel.PENDING,
        eventCount = tuple.get(orderDomainEventModel.count()) ?: 0L,
        averageRetries = tuple.get(orderDomainEventModel.retryCount.avg()) ?: 0.0,
        maxRetries = tuple.get(orderDomainEventModel.retryCount.max()) ?: 0,
        earliestNextRun = tuple.get(orderDomainEventModel.nextRunAt.min()),
        processedCount = tuple.get(processedCase.sumLong()) ?: 0L,
      )
    }
  }

  override fun cleanupProcessedEvents(olderThan: LocalDateTime): Int =
    queryFactory
      .delete(orderDomainEventModel)
      .where(
        orderDomainEventModel.status
          .eq(EventStatusModel.COMPLETED)
          .and(orderDomainEventModel.createdAt.lt(olderThan)),
      ).execute()
      .toInt()
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
  val processedCount: Long,
)

interface DomainEventRepositoryCustom {
  fun findNextPendingEventForUpdateEnhanced(
    status: EventStatusModel,
    now: LocalDateTime,
  ): OrderDomainEventModel?

  fun findPendingEventsWithFilters(
    status: EventStatusModel,
    now: LocalDateTime,
    eventType: String?,
    maxRetries: Int?,
    batchSize: Int,
  ): List<OrderDomainEventModel>

  fun batchUpdateEvents(
    eventIds: List<Long>,
    newStatus: EventStatusModel,
    incrementRetry: Boolean,
    nextRunAt: LocalDateTime?,
  ): Int

  fun findEventsInTimeRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    eventTypes: List<String>?,
    statuses: List<EventStatusModel>?,
  ): List<OrderDomainEventModel>

  fun countEventsByTypeAndStatus(
    eventType: String,
    status: EventStatusModel,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): Long

  fun getEventProcessingAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<EventAnalyticsRow>

  fun cleanupProcessedEvents(olderThan: LocalDateTime): Int
}
