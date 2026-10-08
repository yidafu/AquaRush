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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.ObjectMapper

class UserActionLogFormatterTest :
  FunSpec({
    val formatter = UserActionLogFormatter.instance
    val mapper = ObjectMapper()

    fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")

    afterTest {
      CorrelationIdHolder.clear()
      UserContextHolder.clear()
    }
    test("format should produce basic user action json") {
      val map = parse(formatter.format("CLICK", "button-1"))
      map["level"] shouldBe "INFO"
      map["logger"] shouldBe "user-action"
      map["eventType"] shouldBe "USER_ACTION"
      map["actionType"] shouldBe "CLICK"
      map["target"] shouldBe "button-1"
      map["service"] shouldBe "aqua-rush"
    }
    test("format should include correlation and user context") {
      CorrelationIdHolder.setCorrelationId("corr-1")
      UserContextHolder.setUserContext(UserContext(userId = "42", username = "alice", role = "ADMIN", tenantId = "t1"))
      val map = parse(formatter.format("SEARCH", "products"))
      map["correlationId"] shouldBe "corr-1"
      map["userId"] shouldBe "42"
      map["username"] shouldBe "alice"
      map["userRole"] shouldBe "ADMIN"
      map["tenantId"] shouldBe "t1"
    }
    test("format should merge custom properties") {
      val map = parse(formatter.format("CUSTOM", "target", mapOf("extra" to "value")))
      map["extra"] shouldBe "value"
    }
    test("formatPageView should include page fields") {
      val map = parse(formatter.formatPageView("/home", pageTitle = "首页", referrer = "/login", userAgent = "wechat"))
      map["actionType"] shouldBe "PAGE_VIEW"
      map["pageUrl"] shouldBe "/home"
      map["pageTitle"] shouldBe "首页"
      map["referrer"] shouldBe "/login"
      map["userAgent"] shouldBe "wechat"
    }
    test("formatClick should include element fields") {
      val map =
        parse(
          formatter.formatClick(
            elementId = "btn",
            elementType = "button",
            elementText = "确定",
            coordinates = mapOf("screenX" to 10, "screenY" to 20, "pageX" to 30, "pageY" to 40),
          ),
        )
      map["actionType"] shouldBe "CLICK"
      map["elementId"] shouldBe "btn"
      map["elementText"] shouldBe "确定"
      map["screenX"] shouldBe 10
      map["pageX"] shouldBe 30
    }
    test("formatClick without coordinates should omit coordinate keys") {
      val map = parse(formatter.formatClick(elementId = "btn", elementType = "button"))
      !map.containsKey("screenX") shouldBe true
    }
    test("formatDrag should compute distances") {
      val map =
        parse(
          formatter.formatDrag(
            startCoordinates = mapOf("pageX" to 10, "pageY" to 20),
            endCoordinates = mapOf("pageX" to 40, "pageY" to 60),
            elementId = "drag",
          ),
        )
      map["actionType"] shouldBe "DRAG"
      map["target"] shouldBe "drag"
      map["distanceX"] shouldBe 30
      map["distanceY"] shouldBe 40
    }
    test("formatDrag without element id should default target") {
      val map =
        parse(
          formatter.formatDrag(
            startCoordinates = mapOf("pageX" to 0, "pageY" to 0),
            endCoordinates = mapOf("pageX" to 5, "pageY" to 5),
          ),
        )
      map["target"] shouldBe "drag_operation"
    }
    test("formatInput should sanitize password") {
      val map = parse(formatter.formatInput("pwd-field", "secret123", inputType = "password"))
      map["inputValue"] shouldBe "***"
    }
    test("formatInput should sanitize email") {
      val map = parse(formatter.formatInput("email-field", "alice@example.com", inputType = "email"))
      map["inputValue"] shouldBe "al***@example.com"
    }
    test("formatInput should sanitize short email") {
      val map = parse(formatter.formatInput("email-field", "a@example.com", inputType = "email"))
      map["inputValue"] shouldBe "***@example.com"
    }
    test("formatInput should sanitize invalid email") {
      val map = parse(formatter.formatInput("email-field", "not-an-email", inputType = "email"))
      map["inputValue"] shouldBe "***"
    }
    test("formatInput should sanitize phone") {
      val map = parse(formatter.formatInput("phone-field", "13800138000", inputType = "phone"))
      map["inputValue"] shouldBe "138***8000"
    }
    test("formatInput should sanitize short phone") {
      val map = parse(formatter.formatInput("phone-field", "123", inputType = "tel"))
      map["inputValue"] shouldBe "***"
    }
    test("formatInput should sanitize credit card") {
      val map = parse(formatter.formatInput("card-field", "6222000011112222", inputType = "creditcard"))
      map["inputValue"] shouldBe "6222***2222"
    }
    test("formatInput should sanitize short card") {
      val map = parse(formatter.formatInput("card-field", "1234", inputType = "bankcard"))
      map["inputValue"] shouldBe "***"
    }
    test("formatInput should truncate long text input") {
      val longValue = "x".repeat(150)
      val map = parse(formatter.formatInput("text-field", longValue, inputType = "text"))
      val value = map["inputValue"] as String
      value.contains("[TRUNCATED]") shouldBe true
    }
    test("formatInput should keep short text input") {
      val map = parse(formatter.formatInput("text-field", "hello", inputType = "text"))
      map["inputValue"] shouldBe "hello"
    }
    test("formatInput should include input length") {
      val map = parse(formatter.formatInput("text-field", "hello"))
      map["inputLength"] shouldBe 5
      map["inputType"] shouldBe "text"
    }
    test("formatBackendOperation success should produce INFO") {
      val map = parse(formatter.formatBackendOperation("CREATE", "order", "SUCCESS", "order-1"))
      map["level"] shouldBe "INFO"
      map["actionType"] shouldBe "BACKEND_OPERATION"
      map["module"] shouldBe "ORDER"
    }
    test("formatBackendOperation error should produce ERROR") {
      val map = parse(formatter.formatBackendOperation("CREATE", "order", "FAILURE", "order-1"))
      map["level"] shouldBe "ERROR"
    }
    test("formatBackendOperation warning should produce WARN") {
      val map = parse(formatter.formatBackendOperation("CREATE", "order", "WARNING", "order-1"))
      map["level"] shouldBe "WARN"
    }
    test("formatCustomAction should use provided level") {
      val map = parse(formatter.formatCustomAction("special", "target", level = "ERROR"))
      map["level"] shouldBe "ERROR"
      map["actionType"] shouldBe "SPECIAL"
    }
  })
