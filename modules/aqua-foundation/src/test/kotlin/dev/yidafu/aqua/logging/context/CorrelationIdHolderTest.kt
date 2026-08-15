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

package dev.yidafu.aqua.logging.context

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CorrelationIdHolderTest {
  @AfterEach
  fun tearDown() {
    CorrelationIdHolder.clear()
  }

  @Test
  fun `set and get should round trip`() {
    CorrelationIdHolder.setCorrelationId("abc")
    assertEquals("abc", CorrelationIdHolder.getCorrelationId())
  }

  @Test
  fun `get should return null when unset`() {
    assertNull(CorrelationIdHolder.getCorrelationId())
  }

  @Test
  fun `generateAndSet should produce non-empty id`() {
    val id = CorrelationIdHolder.generateAndSet()
    assertNotNull(id)
    assertTrue(id.length >= 32)
    assertEquals(id, CorrelationIdHolder.getCorrelationId())
  }

  @Test
  fun `generateAndSet should produce unique ids`() {
    assertNotEquals(CorrelationIdHolder.generateAndSet(), CorrelationIdHolder.generateAndSet())
  }

  @Test
  fun `clear should remove correlation id`() {
    CorrelationIdHolder.setCorrelationId("abc")
    CorrelationIdHolder.clear()
    assertNull(CorrelationIdHolder.getCorrelationId())
  }

  @Test
  fun `hasCorrelationId should reflect state`() {
    assertFalse(CorrelationIdHolder.hasCorrelationId())
    CorrelationIdHolder.setCorrelationId("abc")
    assertTrue(CorrelationIdHolder.hasCorrelationId())
    CorrelationIdHolder.clear()
    assertFalse(CorrelationIdHolder.hasCorrelationId())
  }
}
