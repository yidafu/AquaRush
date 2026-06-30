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

package dev.yidafu.aqua.common.migration

import dev.yidafu.aqua.common.utils.MoneyUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("Monetary Fields Migration Tests")
class MonetaryFieldsMigrationTest {
  // Mock database state before migration
  data class PreMigrationProduct(
    val id: Long,
    val name: String,
    val price: BigDecimal?, // DECIMAL(10,2) in yuan
    val status: String,
  )

  data class PreMigrationOrder(
    val id: Long,
    val orderNo: String,
    val totalAmount: BigDecimal?, // DECIMAL(10,2) in yuan
    val status: String,
  )

  data class PreMigrationPayment(
    val id: Long,
    val amount: BigDecimal?, // DECIMAL(10,2) in yuan
    val transactionId: String?,
  )

  // Expected database state after migration
  data class PostMigrationProduct(
    val id: Long,
    val name: String,
    val price: Long?, // BIGINT in cents
    val status: String,
  )

  data class PostMigrationOrder(
    val id: Long,
    val orderNo: String,
    val totalAmount: Long?, // BIGINT in cents
    val status: String,
  )

  data class PostMigrationPayment(
    val id: Long,
    val amount: Long?, // BIGINT in cents
    val transactionId: String?,
  )

  private val preMigrationProducts =
    listOf(
      PreMigrationProduct(1L, "Bottled Water 500ml", BigDecimal("12.50"), "ONLINE"),
      PreMigrationProduct(2L, "Bottled Water 1L", BigDecimal("18.99"), "ONLINE"),
      PreMigrationProduct(3L, "Premium Water 500ml", BigDecimal("25.00"), "OFFLINE"),
      PreMigrationProduct(4L, "Mineral Water 500ml", BigDecimal("0.99"), "ONLINE"),
      PreMigrationProduct(5L, "Spring Water 2L", BigDecimal("35.75"), "ONLINE"),
      PreMigrationProduct(6L, "Free Sample", BigDecimal("0.00"), "OFFLINE"),
      PreMigrationProduct(7L, "Expensive Water", BigDecimal("999.99"), "ONLINE"),
    )

  private val preMigrationOrders =
    listOf(
      PreMigrationOrder(101L, "ORDER-001", BigDecimal("25.00"), "COMPLETED"),
      PreMigrationOrder(102L, "ORDER-002", BigDecimal("37.98"), "PENDING_DELIVERY"),
      PreMigrationOrder(103L, "ORDER-003", BigDecimal("12.50"), "PENDING_PAYMENT"),
      PreMigrationOrder(104L, "ORDER-004", BigDecimal("61.75"), "DELIVERING"),
      PreMigrationOrder(105L, "ORDER-005", BigDecimal("0.00"), "CANCELLED"),
    )

  private val preMigrationPayments =
    listOf(
      PreMigrationPayment(201L, BigDecimal("25.00"), "TXN-001"),
      PreMigrationPayment(202L, BigDecimal("37.98"), "TXN-002"),
      PreMigrationPayment(203L, BigDecimal("12.50"), "TXN-003"),
      PreMigrationPayment(204L, BigDecimal("61.75"), "TXN-004"),
    )

