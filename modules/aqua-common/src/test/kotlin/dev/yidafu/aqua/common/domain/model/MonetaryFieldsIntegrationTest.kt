/*
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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.utils.MoneyUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("Domain Models Monetary Fields Integration Tests")
class MonetaryFieldsIntegrationTest {
  // ========== OrderModel Tests ==========

  @Test
  @DisplayName("OrderModel should handle monetary fields correctly")
  fun `OrderModel handles monetary fields correctly`() {
    val testCases =
      listOf(
        Pair(0L, BigDecimal("0.00")),
        Pair(100L, BigDecimal("1.00")),
        Pair(150L, BigDecimal("1.50")),
        Pair(999L, BigDecimal("9.99")),
        Pair(123456L, BigDecimal("1234.56")),
      )

    for ((centsInCents, expectedYuan) in testCases) {
      val order =
        OrderModel(
          id = 1L,
          orderNumber = "TEST-001",
          userId = 100L,
          productId = 200L,
          quantity = 1,
          amountCents = centsInCents,
          addressId = 300L,
          deliveryAddressId = 301L,
        )

      // Test direct cents field
      assertEquals(centsInCents, order.amountCents, "Amount in cents should match")

      // Test compatibility property
      assertEquals(expectedYuan, order.totalAmount, "Total amount in yuan should match")

      // Test round-trip conversion
      val convertedBack = MoneyUtils.toCents(expectedYuan)
      assertEquals(centsInCents, convertedBack, "Round-trip conversion should match")
    }
  }

  @Test
  @DisplayName("OrderModel should handle large monetary values correctly")
  fun `OrderModel handles large monetary values correctly`() {
    val largeCents = 999999999L // ¥9,999,999.99
    val expectedYuan = BigDecimal("9999999.99")

    val order =
      OrderModel(
        id = 1L,
        orderNumber = "LARGE-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = largeCents,
        addressId = 300L,
        deliveryAddressId = 301L,
      )

    assertEquals(largeCents, order.amountCents)
    assertEquals(expectedYuan, order.totalAmount)
    assertEquals(expectedYuan, MoneyUtils.fromCents(largeCents))
  }

  @Test
  @DisplayName("OrderModel should maintain monetary precision through operations")
  fun `OrderModel maintains monetary precision through operations`() {
    val baseOrder =
      OrderModel(
        id = 1L,
        orderNumber = "PRECISION-001",
        userId = 100L,
        productId = 200L,
        quantity = 3,
        amountCents = 12345L, // ¥123.45
        addressId = 300L,
        deliveryAddressId = 301L,
      )

    // Test calculations using the monetary field
    val unitPrice = baseOrder.amountCents / baseOrder.quantity // Should be integer division
    val expectedUnitPrice = 4115L // ¥41.15

    assertEquals(expectedUnitPrice, unitPrice)
    assertEquals(BigDecimal("41.15"), MoneyUtils.fromCents(unitPrice))

    // Test total amount calculation
    val recalculatedTotal = unitPrice * baseOrder.quantity
    assertEquals(baseOrder.amountCents, recalculatedTotal)
    assertEquals(baseOrder.totalAmount, MoneyUtils.fromCents(recalculatedTotal))
  }

  @Test
  @DisplayName("OrderModel should work correctly with MoneyUtils arithmetic operations")
  fun `OrderModel works with MoneyUtils arithmetic operations`() {
    val order1 =
      OrderModel(
        id = 1L,
        orderNumber = "ORDER-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = 10000L, // ¥100.00
        addressId = 300L,
        deliveryAddressId = 301L,
      )

    val order2 =
      OrderModel(
        id = 2L,
        orderNumber = "ORDER-002",
        userId = 100L,
        productId = 201L,
        quantity = 1,
        amountCents = 15000L, // ¥150.00
        addressId = 300L,
        deliveryAddressId = 301L,
      )

    // Test addition
    val totalAmount = MoneyUtils.addCents(order1.amountCents, order2.amountCents)
    assertEquals(25000L, totalAmount) // ¥250.00
    assertEquals(BigDecimal("250.00"), MoneyUtils.fromCents(totalAmount))

    // Test subtraction
    val difference = MoneyUtils.subtractCents(order2.amountCents, order1.amountCents)
    assertEquals(5000L, difference) // ¥50.00
    assertEquals(BigDecimal("50.00"), MoneyUtils.fromCents(difference))

    // Test multiplication (quantity * unit price)
    val multiplied = MoneyUtils.multiplyCents(order1.amountCents, BigDecimal("2.5"))
    assertEquals(25000L, multiplied) // ¥250.00
    assertEquals(BigDecimal("250.00"), MoneyUtils.fromCents(multiplied))

    // Test percentage calculation
    val percentage = MoneyUtils.calculatePercentage(order1.amountCents, BigDecimal("10.0"))
    assertEquals(1000L, percentage) // ¥10.00
    assertEquals(BigDecimal("10.00"), MoneyUtils.fromCents(percentage))
  }

  @Test
  @DisplayName("OrderModel should maintain monetary precision with boundary values")
  fun `OrderModel maintains precision with boundary values`() {
    // Test zero amount
    val zeroOrder =
      OrderModel(
        id = 1L,
        orderNumber = "ZERO-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = 0L,
        addressId = 300L,
        deliveryAddressId = 301L,
      )
    assertEquals(BigDecimal("0.00"), zeroOrder.totalAmount)

    // Test minimum positive amount (1 cent)
    val minOrder =
      OrderModel(
        id = 2L,
        orderNumber = "MIN-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = 1L, // ¥0.01
        addressId = 300L,
        deliveryAddressId = 301L,
      )
    assertEquals(BigDecimal("0.01"), minOrder.totalAmount)

    // Test single yuan
    val oneYuanOrder =
      OrderModel(
        id = 3L,
        orderNumber = "ONE-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = 100L, // ¥1.00
        addressId = 300L,
        deliveryAddressId = 301L,
      )
    assertEquals(BigDecimal("1.00"), oneYuanOrder.totalAmount)
  }

  @Test
  @DisplayName("OrderModel should handle monetary formatting correctly")
  fun `OrderModel handles monetary formatting correctly`() {
    val order =
      OrderModel(
        id = 1L,
        orderNumber = "FORMAT-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = 12345L, // ¥123.45
        addressId = 300L,
        deliveryAddressId = 301L,
      )

    // Test formatting using MoneyUtils
    val formattedCents = MoneyUtils.formatCents(order.amountCents)
    val formattedYuan = MoneyUtils.formatYuan(order.totalAmount)

    assertEquals("¥123.45", formattedCents)
    assertEquals("¥123.45", formattedYuan)
    assertEquals(formattedCents, formattedYuan)
  }

  @Test
  @DisplayName("OrderModel should maintain consistency across monetary operations")
  fun `OrderModel maintains consistency across monetary operations`() {
    val testAmounts = listOf(100L, 150L, 999L, 12345L, 999999L)

    for (amount in testAmounts) {
      val order =
        OrderModel(
          id = 1L,
          orderNumber = "CONSISTENCY-001",
          userId = 100L,
          productId = 200L,
          quantity = 1,
          amountCents = amount,
          addressId = 300L,
          deliveryAddressId = 301L,
        )

      // Verify internal consistency
      val yuanFromCents = MoneyUtils.fromCents(amount)
      assertEquals(yuanFromCents, order.totalAmount)

      // Verify MoneyUtils compatibility
      assertTrue(MoneyUtils.validateCentsConversion(amount, order.totalAmount))

      // Verify formatting consistency
      val formattedCents = MoneyUtils.formatCents(amount)
      val formattedYuan = MoneyUtils.formatYuan(order.totalAmount)
      assertEquals(formattedCents, formattedYuan)
    }
  }

  @Test
  @DisplayName("OrderModel should work correctly in collection operations")
  fun `OrderModel works correctly in collection operations`() {
    val orders =
      listOf(
        OrderModel(1L, "ORDER-001", 100L, 200L, 1, 10000L, 300L, 301L), // ¥100.00
        OrderModel(2L, "ORDER-002", 100L, 201L, 2, 15000L, 300L, 301L), // ¥150.00
        OrderModel(3L, "ORDER-003", 100L, 202L, 1, 20000L, 300L, 301L), // ¥200.00
        OrderModel(4L, "ORDER-004", 100L, 203L, 3, 5000L, 300L, 301L), // ¥50.00
      )

    // Test aggregation operations
    val totalCents = orders.sumOf { it.amountCents }
    assertEquals(50000L, totalCents) // ¥500.00

    val totalYuan = orders.sumOf { it.totalAmount }
    assertEquals(BigDecimal("500.00"), totalYuan)

    // Test average calculations
    val avgCents = totalCents / orders.size
    assertEquals(12500L, avgCents) // ¥125.00

    // Test filtering and mapping with monetary operations
    val expensiveOrders = orders.filter { it.amountCents > 10000L }
    assertEquals(2, expensiveOrders.size)

    val formattedExpensiveOrders = expensiveOrders.map { MoneyUtils.formatCents(it.amountCents) }
    assertTrue(formattedExpensiveOrders.all { it.startsWith("¥") })
  }

  @Test
  @DisplayName("OrderModel should validate monetary data integrity")
  fun `OrderModel validates monetary data integrity`() {
    // Test with valid amounts
    val validAmounts = listOf(0L, 1L, 100L, 999L, 12345L, 999999L)
    validAmounts.forEach { amount ->
      assertDoesNotThrow {
        OrderModel(
          id = 1L,
          orderNumber = "VALID-001",
          userId = 100L,
          productId = 200L,
          quantity = 1,
          amountCents = amount,
          addressId = 300L,
          deliveryAddressId = 301L,
        )
      }
    }

    // Note: OrderModel itself doesn't validate negative amounts since it's a data class,
    // but service layer should. This test documents the expected behavior.
    val negativeOrder =
      OrderModel(
        id = 2L,
        orderNumber = "INVALID-001",
        userId = 100L,
        productId = 200L,
        quantity = 1,
        amountCents = -100L,
        addressId = 300L,
        deliveryAddressId = 301L,
      )

    // The model allows negative amounts, but MoneyUtils should handle validation
    assertThrows<IllegalArgumentException> {
      MoneyUtils.fromCents(negativeOrder.amountCents)
    }
  }
}
