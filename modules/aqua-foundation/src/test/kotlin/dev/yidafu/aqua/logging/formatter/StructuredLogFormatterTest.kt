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

package dev.yidafu.aqua.logging.formatter

import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import dev.yidafu.aqua.logging.context.UserContext
import dev.yidafu.aqua.logging.context.UserContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StructuredLogFormatterTest {
  private val formatter = StructuredLogFormatter.instance
  private val mapper = ObjectMapper()

  @Suppress("UNCHECKED_CAST")
  private fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

  @AfterEach
  fun tearDown() {
    CorrelationIdHolder.clear()
    UserContextHolder.clear()
  }

  @Test
  fun `format should produce base json fields`() {
    val map = parse(formatter.format("info", "test-logger", "hello"))
    assertEquals("INFO", map["level"])
    assertEquals("test-logger", map["logger"])
    assertEquals("hello", map["message"])
    assertEquals("aqua-rush", map["service"])
  }

  @Test
  fun `format should include exception details`() {
    val exception = IllegalStateException("boom")
    val map = parse(formatter.format("error", "logger", "msg", exception))
    assertEquals("IllegalStateException", map["errorType"])
    assertEquals("boom", map["errorMessage"])
    assertTrue((map["errorStackTrace"] as String).contains("IllegalStateException"))
  }

  @Test
  fun `format should include context and additional data`() {
    CorrelationIdHolder.setCorrelationId("corr")
    UserContextHolder.setUserContext(UserContext(userId = "1", username = "u", role = "R", tenantId = "T"))
    val map = parse(formatter.format("info", "logger", "msg", additionalData = mapOf("custom" to "x")))
    assertEquals("corr", map["correlationId"])
    assertEquals("1", map["userId"])
    assertEquals("R", map["userRole"])
    assertEquals("T", map["tenantId"])
    assertEquals("x", map["custom"])
  }

  @Test
  fun `formatPerformance fast category`() {
    val map = parse(formatter.formatPerformance("op", 10))
    assertEquals("FAST", map["performanceCategory"])
    assertEquals("INFO", map["level"])
  }

  @Test
  fun `formatPerformance normal category`() {
    val map = parse(formatter.formatPerformance("op", 100))
    assertEquals("NORMAL", map["performanceCategory"])
  }

  @Test
  fun `formatPerformance slow category`() {
    val map = parse(formatter.formatPerformance("op", 500))
    assertEquals("SLOW", map["performanceCategory"])
  }

  @Test
  fun `formatPerformance very slow category`() {
    val map = parse(formatter.formatPerformance("op", 5000))
    assertEquals("VERY_SLOW", map["performanceCategory"])
  }

  @Test
  fun `formatPerformance failure should produce WARN`() {
    val map = parse(formatter.formatPerformance("op", 5000, success = false))
    assertEquals("WARN", map["level"])
    assertEquals(false, map["success"])
  }

  @Test
  fun `formatPerformance should include operation and duration`() {
    val map = parse(formatter.formatPerformance("op", 123))
    assertEquals("op", map["operation"])
    assertEquals(123, map["durationMs"])
  }

  @Test
  fun `formatAudit should include audit fields`() {
    val map = parse(formatter.formatAudit("DELETE", "order", "SUCCESS", details = mapOf("id" to 1)))
    assertEquals("AUDIT", map["eventType"])
    assertEquals("DELETE", map["auditAction"])
    assertEquals("order", map["auditResource"])
    assertEquals("SUCCESS", map["auditResult"])
    assertEquals(mapOf("id" to 1), map["auditDetails"])
  }

  @Test
  fun `formatSecurity critical should produce ERROR`() {
    val map = parse(formatter.formatSecurity("LOGIN_FAILED", "CRITICAL", "msg"))
    assertEquals("ERROR", map["level"])
    assertEquals("SECURITY", map["eventType"])
  }

  @Test
  fun `formatSecurity high should produce ERROR`() {
    val map = parse(formatter.formatSecurity("X", "HIGH", "msg"))
    assertEquals("ERROR", map["level"])
  }

  @Test
  fun `formatSecurity medium should produce WARN`() {
    val map = parse(formatter.formatSecurity("X", "MEDIUM", "msg"))
    assertEquals("WARN", map["level"])
  }

  @Test
  fun `formatSecurity low should produce INFO`() {
    val map = parse(formatter.formatSecurity("X", "LOW", "msg"))
    assertEquals("INFO", map["level"])
  }

  @Test
  fun `formatSecurity should include details`() {
    val map = parse(formatter.formatSecurity("LOGIN_FAILED", "MEDIUM", "msg", details = mapOf("ip" to "1.2.3.4")))
    assertEquals(mapOf("ip" to "1.2.3.4"), map["securityDetails"])
  }

  @Test
  fun `formatSecurity should include user context when present`() {
    UserContextHolder.setUserContext(UserContext(userId = "9", username = "u"))
    val map = parse(formatter.formatSecurity("X", "LOW", "msg"))
    assertEquals("9", map["userId"])
  }

  @Test
  fun `format should include user context without role and tenant`() {
    UserContextHolder.setUserContext(UserContext(userId = "9", username = "u"))
    val map = parse(formatter.format("info", "logger", "msg"))
    assertEquals("9", map["userId"])
    assertEquals("u", map["username"])
  }
}
