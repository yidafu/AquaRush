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

package dev.yidafu.aqua.order.service.impl

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.mapdb.DBMaker

/**
 * Unit tests for OrderIdGeneratorServiceImpl
 */
class OrderIdGeneratorServiceImplTest :
  FunSpec({
    var service: OrderIdGeneratorServiceImpl = mockk()

    beforeTest {
      // Use in-memory MapDB for testing
      val db = DBMaker.memoryDB().make()
      service = OrderIdGeneratorServiceImpl.createWithDb(db)
    }
    test("generateOrderId should return 16 character string") {
      val orderId = service.generateOrderId()

      withClue("Order ID should be 16 characters") { orderId.length shouldBe 16 }
    }
    test("generateOrderId should start with YYMMDD format") {
      val orderId = service.generateOrderId()
      val datePart = orderId.substring(0, 6)

      // Should be digits
      withClue("Date part should be all digits") { datePart.all { it.isDigit() } shouldBe true }

      // Should start with year (2 digits)
      withClue("Year should be numeric") { (datePart.substring(0, 2).toIntOrNull() != null) shouldBe true }
    }
    test("generateOrderId should have 10 digit sequence") {
      val orderId = service.generateOrderId()
      val sequencePart = orderId.substring(6)

      withClue("Sequence should be 10 digits") { sequencePart.length shouldBe 10 }
      withClue("Sequence should be all digits") { sequencePart.all { it.isDigit() } shouldBe true }
    }
    test("generateOrderId should generate unique IDs") {
      val orderIds = mutableSetOf<String>()

      repeat(100) {
        orderIds.add(service.generateOrderId())
      }

      withClue("All generated order IDs should be unique") { orderIds.size shouldBe 100 }
    }
    test("generateOrderId should increment randomly") {
      val firstOrderId = service.generateOrderId()
      val secondOrderId = service.generateOrderId()

      val firstSequence = firstOrderId.substring(6).toLong()
      val secondSequence = secondOrderId.substring(6).toLong()

      // Sequences should be different (random increment 1-1000)
      withClue("Second sequence should be greater than first") { (secondSequence > firstSequence) shouldBe true }
    }
    test("generateOrderId should handle large volume") {
      shouldNotThrowAny {
        repeat(1000) {
          service.generateOrderId()
        }
      }
    }
  })
