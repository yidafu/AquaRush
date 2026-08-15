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

class UserActionLogFormatterTest {
  private val formatter = UserActionLogFormatter.instance
  private val mapper = ObjectMapper()

  @Suppress("UNCHECKED_CAST")
  private fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

  @AfterEach
  fun tearDown() {
    CorrelationIdHolder.clear()
    UserContextHolder.clear()
  }

  @Test
  fun `format should produce basic user action json`() {
    val map = parse(formatter.format("CLICK", "button-1"))
    assertEquals("INFO", map["level"])
    assertEquals("user-action", map["logger"])
    assertEquals("USER_ACTION", map["eventType"])
    assertEquals("CLICK", map["actionType"])
    assertEquals("button-1", map["target"])
    assertEquals("aqua-rush", map["service"])
  }

  @Test
  fun `format should include correlation and user context`() {
    CorrelationIdHolder.setCorrelationId("corr-1")
    UserContextHolder.setUserContext(UserContext(userId = "42", username = "alice", role = "ADMIN", tenantId = "t1"))
    val map = parse(formatter.format("SEARCH", "products"))
    assertEquals("corr-1", map["correlationId"])
    assertEquals("42", map["userId"])
    assertEquals("alice", map["username"])
    assertEquals("ADMIN", map["userRole"])
    assertEquals("t1", map["tenantId"])
  }

  @Test
  fun `format should merge custom properties`() {
    val map = parse(formatter.format("CUSTOM", "target", mapOf("extra" to "value")))
    assertEquals("value", map["extra"])
  }

  @Test
  fun `formatPageView should include page fields`() {
    val map = parse(formatter.formatPageView("/home", pageTitle = "首页", referrer = "/login", userAgent = "wechat"))
    assertEquals("PAGE_VIEW", map["actionType"])
    assertEquals("/home", map["pageUrl"])
    assertEquals("首页", map["pageTitle"])
    assertEquals("/login", map["referrer"])
    assertEquals("wechat", map["userAgent"])
  }

  @Test
  fun `formatClick should include element fields`() {
    val map =
      parse(
        formatter.formatClick(
          elementId = "btn",
          elementType = "button",
          elementText = "确定",
          coordinates = mapOf("screenX" to 10, "screenY" to 20, "pageX" to 30, "pageY" to 40),
        ),
      )
    assertEquals("CLICK", map["actionType"])
    assertEquals("btn", map["elementId"])
    assertEquals("确定", map["elementText"])
    assertEquals(10, map["screenX"])
    assertEquals(30, map["pageX"])
  }

  @Test
  fun `formatClick without coordinates should omit coordinate keys`() {
    val map = parse(formatter.formatClick(elementId = "btn", elementType = "button"))
    assertTrue(!map.containsKey("screenX"))
  }

  @Test
  fun `formatDrag should compute distances`() {
    val map =
      parse(
        formatter.formatDrag(
          startCoordinates = mapOf("pageX" to 10, "pageY" to 20),
          endCoordinates = mapOf("pageX" to 40, "pageY" to 60),
          elementId = "drag",
        ),
      )
    assertEquals("DRAG", map["actionType"])
    assertEquals("drag", map["target"])
    assertEquals(30, map["distanceX"])
    assertEquals(40, map["distanceY"])
  }

  @Test
  fun `formatDrag without element id should default target`() {
    val map =
      parse(
        formatter.formatDrag(
          startCoordinates = mapOf("pageX" to 0, "pageY" to 0),
          endCoordinates = mapOf("pageX" to 5, "pageY" to 5),
        ),
      )
    assertEquals("drag_operation", map["target"])
  }

  @Test
  fun `formatInput should sanitize password`() {
    val map = parse(formatter.formatInput("pwd-field", "secret123", inputType = "password"))
    assertEquals("***", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize email`() {
    val map = parse(formatter.formatInput("email-field", "alice@example.com", inputType = "email"))
    assertEquals("al***@example.com", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize short email`() {
    val map = parse(formatter.formatInput("email-field", "a@example.com", inputType = "email"))
    assertEquals("***@example.com", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize invalid email`() {
    val map = parse(formatter.formatInput("email-field", "not-an-email", inputType = "email"))
    assertEquals("***", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize phone`() {
    val map = parse(formatter.formatInput("phone-field", "13800138000", inputType = "phone"))
    assertEquals("138***8000", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize short phone`() {
    val map = parse(formatter.formatInput("phone-field", "123", inputType = "tel"))
    assertEquals("***", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize credit card`() {
    val map = parse(formatter.formatInput("card-field", "6222000011112222", inputType = "creditcard"))
    assertEquals("6222***2222", map["inputValue"])
  }

  @Test
  fun `formatInput should sanitize short card`() {
    val map = parse(formatter.formatInput("card-field", "1234", inputType = "bankcard"))
    assertEquals("***", map["inputValue"])
  }

  @Test
  fun `formatInput should truncate long text input`() {
    val longValue = "x".repeat(150)
    val map = parse(formatter.formatInput("text-field", longValue, inputType = "text"))
    val value = map["inputValue"] as String
    assertTrue(value.contains("[TRUNCATED]"))
  }

  @Test
  fun `formatInput should keep short text input`() {
    val map = parse(formatter.formatInput("text-field", "hello", inputType = "text"))
    assertEquals("hello", map["inputValue"])
  }

  @Test
  fun `formatInput should include input length`() {
    val map = parse(formatter.formatInput("text-field", "hello"))
    assertEquals(5, map["inputLength"])
    assertEquals("text", map["inputType"])
  }

  @Test
  fun `formatBackendOperation success should produce INFO`() {
    val map = parse(formatter.formatBackendOperation("CREATE", "order", "SUCCESS", "order-1"))
    assertEquals("INFO", map["level"])
    assertEquals("BACKEND_OPERATION", map["actionType"])
    assertEquals("ORDER", map["module"])
  }

  @Test
  fun `formatBackendOperation error should produce ERROR`() {
    val map = parse(formatter.formatBackendOperation("CREATE", "order", "FAILURE", "order-1"))
    assertEquals("ERROR", map["level"])
  }

  @Test
  fun `formatBackendOperation warning should produce WARN`() {
    val map = parse(formatter.formatBackendOperation("CREATE", "order", "WARNING", "order-1"))
    assertEquals("WARN", map["level"])
  }

  @Test
  fun `formatCustomAction should use provided level`() {
    val map = parse(formatter.formatCustomAction("special", "target", level = "ERROR"))
    assertEquals("ERROR", map["level"])
    assertEquals("SPECIAL", map["actionType"])
  }
}
