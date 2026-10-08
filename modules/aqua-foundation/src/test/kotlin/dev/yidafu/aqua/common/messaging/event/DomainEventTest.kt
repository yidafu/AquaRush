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

package dev.yidafu.aqua.common.messaging.event

import dev.yidafu.aqua.common.domain.model.enums.EventStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DomainEventTest :
  FunSpec({

    test("DomainEvent should have sensible defaults") {
      val event = DomainEvent(eventType = "ORDER_CREATED", payload = "{}")
      event.eventType shouldBe "ORDER_CREATED"
      event.payload shouldBe "{}"
      event.retryCount shouldBe 0
      event.status shouldBe EventStatus.PENDING
    }
    test("DomainEvent should be serializable") {
      val event = DomainEvent(eventType = "X", payload = "{}")
      (event is java.io.Serializable) shouldBe true
    }
    test("DomainEvent should support mutation of mutable fields") {
      val event = DomainEvent(eventType = "X", payload = "{}")
      event.eventType = "Y"
      event.payload = "[]"
      event.retryCount = 2
      event.status = EventStatus.COMPLETED
      event.eventType shouldBe "Y"
      event.payload shouldBe "[]"
      event.retryCount shouldBe 2
      event.status shouldBe EventStatus.COMPLETED
    }
    test("DomainEvent should carry aggregate id") {
      val event = DomainEvent(id = 1L, aggregateId = "agg-1", eventType = "X", payload = "{}")
      event.id shouldBe 1L
      event.aggregateId shouldBe "agg-1"
    }
    test("DomainEventType should expose values") {
      DomainEventType.ORDER_CREATED.value.isNotBlank() shouldBe true
      "ORDER_PAID" shouldBe DomainEventType.ORDER_PAID.value
    }
  })