  @Test
  @DisplayName("Should convert decimal yuan values to cents correctly")
  fun `convert decimal yuan to cents correctly`() {
    // Test conversion of all sample products
    val convertedProducts =
      preMigrationProducts.map { product ->
        PostMigrationProduct(
          id = product.id,
          name = product.name,
          price = product.price?.let { MoneyUtils.toCents(it) },
          status = product.status,
        )
      }

    val expectedProducts =
      listOf(
        PostMigrationProduct(1L, "Bottled Water 500ml", 1250L, "ONLINE"),
        PostMigrationProduct(2L, "Bottled Water 1L", 1899L, "ONLINE"),
        PostMigrationProduct(3L, "Premium Water 500ml", 2500L, "OFFLINE"),
        PostMigrationProduct(4L, "Mineral Water 500ml", 99L, "ONLINE"),
        PostMigrationProduct(5L, "Spring Water 2L", 3575L, "ONLINE"),
        PostMigrationProduct(6L, "Free Sample", 0L, "OFFLINE"),
        PostMigrationProduct(7L, "Expensive Water", 99999L, "ONLINE"),
      )

    assertEquals(expectedProducts.size, convertedProducts.size)
    expectedProducts.zip(convertedProducts).forEach { (expected, actual) ->
      assertEquals(expected.id, actual.id)
      assertEquals(expected.name, actual.name)
      assertEquals(expected.price, actual.price, "Price conversion failed for ${expected.name}")
      assertEquals(expected.status, actual.status)
    }
  }

  @Test
  @DisplayName("Should convert decimal order amounts to cents correctly")
  fun `convert decimal order amounts to cents correctly`() {
    val convertedOrders =
      preMigrationOrders.map { order ->
        PostMigrationOrder(
          id = order.id,
          orderNo = order.orderNo,
          totalAmount = order.totalAmount?.let { MoneyUtils.toCents(it) },
          status = order.status,
        )
      }

    val expectedOrders =
      listOf(
        PostMigrationOrder(101L, "ORDER-001", 2500L, "COMPLETED"),
        PostMigrationOrder(102L, "ORDER-002", 3798L, "PENDING_DELIVERY"),
        PostMigrationOrder(103L, "ORDER-003", 1250L, "PENDING_PAYMENT"),
        PostMigrationOrder(104L, "ORDER-004", 6175L, "DELIVERING"),
        PostMigrationOrder(105L, "ORDER-005", 0L, "CANCELLED"),
      )

    assertEquals(expectedOrders.size, convertedOrders.size)
    expectedOrders.zip(convertedOrders).forEach { (expected, actual) ->
      assertEquals(expected.id, actual.id)
      assertEquals(expected.orderNo, actual.orderNo)
      assertEquals(expected.totalAmount, actual.totalAmount, "Amount conversion failed for ${expected.orderNo}")
      assertEquals(expected.status, actual.status)
    }
  }

  @Test
  @DisplayName("Should convert decimal payment amounts to cents correctly")
  fun `convert decimal payment amounts to cents correctly`() {
    val convertedPayments =
      preMigrationPayments.map { payment ->
        PostMigrationPayment(
          id = payment.id,
          amount = payment.amount?.let { MoneyUtils.toCents(it) },
          transactionId = payment.transactionId,
        )
      }

    val expectedPayments =
      listOf(
        PostMigrationPayment(201L, 2500L, "TXN-001"),
        PostMigrationPayment(202L, 3798L, "TXN-002"),
        PostMigrationPayment(203L, 1250L, "TXN-003"),
        PostMigrationPayment(204L, 6175L, "TXN-004"),
      )

    assertEquals(expectedPayments.size, convertedPayments.size)
    expectedPayments.zip(convertedPayments).forEach { (expected, actual) ->
      assertEquals(expected.id, actual.id)
      assertEquals(expected.amount, actual.amount, "Payment amount conversion failed for ${expected.transactionId}")
      assertEquals(expected.transactionId, actual.transactionId)
    }
  }

  @Test
  @DisplayName("Should handle null values correctly during migration")
  fun `handle null values correctly during migration`() {
    val productsWithNulls =
      listOf(
        PreMigrationProduct(1L, "Product 1", null, "ONLINE"),
        PreMigrationProduct(2L, "Product 2", BigDecimal("10.00"), "OFFLINE"),
        PreMigrationProduct(3L, "Product 3", null, "ONLINE"),
      )

    val convertedProducts =
      productsWithNulls.map { product ->
        PostMigrationProduct(
          id = product.id,
          name = product.name,
          price = product.price?.let { MoneyUtils.toCents(it) },
          status = product.status,
        )
      }

    assertNull(convertedProducts[0].price, "Null price should remain null after conversion")
    assertEquals(1000L, convertedProducts[1].price, "Non-null price should be converted")
    assertNull(convertedProducts[2].price, "Null price should remain null after conversion")
  }

