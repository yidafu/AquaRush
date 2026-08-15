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

package dev.yidafu.aqua.logging.util

import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import dev.yidafu.aqua.logging.context.UserContext
import dev.yidafu.aqua.logging.context.UserContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LogBuilderTest {
  private val mapper = ObjectMapper()

  @AfterEach
  fun tearDown() {
    CorrelationIdHolder.clear()
    UserContextHolder.clear()
  }

  @Suppress("UNCHECKED_CAST")
  private fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

  @Test
  fun `buildJson should include default timestamp and context`() {
    CorrelationIdHolder.setCorrelationId("corr")
    UserContextHolder.setUserContext(UserContext(userId = "1", username = "u"))
    val json = LogBuilder().message("hi").buildJson()
    val map = parse(json)
    assertEquals("hi", map["message"])
    assertEquals("corr", map["correlationId"])
    assertEquals("1", map["userId"])
    assertNotNull(map["timestamp"])
  }

  @Test
  fun `field should add single field`() {
    val builder = LogBuilder().field("key", "value")
    assertEquals("value", builder.getData()["key"])
  }

  @Test
  fun `fields should add multiple fields`() {
    val builder = LogBuilder().fields(mapOf("a" to 1, "b" to 2))
    assertEquals(1, builder.getData()["a"])
    assertEquals(2, builder.getData()["b"])
  }

  @Test
  fun `eventType should set event type`() {
    val builder = LogBuilder().eventType("USER_ACTION")
    assertEquals("USER_ACTION", builder.getData()["eventType"])
  }

  @Test
  fun `result success should set success flag`() {
    val builder = LogBuilder().result(true)
    assertEquals(true, builder.getData()["success"])
  }

  @Test
  fun `result failure with error should include error details`() {
    val builder = LogBuilder().result(false, IllegalStateException("boom"))
    val data = builder.getData()
    assertEquals(false, data["success"])
    assertEquals("IllegalStateException", data["errorType"])
    assertEquals("boom", data["errorMessage"])
    assertTrue((data["errorStackTrace"] as String).contains("boom"))
  }

  @Test
  fun `result failure without error should omit error details`() {
    val builder = LogBuilder().result(false)
    assertFalse(builder.getData().containsKey("errorType"))
  }

  @Test
  fun `performance should add duration and flag`() {
    val builder = LogBuilder().performance(123)
    assertEquals(123L, builder.getData()["durationMs"])
    assertEquals(true, builder.getData()["performance"])
  }

  @Test
  fun `audit should add audit fields`() {
    val builder = LogBuilder().audit("CREATE", "order", "SUCCESS")
    val data = builder.getData()
    assertEquals(true, data["audit"])
    assertEquals("CREATE", data["auditAction"])
    assertEquals("order", data["auditResource"])
    assertEquals("SUCCESS", data["auditResult"])
  }

  @Test
  fun `security should add security fields`() {
    val builder = LogBuilder().security("LOGIN", "HIGH", details = mapOf("ip" to "1.1.1.1"))
    val data = builder.getData()
    assertEquals(true, data["security"])
    assertEquals("LOGIN", data["securityEventType"])
    assertEquals("HIGH", data["securitySeverity"])
    assertEquals(mapOf("ip" to "1.1.1.1"), data["securityDetails"])
  }

  @Test
  fun `security without details should omit details`() {
    val builder = LogBuilder().security("LOGIN", "HIGH")
    assertFalse(builder.getData().containsKey("securityDetails"))
  }

  @Test
  fun `business should add business fields`() {
    val builder = LogBuilder().business("CREATE", "ORDER", "SUCCESS")
    val data = builder.getData()
    assertEquals(true, data["business"])
    assertEquals("CREATE", data["businessOperation"])
    assertEquals("ORDER", data["businessModule"])
    assertEquals("SUCCESS", data["businessResult"])
  }

  @Test
  fun `buildString should join key-value parts`() {
    val result = LogBuilder().message("hi").eventType("EVT").buildString()
    assertTrue(result.contains("message=hi"))
    assertTrue(result.contains("eventType=EVT"))
  }

  @Test
  fun `buildString should include error and business fields`() {
    val result = LogBuilder().result(false, RuntimeException("err")).business("OP", "MOD", "FAILED").buildString()
    assertTrue(result.contains("errorType=RuntimeException"))
    assertTrue(result.contains("errorMessage=err"))
    assertTrue(result.contains("businessOperation=OP"))
  }

  @Test
  fun `buildJson should serialize all chained data`() {
    val json =
      LogBuilder()
        .field("trace", "t1")
        .eventType("PERF")
        .performance(10)
        .buildJson()
    val map = parse(json)
    assertEquals("t1", map["trace"])
    assertEquals("PERF", map["eventType"])
    assertEquals(10, map["durationMs"])
  }

  @Test
  fun `getData should return immutable snapshot`() {
    val builder = LogBuilder()
    val snapshot = builder.getData()
    builder.field("later", 1)
    assertFalse(snapshot.containsKey("later"))
  }
}
