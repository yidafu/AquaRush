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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.util.MoneyUtils
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MonetaryFieldsIntegrationTest :
  FunSpec({
    // ========== OrderModel Tests ==========

    test("OrderModel handles monetary fields correctly") {
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
            orderNo = "TEST-001",
            userId = 100L,
            amountCents = centsInCents,
            addressId = 300L,
          )

        // Test direct cents field
        withClue("Amount in cents should match") { order.amountCents shouldBe centsInCents }

        // Test compatibility property
        withClue("Total amount in yuan should match") { order.totalAmount shouldBe expectedYuan }

        // Test round-trip conversion
        val convertedBack = MoneyUtils.toCents(expectedYuan)
        withClue("Round-trip conversion should match") { convertedBack shouldBe centsInCents }
      }
    }
    test("OrderModel handles large monetary values correctly") {
      val largeCents = 999999999L // ¥9,999,999.99
      val expectedYuan = BigDecimal("9999999.99")

      val order =
        OrderModel(
          id = 1L,
          orderNo = "LARGE-001",
          userId = 100L,
          amountCents = largeCents,
          addressId = 300L,
        )

      order.amountCents shouldBe largeCents
      order.totalAmount shouldBe expectedYuan
      MoneyUtils.fromCents(largeCents) shouldBe expectedYuan
    }
    test("OrderModel maintains monetary precision through operations") {
      val baseOrder =
        OrderModel(
          id = 1L,
          orderNo = "PRECISION-001",
          userId = 100L,
          amountCents = 12345L, // ¥123.45
          addressId = 300L,
        )

      // Test calculations using the monetary field
      val unitPrice = baseOrder.amountCents / 3 // Should be integer division
      val expectedUnitPrice = 4115L // ¥41.15

      unitPrice shouldBe expectedUnitPrice
      MoneyUtils.fromCents(unitPrice) shouldBe BigDecimal("41.15")

      // Test total amount calculation
      val recalculatedTotal = unitPrice * 3
      recalculatedTotal shouldBe baseOrder.amountCents
      MoneyUtils.fromCents(recalculatedTotal) shouldBe baseOrder.totalAmount
    }
    test("OrderModel works with MoneyUtils arithmetic operations") {
      val order1 =
        OrderModel(
          id = 1L,
          orderNo = "ORDER-001",
          userId = 100L,
          amountCents = 10000L, // ¥100.00
          addressId = 300L,
        )

      val order2 =
        OrderModel(
          id = 2L,
          orderNo = "ORDER-002",
          userId = 100L,
          amountCents = 15000L, // ¥150.00
          addressId = 300L,
        )

      // Test addition
      val totalAmount = MoneyUtils.addCents(order1.amountCents, order2.amountCents)
      totalAmount shouldBe 25000L // ¥250.00
      MoneyUtils.fromCents(totalAmount) shouldBe BigDecimal("250.00")

      // Test subtraction
      val difference = MoneyUtils.subtractCents(order2.amountCents, order1.amountCents)
      difference shouldBe 5000L // ¥50.00
      MoneyUtils.fromCents(difference) shouldBe BigDecimal("50.00")

      // Test multiplication (quantity * unit price)
      val multiplied = MoneyUtils.multiplyCents(order1.amountCents, BigDecimal("2.5"))
      multiplied shouldBe 25000L // ¥250.00
      MoneyUtils.fromCents(multiplied) shouldBe BigDecimal("250.00")

      // Test percentage calculation
      val percentage = MoneyUtils.calculatePercentage(order1.amountCents, BigDecimal("10.0"))
      percentage shouldBe 1000L // ¥10.00
      MoneyUtils.fromCents(percentage) shouldBe BigDecimal("10.00")
    }
    test("OrderModel maintains precision with boundary values") {
      // Test zero amount
      val zeroOrder =
        OrderModel(
          id = 1L,
          orderNo = "ZERO-001",
          userId = 100L,
          amountCents = 0L,
          addressId = 300L,
        )
      zeroOrder.totalAmount shouldBe BigDecimal("0.00")

      // Test minimum positive amount (1 cent)
      val minOrder =
        OrderModel(
          id = 2L,
          orderNo = "MIN-001",
          userId = 100L,
          amountCents = 1L, // ¥0.01
          addressId = 300L,
        )
      minOrder.totalAmount shouldBe BigDecimal("0.01")

      // Test single yuan
      val oneYuanOrder =
        OrderModel(
          id = 3L,
          orderNo = "ONE-001",
          userId = 100L,
          amountCents = 100L, // ¥1.00
          addressId = 300L,
        )
      oneYuanOrder.totalAmount shouldBe BigDecimal("1.00")
    }
    test("OrderModel handles monetary formatting correctly") {
      val order =
        OrderModel(
          id = 1L,
          orderNo = "FORMAT-001",
          userId = 100L,
          amountCents = 12345L, // ¥123.45
          addressId = 300L,
        )

      // Test formatting using MoneyUtils
      val formattedCents = MoneyUtils.formatCents(order.amountCents)
      val formattedYuan = MoneyUtils.formatYuan(order.totalAmount)

      formattedCents shouldBe "¥123.45"
      formattedYuan shouldBe "¥123.45"
      formattedYuan shouldBe formattedCents
    }
    test("OrderModel maintains consistency across monetary operations") {
      val testAmounts = listOf(100L, 150L, 999L, 12345L, 999999L)

      for (amount in testAmounts) {
        val order =
          OrderModel(
            id = 1L,
            orderNo = "CONSISTENCY-001",
            userId = 100L,
            amountCents = amount,
            addressId = 300L,
          )

        // Verify internal consistency
        val yuanFromCents = MoneyUtils.fromCents(amount)
        order.totalAmount shouldBe yuanFromCents

        // Verify MoneyUtils compatibility
        MoneyUtils.validateCentsConversion(amount, order.totalAmount) shouldBe true

        // Verify formatting consistency
        val formattedCents = MoneyUtils.formatCents(amount)
        val formattedYuan = MoneyUtils.formatYuan(order.totalAmount)
        formattedYuan shouldBe formattedCents
      }
    }
    test("OrderModel works correctly in collection operations") {
      val orders =
        listOf(
          OrderModel(orderNo = "ORDER-001", userId = 100L, amountCents = 10000L, addressId = 300L), // ¥100.00
          OrderModel(orderNo = "ORDER-002", userId = 100L, amountCents = 15000L, addressId = 300L), // ¥150.00
          OrderModel(orderNo = "ORDER-003", userId = 100L, amountCents = 20000L, addressId = 300L), // ¥200.00
          OrderModel(orderNo = "ORDER-004", userId = 100L, amountCents = 5000L, addressId = 300L), // ¥50.00
        )

      // Test aggregation operations
      val totalCents = orders.sumOf { it.amountCents }
      totalCents shouldBe 50000L // ¥500.00

      val totalYuan = orders.sumOf { it.totalAmount }
      totalYuan shouldBe BigDecimal("500.00")

      // Test average calculations
      val avgCents = totalCents / orders.size
      avgCents shouldBe 12500L // ¥125.00

      // Test filtering and mapping with monetary operations
      val expensiveOrders = orders.filter { it.amountCents > 10000L }
      expensiveOrders.size shouldBe 2

      val formattedExpensiveOrders = expensiveOrders.map { MoneyUtils.formatCents(it.amountCents) }
      formattedExpensiveOrders.all { it.startsWith("¥") } shouldBe true
    }
    test("OrderModel validates monetary data integrity") {
      // Test with valid amounts
      val validAmounts = listOf(0L, 1L, 100L, 999L, 12345L, 999999L)
      validAmounts.forEach { amount ->
        shouldNotThrowAny {
          OrderModel(
            id = 1L,
            orderNo = "VALID-001",
            userId = 100L,
            amountCents = amount,
            addressId = 300L,
          )
        }
      }

      // Note: OrderModel itself doesn't validate negative amounts since it's a data class,
      // but service layer should. This test documents the expected behavior.
      val negativeOrder =
        OrderModel(
          id = 2L,
          orderNo = "INVALID-001",
          userId = 100L,
          amountCents = -100L,
          addressId = 300L,
        )

      // The model allows negative amounts, but MoneyUtils should handle validation
      shouldThrow<IllegalArgumentException> {
        MoneyUtils.fromCents(negativeOrder.amountCents)
      }
    }
  })