  @Test
  @DisplayName("Should validate monetary precision after migration")
  fun `validate monetary precision after migration`() {
    // Test edge cases that could cause precision loss
    val edgeCases =
      listOf(
        BigDecimal("0.01"), // Minimum positive amount
        BigDecimal("0.10"), // One jiao
        BigDecimal("1.00"), // One yuan
        BigDecimal("1.23"), // Typical amount
        BigDecimal("9.99"), // Two decimal places before 10
        BigDecimal("10.00"), // Round number
        BigDecimal("99.99"), // Two decimal places before 100
        BigDecimal("123.45"), // Common price point
        BigDecimal("999.99"), // Maximum typical consumer price
      )

    edgeCases.forEach { yuanAmount ->
      val cents = MoneyUtils.toCents(yuanAmount)
      val convertedBack = MoneyUtils.fromCents(cents)

      assertEquals(
        yuanAmount,
        convertedBack,
        "Precision loss detected during round-trip conversion of $yuanAmount",
      )

      // Verify the exact cents value
      val expectedCents = (yuanAmount * BigDecimal(100)).toLong()
      assertEquals(
        expectedCents,
        cents,
        "Incorrect cents calculation for $yuanAmount",
      )
    }
  }

  @Test
  @DisplayName("Should handle boundary values correctly during migration")
  fun `handle boundary values correctly during migration`() {
    val boundaryCases =
      listOf(
        BigDecimal("0.00"), // Zero
        BigDecimal("0.01"), // Minimum positive
        BigDecimal("0.99"), // Just under 1 yuan
        BigDecimal("99999.99"), // Large amount
      )

    boundaryCases.forEach { yuanAmount ->
      assertDoesNotThrow("Should handle boundary value: $yuanAmount") {
        val cents = MoneyUtils.toCents(yuanAmount)
        val convertedBack = MoneyUtils.fromCents(cents)
        assertEquals(yuanAmount, convertedBack)
      }
    }
  }

  @Test
  @DisplayName("Should reject invalid monetary values during migration")
  fun `reject invalid monetary values during migration`() {
    val invalidCases =
      listOf(
        BigDecimal("-1.00"), // Negative amount
        BigDecimal("1.001"), // Too many decimal places
        BigDecimal("1.2345"), // Too many decimal places
      )

    invalidCases.forEach { invalidAmount ->
      assertThrows<IllegalArgumentException>("Should reject invalid amount: $invalidAmount") {
        MoneyUtils.toCents(invalidAmount)
      }
    }
  }

