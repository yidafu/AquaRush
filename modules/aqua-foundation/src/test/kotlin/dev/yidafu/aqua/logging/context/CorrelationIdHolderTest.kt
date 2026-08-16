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
import io.kotest.matchers.shouldNotBe

class CorrelationIdHolderTest :
  FunSpec({

    afterTest {
      CorrelationIdHolder.clear()
    }
    test("set and get should round trip") {
      CorrelationIdHolder.setCorrelationId("abc")
      CorrelationIdHolder.getCorrelationId() shouldBe "abc"
    }
    test("get should return null when unset") {
      CorrelationIdHolder.getCorrelationId().shouldBeNull()
    }
    test("generateAndSet should produce non-empty id") {
      val id = CorrelationIdHolder.generateAndSet()
      id.shouldNotBeNull()
      (id.length >= 32) shouldBe true
      CorrelationIdHolder.getCorrelationId() shouldBe id
    }
    test("generateAndSet should produce unique ids") {
      CorrelationIdHolder.generateAndSet() shouldNotBe CorrelationIdHolder.generateAndSet()
    }
    test("clear should remove correlation id") {
      CorrelationIdHolder.setCorrelationId("abc")
      CorrelationIdHolder.clear()
      CorrelationIdHolder.getCorrelationId().shouldBeNull()
    }
    test("hasCorrelationId should reflect state") {
      CorrelationIdHolder.hasCorrelationId() shouldBe false
      CorrelationIdHolder.setCorrelationId("abc")
      CorrelationIdHolder.hasCorrelationId() shouldBe true
      CorrelationIdHolder.clear()
      CorrelationIdHolder.hasCorrelationId() shouldBe false
    }
  })
