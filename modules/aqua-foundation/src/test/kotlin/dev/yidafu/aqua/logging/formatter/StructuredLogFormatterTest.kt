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

class StructuredLogFormatterTest :
  FunSpec({
    val formatter = StructuredLogFormatter.instance
    val mapper = ObjectMapper()

    fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")

    afterTest {
      CorrelationIdHolder.clear()
      UserContextHolder.clear()
    }
    test("format should produce base json fields") {
      val map = parse(formatter.format("info", "test-logger", "hello"))
      map["level"] shouldBe "INFO"
      map["logger"] shouldBe "test-logger"
      map["message"] shouldBe "hello"
      map["service"] shouldBe "aqua-rush"
    }
    test("format should include exception details") {
      val exception = IllegalStateException("boom")
      val map = parse(formatter.format("error", "logger", "msg", exception))
      map["errorType"] shouldBe "IllegalStateException"
      map["errorMessage"] shouldBe "boom"
      (map["errorStackTrace"] as String).contains("IllegalStateException") shouldBe true
    }
    test("format should include context and additional data") {
      CorrelationIdHolder.setCorrelationId("corr")
      UserContextHolder.setUserContext(UserContext(userId = "1", username = "u", role = "R", tenantId = "T"))
      val map = parse(formatter.format("info", "logger", "msg", additionalData = mapOf("custom" to "x")))
      map["correlationId"] shouldBe "corr"
      map["userId"] shouldBe "1"
      map["userRole"] shouldBe "R"
      map["tenantId"] shouldBe "T"
      map["custom"] shouldBe "x"
    }
    test("formatPerformance fast category") {
      val map = parse(formatter.formatPerformance("op", 10))
      map["performanceCategory"] shouldBe "FAST"
      map["level"] shouldBe "INFO"
    }
    test("formatPerformance normal category") {
      val map = parse(formatter.formatPerformance("op", 100))
      map["performanceCategory"] shouldBe "NORMAL"
    }
    test("formatPerformance slow category") {
      val map = parse(formatter.formatPerformance("op", 500))
      map["performanceCategory"] shouldBe "SLOW"
    }
    test("formatPerformance very slow category") {
      val map = parse(formatter.formatPerformance("op", 5000))
      map["performanceCategory"] shouldBe "VERY_SLOW"
    }
    test("formatPerformance failure should produce WARN") {
      val map = parse(formatter.formatPerformance("op", 5000, success = false))
      map["level"] shouldBe "WARN"
      map["success"] shouldBe false
    }
    test("formatPerformance should include operation and duration") {
      val map = parse(formatter.formatPerformance("op", 123))
      map["operation"] shouldBe "op"
      map["durationMs"] shouldBe 123
    }
    test("formatAudit should include audit fields") {
      val map = parse(formatter.formatAudit("DELETE", "order", "SUCCESS", details = mapOf("id" to 1)))
      map["eventType"] shouldBe "AUDIT"
      map["auditAction"] shouldBe "DELETE"
      map["auditResource"] shouldBe "order"
      map["auditResult"] shouldBe "SUCCESS"
      map["auditDetails"] shouldBe mapOf("id" to 1)
    }
    test("formatSecurity critical should produce ERROR") {
      val map = parse(formatter.formatSecurity("LOGIN_FAILED", "CRITICAL", "msg"))
      map["level"] shouldBe "ERROR"
      map["eventType"] shouldBe "SECURITY"
    }
    test("formatSecurity high should produce ERROR") {
      val map = parse(formatter.formatSecurity("X", "HIGH", "msg"))
      map["level"] shouldBe "ERROR"
    }
    test("formatSecurity medium should produce WARN") {
      val map = parse(formatter.formatSecurity("X", "MEDIUM", "msg"))
      map["level"] shouldBe "WARN"
    }
    test("formatSecurity low should produce INFO") {
      val map = parse(formatter.formatSecurity("X", "LOW", "msg"))
      map["level"] shouldBe "INFO"
    }
    test("formatSecurity should include details") {
      val map = parse(formatter.formatSecurity("LOGIN_FAILED", "MEDIUM", "msg", details = mapOf("ip" to "1.2.3.4")))
      map["securityDetails"] shouldBe mapOf("ip" to "1.2.3.4")
    }
    test("formatSecurity should include user context when present") {
      UserContextHolder.setUserContext(UserContext(userId = "9", username = "u"))
      val map = parse(formatter.formatSecurity("X", "LOW", "msg"))
      map["userId"] shouldBe "9"
    }
    test("format should include user context without role and tenant") {
      UserContextHolder.setUserContext(UserContext(userId = "9", username = "u"))
      val map = parse(formatter.format("info", "logger", "msg"))
      map["userId"] shouldBe "9"
      map["username"] shouldBe "u"
    }
  })