  @Test
  @DisplayName("Should verify database migration integrity")
  fun `verify database migration integrity`() {
    // Simulate a complete migration and verify integrity

    // Step 1: Convert all monetary fields
    val migratedProducts =
      preMigrationProducts.map { product ->
        PostMigrationProduct(
          id = product.id,
          name = product.name,
          price = product.price?.let { MoneyUtils.toCents(it) },
          status = product.status,
        )
      }

    val migratedOrders =
      preMigrationOrders.map { order ->
        PostMigrationOrder(
          id = order.id,
          orderNo = order.orderNo,
          totalAmount = order.totalAmount?.let { MoneyUtils.toCents(it) },
          status = order.status,
        )
      }

    val migratedPayments =
      preMigrationPayments.map { payment ->
        PostMigrationPayment(
          id = payment.id,
          amount = payment.amount?.let { MoneyUtils.toCents(it) },
          transactionId = payment.transactionId,
        )
      }

    // Step 2: Verify all amounts are non-negative (database constraint)
    migratedProducts.forEach { product ->
      assertTrue(
        product.price == null || product.price >= 0,
        "Product price should be non-negative: ${product.name}",
      )
    }

    migratedOrders.forEach { order ->
      assertTrue(
        order.totalAmount == null || order.totalAmount >= 0,
        "Order amount should be non-negative: ${order.orderNo}",
      )
    }

    migratedPayments.forEach { payment ->
      assertTrue(
        payment.amount == null || payment.amount > 0,
        "Payment amount should be positive: ${payment.transactionId}",
      )
    }

    // Step 3: Verify total value calculations still work
    val totalProductValue =
      migratedProducts
        .mapNotNull { it.price }
        .sumOf { it }

    val expectedTotalProductValue =
      preMigrationProducts
        .mapNotNull { it.price }
        .map { MoneyUtils.toCents(it) }
        .sumOf { it }

    assertEquals(
      expectedTotalProductValue,
      totalProductValue,
      "Total product value should be preserved",
    )

    // Step 4: Verify business logic still works
    val expensiveProducts = migratedProducts.filter { it.price != null && it.price > 2000L }
    assertEquals(3, expensiveProducts.size, "Should find correct number of expensive products")

    val largeOrders = migratedOrders.filter { it.totalAmount != null && it.totalAmount > 3000L }
    assertEquals(2, largeOrders.size, "Should find correct number of large orders")
  }

  @Test
  @DisplayName("Should verify rollback capability")
  fun `verify rollback capability`() {
    // Test rollback by converting cents back to decimal
    val migratedProducts =
      preMigrationProducts.map { product ->
        PostMigrationProduct(
          id = product.id,
          name = product.name,
          price = product.price?.let { MoneyUtils.toCents(it) },
          status = product.status,
        )
      }

    // Simulate rollback by converting back to yuan
    val rolledBackProducts =
      migratedProducts.map { product ->
        PreMigrationProduct(
          id = product.id,
          name = product.name,
          price = product.price?.let { MoneyUtils.fromCents(it) },
          status = product.status,
        )
      }

    // Verify rollback is accurate
    assertEquals(preMigrationProducts.size, rolledBackProducts.size)
    preMigrationProducts.zip(rolledBackProducts).forEach { (original, rolledBack) ->
      assertEquals(original.id, rolledBack.id)
      assertEquals(original.name, rolledBack.name)
      assertEquals(
        original.price,
        rolledBack.price,
        "Rollback failed for ${original.name}: expected ${original.price}, got ${rolledBack.price}",
      )
      assertEquals(original.status, rolledBack.status)
    }
  }

  @Test
  @DisplayName("Should test migration performance with large datasets")
  fun `test migration performance with large datasets`() {
    // Generate a large dataset
    val largeDataset =
      (1L..10000L).map { id ->
        PreMigrationProduct(
          id = id,
          name = "Product $id",
          price = BigDecimal((id % 1000).toString() + ".99"),
          status = if (id % 2 == 0L) "ONLINE" else "OFFLINE",
        )
      }

    // Measure conversion performance
    val startTime = System.currentTimeMillis()

    val convertedDataset =
      largeDataset.map { product ->
        PostMigrationProduct(
          id = product.id,
          name = product.name,
          price = product.price?.let { MoneyUtils.toCents(it) },
          status = product.status,
        )
      }

    val endTime = System.currentTimeMillis()
    val conversionTime = endTime - startTime

    // Verify conversion is complete and accurate
    assertEquals(largeDataset.size, convertedDataset.size)

    // Performance check: should complete 10,000 conversions in reasonable time
    assertTrue(
      conversionTime < 5000,
      "Migration should complete quickly. Took ${conversionTime}ms for 10,000 records",
    )

    // Spot check some conversions
    val sampleProducts =
      listOf(1L, 5000L, 9999L).map { id ->
        convertedDataset.find { it.id == id }
      }

    sampleProducts.forEach { product ->
      assertNotNull(product)
      assertEquals(product!!.price % 100, 99L, "Should preserve .99 pattern")
    }
  }
}
