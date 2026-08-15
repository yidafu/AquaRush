/**
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

package dev.yidafu.aqua.common.messaging.service

import dev.yidafu.aqua.common.messaging.config.SimplifiedMessagingProperties
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.publisher.EventPublisher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimplifiedEventPublishServiceTest {
  private lateinit var eventPublisher: EventPublisher
  private lateinit var service: SimplifiedEventPublishService

  @BeforeEach
  fun setUp() {
    eventPublisher = mockk(relaxed = true)
    service = SimplifiedEventPublishService(eventPublisher, SimplifiedMessagingProperties(enabled = true))
  }

  @Test
  fun `publishDomainEvent should publish and return true`() {
    every { eventPublisher.publishSync(any()) } returns true
    val result = service.publishDomainEvent("ORDER_CREATED", "123", mapOf("a" to 1))
    assertTrue(result)
    verify { eventPublisher.publishSync(any()) }
  }

  @Test
  fun `publishDomainEvent should return false when publishing fails`() {
    every { eventPublisher.publishSync(any()) } throws RuntimeException("broker down")
    val result = service.publishDomainEvent("ORDER_CREATED", "123", mapOf("a" to 1))
    assertFalse(result)
  }

  @Test
  fun `publishOrderCreated should publish order created event`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishOrderCreated(100L, 1L, 5000L))
    verify { eventPublisher.publishSync(any()) }
  }

  @Test
  fun `publishOrderPaid should publish order paid event`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishOrderPaid(100L, 1L, 5000L))
  }

  @Test
  fun `publishOrderCancelled should include refund flag`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishOrderCancelled(100L, 1L, "user request", shouldRefund = true))
  }

  @Test
  fun `publishOrderCancelled without paymentTransactionId should publish`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishOrderCancelled(100L, 1L, "reason"))
  }

  @Test
  fun `publishDeliveryCompleted should publish delivery event`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishDeliveryCompleted(100L, 5L))
  }

  @Test
  fun `publishDeliveryAssigned should publish assignment event`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishDeliveryAssigned(adminId = 1L, orderId = 100L, deliveryWorkerId = 5L, userId = 2L))
  }

  @Test
  fun `publishDeliveryStarted should publish started event`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishDeliveryStarted(100L, 5L))
  }

  @Test
  fun `publishOrderCompleted should publish completed event`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishOrderCompleted(100L, 1L, 5L))
  }

  @Test
  fun `publishPaymentTimeout with Long ids should publish`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishPaymentTimeout(100L, 1L))
  }

  @Test
  fun `publishPaymentTimeout with UUID ids should publish`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishPaymentTimeout(UUID.randomUUID(), UUID.randomUUID()))
  }

  @Test
  fun `publishDeliveryTimeout with UUID ids should publish`() {
    every { eventPublisher.publishSync(any()) } returns true
    assertTrue(service.publishDeliveryTimeout(UUID.randomUUID(), UUID.randomUUID()))
  }

  @Test
  fun `publishAddressUpdate should publish address event`() {
    every { eventPublisher.publishSync(any()) } returns true
    service.publishAddressUpdate(10L, 20L)
    verify { eventPublisher.publishSync(any()) }
  }

  @Test
  fun `getSystemStatus should report messaging config`() {
    val status = service.getSystemStatus()
    assertEquals(true, status["messagingEnabled"])
    assertEquals("artemis", status["strategy"])
  }

  @Test
  fun `isPublisherHealthy should return true when available`() {
    every { eventPublisher.isAvailable() } returns true
    assertTrue(service.isPublisherHealthy())
  }

  @Test
  fun `isPublisherHealthy should return false when check fails`() {
    every { eventPublisher.isAvailable() } throws RuntimeException("down")
    assertFalse(service.isPublisherHealthy())
  }

  @Test
  fun `published event should have correct type`() {
    every { eventPublisher.publishSync(any()) } returns true
    service.publishOrderCreated(100L, 1L, 5000L)
    verify {
      eventPublisher.publishSync(
        match<DomainEvent> { event -> event.eventType == DomainEventType.ORDER_CREATED.value },
      )
    }
  }
}
