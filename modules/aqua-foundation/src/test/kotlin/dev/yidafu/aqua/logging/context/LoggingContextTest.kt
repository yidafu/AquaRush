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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoggingContextTest {
  @AfterEach
  fun tearDown() {
    CorrelationIdHolder.clear()
    UserContextHolder.clear()
  }

  @Test
  fun `put get remove should manage custom data`() {
    val context = LoggingContext.current()
    context.put("key", "value")
    assertEquals("value", context.get("key"))
    context.remove("key")
    assertNull(context.get("key"))
  }

  @Test
  fun `getAllCustomData should return snapshot`() {
    val context = LoggingContext.current()
    context.put("a", 1)
    context.put("b", 2)
    assertEquals(mapOf("a" to 1, "b" to 2), context.getAllCustomData())
  }

  @Test
  fun `clearCustomData should remove all custom data`() {
    val context = LoggingContext.current()
    context.put("a", 1)
    context.clearCustomData()
    assertTrue(context.getAllCustomData().isEmpty())
  }

  @Test
  fun `correlationId should reflect holder state`() {
    CorrelationIdHolder.setCorrelationId("cid")
    val context = LoggingContext.current()
    assertEquals("cid", context.correlationId)
  }

  @Test
  fun `userContext should reflect holder state`() {
    UserContextHolder.setUserContext(UserContext(userId = "1", username = "u"))
    val context = LoggingContext.current()
    assertEquals("1", context.userContext?.userId)
  }

  @Test
  fun `startTime should be set`() {
    assertNotNull(LoggingContext.current().startTime)
  }

  @Test
  fun `buildContextMap should include correlation and user info`() {
    CorrelationIdHolder.setCorrelationId("cid")
    UserContextHolder.setUserContext(UserContext(userId = "1", username = "u", role = "R", tenantId = "T"))
    val context = LoggingContext.current()
    context.put("custom", "x")
    val map = context.buildContextMap()
    assertEquals("cid", map["correlationId"])
    assertEquals("1", map["userId"])
    assertEquals("u", map["username"])
    assertEquals("R", map["userRole"])
    assertEquals("T", map["tenantId"])
    assertEquals("x", map["custom"])
    assertNotNull(map["timestamp"])
  }

  @Test
  fun `buildContextMap should include additional user info`() {
    UserContextHolder.setUserContext(UserContext(userId = "1", username = "u", additionalInfo = mapOf("dept" to "ops")))
    val map = LoggingContext.current().buildContextMap()
    assertEquals("ops", map["dept"])
  }

  @Test
  fun `buildContextMap without context should only include timestamp`() {
    val map = LoggingContext.current().buildContextMap()
    assertTrue(!map.containsKey("correlationId"))
    assertTrue(!map.containsKey("userId"))
    assertNotNull(map["timestamp"])
  }

  @Test
  fun `current should create fresh context each call`() {
    val a = LoggingContext.current()
    val b = LoggingContext.current()
    assertTrue(a !== b)
  }
}
