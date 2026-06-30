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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mapdb.DBMaker

/**
 * Unit tests for OrderIdGeneratorServiceImpl
 */
class OrderIdGeneratorServiceImplTest {
  private lateinit var service: OrderIdGeneratorServiceImpl

  @BeforeEach
  fun setUp() {
    // Use in-memory MapDB for testing
    val db = DBMaker.memoryDB().make()
    service = OrderIdGeneratorServiceImpl.createWithDb(db)
  }

  @Test
  fun `generateOrderId should return 16 character string`() {
    val orderId = service.generateOrderId()

    assertEquals(16, orderId.length, "Order ID should be 16 characters")
  }

  @Test
  fun `generateOrderId should start with YYMMDD format`() {
    val orderId = service.generateOrderId()
    val datePart = orderId.substring(0, 6)

    // Should be digits
    assertTrue(datePart.all { it.isDigit() }, "Date part should be all digits")

    // Should start with year (2 digits)
    assertTrue(datePart.substring(0, 2).toIntOrNull() != null, "Year should be numeric")
  }

  @Test
  fun `generateOrderId should have 10 digit sequence`() {
    val orderId = service.generateOrderId()
    val sequencePart = orderId.substring(6)

    assertEquals(10, sequencePart.length, "Sequence should be 10 digits")
    assertTrue(sequencePart.all { it.isDigit() }, "Sequence should be all digits")
  }

  @Test
  fun `generateOrderId should generate unique IDs`() {
    val orderIds = mutableSetOf<String>()

    repeat(100) {
      orderIds.add(service.generateOrderId())
    }

    assertEquals(100, orderIds.size, "All generated order IDs should be unique")
  }

  @Test
  fun `generateOrderId should increment randomly`() {
    val firstOrderId = service.generateOrderId()
    val secondOrderId = service.generateOrderId()

    val firstSequence = firstOrderId.substring(6).toLong()
    val secondSequence = secondOrderId.substring(6).toLong()

    // Sequences should be different (random increment 1-1000)
    assertTrue(secondSequence > firstSequence, "Second sequence should be greater than first")
  }

  @Test
  fun `generateOrderId should handle large volume`() {
    assertDoesNotThrow {
      repeat(1000) {
        service.generateOrderId()
      }
    }
  }
}
