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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainEventTest {
  @Test
  fun `DomainEvent should have sensible defaults`() {
    val event = DomainEvent(eventType = "ORDER_CREATED", payload = "{}")
    assertEquals("ORDER_CREATED", event.eventType)
    assertEquals("{}", event.payload)
    assertEquals(0, event.retryCount)
    assertEquals(EventStatus.PENDING, event.status)
  }

  @Test
  fun `DomainEvent should be serializable`() {
    val event = DomainEvent(eventType = "X", payload = "{}")
    assertTrue(event is java.io.Serializable)
  }

  @Test
  fun `DomainEvent should support mutation of mutable fields`() {
    val event = DomainEvent(eventType = "X", payload = "{}")
    event.eventType = "Y"
    event.payload = "[]"
    event.retryCount = 2
    event.status = EventStatus.COMPLETED
    assertEquals("Y", event.eventType)
    assertEquals("[]", event.payload)
    assertEquals(2, event.retryCount)
    assertEquals(EventStatus.COMPLETED, event.status)
  }

  @Test
  fun `DomainEvent should carry aggregate id`() {
    val event = DomainEvent(id = 1L, aggregateId = "agg-1", eventType = "X", payload = "{}")
    assertEquals(1L, event.id)
    assertEquals("agg-1", event.aggregateId)
  }

  @Test
  fun `DomainEventType should expose values`() {
    assertTrue(DomainEventType.ORDER_CREATED.value.isNotBlank())
    assertEquals(DomainEventType.ORDER_PAID.value, "ORDER_PAID")
  }
}
