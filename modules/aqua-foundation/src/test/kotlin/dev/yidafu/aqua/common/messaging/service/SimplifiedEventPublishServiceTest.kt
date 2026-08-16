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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class SimplifiedEventPublishServiceTest :
  FunSpec({
    var eventPublisher: EventPublisher = mockk()
    var service: SimplifiedEventPublishService = mockk()

    beforeTest {
      eventPublisher = mockk(relaxed = true)
      service = SimplifiedEventPublishService(eventPublisher, SimplifiedMessagingProperties(enabled = true))
    }
    test("publishDomainEvent should publish and return true") {
      every { eventPublisher.publishSync(any()) } returns true
      val result = service.publishDomainEvent("ORDER_CREATED", "123", mapOf("a" to 1))
      result shouldBe true
      verify { eventPublisher.publishSync(any()) }
    }
    test("publishDomainEvent should return false when publishing fails") {
      every { eventPublisher.publishSync(any()) } throws RuntimeException("broker down")
      val result = service.publishDomainEvent("ORDER_CREATED", "123", mapOf("a" to 1))
      result shouldBe false
    }
    test("publishOrderCreated should publish order created event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishOrderCreated(100L, 1L, 5000L) shouldBe true
      verify { eventPublisher.publishSync(any()) }
    }
    test("publishOrderPaid should publish order paid event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishOrderPaid(100L, 1L, 5000L) shouldBe true
    }
    test("publishOrderCancelled should include refund flag") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishOrderCancelled(100L, 1L, "user request", shouldRefund = true) shouldBe true
    }
    test("publishOrderCancelled without paymentTransactionId should publish") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishOrderCancelled(100L, 1L, "reason") shouldBe true
    }
    test("publishDeliveryCompleted should publish delivery event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishDeliveryCompleted(100L, 5L) shouldBe true
    }
    test("publishDeliveryAssigned should publish assignment event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishDeliveryAssigned(adminId = 1L, orderId = 100L, deliveryWorkerId = 5L, userId = 2L) shouldBe true
    }
    test("publishDeliveryStarted should publish started event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishDeliveryStarted(100L, 5L) shouldBe true
    }
    test("publishOrderCompleted should publish completed event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishOrderCompleted(100L, 1L, 5L) shouldBe true
    }
    test("publishPaymentTimeout with Long ids should publish") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishPaymentTimeout(100L, 1L) shouldBe true
    }
    test("publishPaymentTimeout with UUID ids should publish") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishPaymentTimeout(UUID.randomUUID(), UUID.randomUUID()) shouldBe true
    }
    test("publishDeliveryTimeout with UUID ids should publish") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishDeliveryTimeout(UUID.randomUUID(), UUID.randomUUID()) shouldBe true
    }
    test("publishAddressUpdate should publish address event") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishAddressUpdate(10L, 20L)
      verify { eventPublisher.publishSync(any()) }
    }
    test("getSystemStatus should report messaging config") {
      val status = service.getSystemStatus()
      status["messagingEnabled"] shouldBe true
      status["strategy"] shouldBe "artemis"
    }
    test("isPublisherHealthy should return true when available") {
      every { eventPublisher.isAvailable() } returns true
      service.isPublisherHealthy() shouldBe true
    }
    test("isPublisherHealthy should return false when check fails") {
      every { eventPublisher.isAvailable() } throws RuntimeException("down")
      service.isPublisherHealthy() shouldBe false
    }
    test("published event should have correct type") {
      every { eventPublisher.publishSync(any()) } returns true
      service.publishOrderCreated(100L, 1L, 5000L)
      verify {
        eventPublisher.publishSync(
          match<DomainEvent> { event -> event.eventType == DomainEventType.ORDER_CREATED.value },
        )
      }
    }
  })
