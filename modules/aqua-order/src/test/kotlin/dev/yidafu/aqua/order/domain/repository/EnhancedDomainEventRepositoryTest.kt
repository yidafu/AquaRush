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
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test class for enhanced domain event repository methods
 * Tests the modern event-driven architecture with pessimistic locking
 */
@ExtendWith(MockitoExtension::class)
@DataJpaTest
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
])
class EnhancedDomainEventRepositoryTest {

    @Mock
    private lateinit var entityManager: EntityManager

    private lateinit var enhancedRepository: DomainEventRepositoryImpl

    @BeforeEach
    fun setUp() {
        enhancedRepository = DomainEventRepositoryImpl(entityManager)
    }

    @Test
    fun `should find next pending event for update with pessimistic locking`() {
        // Given
        val status = EventStatus.PENDING
        val now = LocalDateTime.now()

        // Mock native query behavior
        val mockQuery = mockk<jakarta.persistence.Query<DomainEvent>>()
        whenever(entityManager.createQuery(any())).thenReturn(mockQuery)
        whenever(mockQuery.setParameter("status", status)).thenReturn(mockQuery)
        whenever(mockQuery.setParameter("now", now)).thenReturn(mockQuery)
        whenever(mockQuery.maxResults).thenReturn(mockQuery)
        whenever(mockQuery.singleResult).thenReturn(mockk<DomainEvent>())

        // When
        val result = enhancedRepository.findNextPendingEventForUpdateEnhanced(status, now)

        // Then
        assertNotNull(result)
    }

    @Test
    fun `should find pending events with filters`() {
        // Given
        val status = EventStatus.PENDING
        val now = LocalDateTime.now()
        val eventType = "ORDER_CREATED"
        val maxRetries = 3
        val batchSize = 50

        // Mock Criteria API behavior
        whenever(entityManager.criteriaBuilder).thenReturn(mockk())
        whenever(entityManager.createQuery(DomainEvent::class.java)).thenReturn(mockk())

        // When
        val result = enhancedRepository.findPendingEventsWithFilters(
            status = status,
            now = now,
            eventType = eventType,
            maxRetries = maxRetries,
            batchSize = batchSize
        )

        // Then
        assertNotNull(result)
    }

    @Test
    fun `should batch update events`() {
        // Given
        val eventIds = listOf(1L, 2L, 3L)
        val newStatus = EventStatus.PROCESSED
        val incrementRetry = true
        val nextRunAt = LocalDateTime.now().plusMinutes(5)

        // Mock Criteria API for update
        whenever(entityManager.criteriaBuilder).thenReturn(mockk())
        val mockUpdate = mockk<jakarta.persistence.criteria.CriteriaUpdate<DomainEvent>>()
        whenever(mockUpdate.set(any(), any())).thenReturn(mockUpdate)
        whenever(mockUpdate.set(any(), any())).thenReturn(mockUpdate)
        whenever(mockUpdate.where(any())).thenReturn(mockUpdate)
        whenever(entityManager.createQuery(mockUpdate)).thenReturn(mockk())
        whenever(mockQuery.executeUpdate()).thenReturn(3)

        // When
        val result = enhancedRepository.batchUpdateEvents(
            eventIds, newStatus, incrementRetry, nextRunAt
        )

        // Then
        assertEquals(3, result)
    }

    @Test
    fun `should find events in time range`() {
        // Given
        val startDate = LocalDateTime.now().minusDays(7)
        val endDate = LocalDateTime.now()
        val eventTypes = listOf("ORDER_CREATED", "PAYMENT_INITIATED")
        val statuses = listOf(EventStatus.PENDING, EventStatus.PROCESSED)

        // Mock Criteria API behavior
        whenever(entityManager.criteriaBuilder).thenReturn(mockk())
        whenever(entityManager.createQuery(DomainEvent::class.java)).thenReturn(mockk())

        // When
        val result = enhancedRepository.findEventsInTimeRange(
            startDate, endDate, eventTypes, statuses
        )

        // Then
        assertNotNull(result)
    }

    @Test
    fun `should get event processing analytics`() {
        // Given
        val startDate = LocalDateTime.now().minusDays(30)
        val endDate = LocalDateTime.now()

        // Mock native query behavior
        val mockQuery = mockk<jakarta.persistence.Query<*>>()
        whenever(entityManager.createNativeQuery(any())).thenReturn(mockQuery)
        whenever(mockQuery.setParameter("startDate", startDate)).thenReturn(mockQuery)
        whenever(mockQuery.setParameter("endDate", endDate)).thenReturn(mockQuery)

        val mockResults = mutableListOf<Array<Any>>()
        // Mock analytics data rows
        mockResults.add(arrayOf(
            LocalDateTime.now().minusDays(15).toLocalDate(),
            "ORDER_CREATED",
            EventStatus.PENDING,
            50L,
            2, 3.5, 1, 45L, 2L
        ))

        whenever(mockQuery.resultList).thenReturn(mockResults)

        // When
        val result = enhancedRepository.getEventProcessingAnalytics(startDate, endDate)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
    }

    @Test
    fun `should cleanup processed events`() {
        // Given
        val cutoffDate = LocalDateTime.now().minusDays(30)

        // Mock Criteria API for delete
        whenever(entityManager.criteriaBuilder).thenReturn(mockk())
        val mockDelete = mockk<jakarta.persistence.criteria.CriteriaDelete<DomainEvent>>()
        whenever(mockDelete.set(any(), any())).thenReturn(mockDelete)
        whenever(mockDelete.where(any())).thenReturn(mockDelete)
        whenever(entityManager.createQuery(mockDelete)).thenReturn(mockk())
        whenever(mockQuery.executeUpdate()).thenReturn(10)

        // When
        val result = enhancedRepository.cleanupProcessedEvents(cutoffDate)

        // Then
        assertEquals(10, result)
    }
}
