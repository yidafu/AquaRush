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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import tools.jackson.databind.ObjectMapper

class LogBuilderTest :
  FunSpec({
    val mapper = ObjectMapper()

    fun parse(json: String): Map<String, Any> = mapper.readValue(json, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")

    afterTest {
      CorrelationIdHolder.clear()
      UserContextHolder.clear()
    }
    test("buildJson should include default timestamp and context") {
      CorrelationIdHolder.setCorrelationId("corr")
      UserContextHolder.setUserContext(UserContext(userId = "1", username = "u"))
      val json = LogBuilder().message("hi").buildJson()
      val map = parse(json)
      map["message"] shouldBe "hi"
      map["correlationId"] shouldBe "corr"
      map["userId"] shouldBe "1"
      map["timestamp"].shouldNotBeNull()
    }
    test("field should add single field") {
      val builder = LogBuilder().field("key", "value")
      builder.getData()["key"] shouldBe "value"
    }
    test("fields should add multiple fields") {
      val builder = LogBuilder().fields(mapOf("a" to 1, "b" to 2))
      builder.getData()["a"] shouldBe 1
      builder.getData()["b"] shouldBe 2
    }
    test("eventType should set event type") {
      val builder = LogBuilder().eventType("USER_ACTION")
      builder.getData()["eventType"] shouldBe "USER_ACTION"
    }
    test("result success should set success flag") {
      val builder = LogBuilder().result(true)
      builder.getData()["success"] shouldBe true
    }
    test("result failure with error should include error details") {
      val builder = LogBuilder().result(false, IllegalStateException("boom"))
      val data = builder.getData()
      data["success"] shouldBe false
      data["errorType"] shouldBe "IllegalStateException"
      data["errorMessage"] shouldBe "boom"
      (data["errorStackTrace"] as String).contains("boom") shouldBe true
    }
    test("result failure without error should omit error details") {
      val builder = LogBuilder().result(false)
      builder.getData().containsKey("errorType") shouldBe false
    }
    test("performance should add duration and flag") {
      val builder = LogBuilder().performance(123)
      builder.getData()["durationMs"] shouldBe 123L
      builder.getData()["performance"] shouldBe true
    }
    test("audit should add audit fields") {
      val builder = LogBuilder().audit("CREATE", "order", "SUCCESS")
      val data = builder.getData()
      data["audit"] shouldBe true
      data["auditAction"] shouldBe "CREATE"
      data["auditResource"] shouldBe "order"
      data["auditResult"] shouldBe "SUCCESS"
    }
    test("security should add security fields") {
      val builder = LogBuilder().security("LOGIN", "HIGH", details = mapOf("ip" to "1.1.1.1"))
      val data = builder.getData()
      data["security"] shouldBe true
      data["securityEventType"] shouldBe "LOGIN"
      data["securitySeverity"] shouldBe "HIGH"
      data["securityDetails"] shouldBe mapOf("ip" to "1.1.1.1")
    }
    test("security without details should omit details") {
      val builder = LogBuilder().security("LOGIN", "HIGH")
      builder.getData().containsKey("securityDetails") shouldBe false
    }
    test("business should add business fields") {
      val builder = LogBuilder().business("CREATE", "ORDER", "SUCCESS")
      val data = builder.getData()
      data["business"] shouldBe true
      data["businessOperation"] shouldBe "CREATE"
      data["businessModule"] shouldBe "ORDER"
      data["businessResult"] shouldBe "SUCCESS"
    }
    test("buildString should join key-value parts") {
      val result = LogBuilder().message("hi").eventType("EVT").buildString()
      result.contains("message=hi") shouldBe true
      result.contains("eventType=EVT") shouldBe true
    }
    test("buildString should include error and business fields") {
      val result = LogBuilder().result(false, RuntimeException("err")).business("OP", "MOD", "FAILED").buildString()
      result.contains("errorType=RuntimeException") shouldBe true
      result.contains("errorMessage=err") shouldBe true
      result.contains("businessOperation=OP") shouldBe true
    }
    test("buildJson should serialize all chained data") {
      val json =
        LogBuilder()
          .field("trace", "t1")
          .eventType("PERF")
          .performance(10)
          .buildJson()
      val map = parse(json)
      map["trace"] shouldBe "t1"
      map["eventType"] shouldBe "PERF"
      map["durationMs"] shouldBe 10
    }
    test("getData should return immutable snapshot") {
      val builder = LogBuilder()
      val snapshot = builder.getData()
      builder.field("later", 1)
      snapshot.containsKey("later") shouldBe false
    }
  })
