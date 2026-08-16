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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class LoggingContextTest :
  FunSpec({

    afterTest {
      CorrelationIdHolder.clear()
      UserContextHolder.clear()
    }
    test("put get remove should manage custom data") {
      val context = LoggingContext.current()
      context.put("key", "value")
      context.get("key") shouldBe "value"
      context.remove("key")
      context.get("key").shouldBeNull()
    }
    test("getAllCustomData should return snapshot") {
      val context = LoggingContext.current()
      context.put("a", 1)
      context.put("b", 2)
      context.getAllCustomData() shouldBe mapOf("a" to 1, "b" to 2)
    }
    test("clearCustomData should remove all custom data") {
      val context = LoggingContext.current()
      context.put("a", 1)
      context.clearCustomData()
      context.getAllCustomData().isEmpty() shouldBe true
    }
    test("correlationId should reflect holder state") {
      CorrelationIdHolder.setCorrelationId("cid")
      val context = LoggingContext.current()
      context.correlationId shouldBe "cid"
    }
    test("userContext should reflect holder state") {
      UserContextHolder.setUserContext(UserContext(userId = "1", username = "u"))
      val context = LoggingContext.current()
      context.userContext?.userId shouldBe "1"
    }
    test("startTime should be set") {
      LoggingContext.current().startTime.shouldNotBeNull()
    }
    test("buildContextMap should include correlation and user info") {
      CorrelationIdHolder.setCorrelationId("cid")
      UserContextHolder.setUserContext(UserContext(userId = "1", username = "u", role = "R", tenantId = "T"))
      val context = LoggingContext.current()
      context.put("custom", "x")
      val map = context.buildContextMap()
      map["correlationId"] shouldBe "cid"
      map["userId"] shouldBe "1"
      map["username"] shouldBe "u"
      map["userRole"] shouldBe "R"
      map["tenantId"] shouldBe "T"
      map["custom"] shouldBe "x"
      map["timestamp"].shouldNotBeNull()
    }
    test("buildContextMap should include additional user info") {
      UserContextHolder.setUserContext(UserContext(userId = "1", username = "u", additionalInfo = mapOf("dept" to "ops")))
      val map = LoggingContext.current().buildContextMap()
      map["dept"] shouldBe "ops"
    }
    test("buildContextMap without context should only include timestamp") {
      val map = LoggingContext.current().buildContextMap()
      !map.containsKey("correlationId") shouldBe true
      !map.containsKey("userId") shouldBe true
      map["timestamp"].shouldNotBeNull()
    }
    test("current should create fresh context each call") {
      val a = LoggingContext.current()
      val b = LoggingContext.current()
      (a !== b) shouldBe true
    }
  })
