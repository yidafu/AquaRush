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

import dev.yidafu.aqua.common.util.MoneyUtils
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MonetaryFieldsMigrationTest :
  FunSpec({
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
    val preMigrationProducts =
      listOf(
        PreMigrationProduct(1L, "Bottled Water 500ml", BigDecimal("12.50"), "ONLINE"),
        PreMigrationProduct(2L, "Bottled Water 1L", BigDecimal("18.99"), "ONLINE"),
        PreMigrationProduct(3L, "Premium Water 500ml", BigDecimal("25.00"), "OFFLINE"),
        PreMigrationProduct(4L, "Mineral Water 500ml", BigDecimal("0.99"), "ONLINE"),
        PreMigrationProduct(5L, "Spring Water 2L", BigDecimal("35.75"), "ONLINE"),
        PreMigrationProduct(6L, "Free Sample", BigDecimal("0.00"), "OFFLINE"),
        PreMigrationProduct(7L, "Expensive Water", BigDecimal("999.99"), "ONLINE"),
      )
    val preMigrationOrders =
      listOf(
        PreMigrationOrder(101L, "ORDER-001", BigDecimal("25.00"), "COMPLETED"),
        PreMigrationOrder(102L, "ORDER-002", BigDecimal("37.98"), "PENDING_DELIVERY"),
        PreMigrationOrder(103L, "ORDER-003", BigDecimal("12.50"), "PENDING_PAYMENT"),
        PreMigrationOrder(104L, "ORDER-004", BigDecimal("61.75"), "DELIVERING"),
        PreMigrationOrder(105L, "ORDER-005", BigDecimal("0.00"), "CANCELLED"),
      )
    val preMigrationPayments =
      listOf(
        PreMigrationPayment(201L, BigDecimal("25.00"), "TXN-001"),
        PreMigrationPayment(202L, BigDecimal("37.98"), "TXN-002"),
        PreMigrationPayment(203L, BigDecimal("12.50"), "TXN-003"),
        PreMigrationPayment(204L, BigDecimal("61.75"), "TXN-004"),
      )
    // Mock database state before migration

    // Expected database state after migration

    test("convert decimal yuan to cents correctly") {
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

      convertedProducts.size shouldBe expectedProducts.size
      expectedProducts.zip(convertedProducts).forEach { (expected, actual) ->
        actual.id shouldBe expected.id
        actual.name shouldBe expected.name
        withClue("Price conversion failed for ${expected.name}") { actual.price shouldBe expected.price }
        actual.status shouldBe expected.status
      }
    }
    test("convert decimal order amounts to cents correctly") {
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

      convertedOrders.size shouldBe expectedOrders.size
      expectedOrders.zip(convertedOrders).forEach { (expected, actual) ->
        actual.id shouldBe expected.id
        actual.orderNo shouldBe expected.orderNo
        withClue("Amount conversion failed for ${expected.orderNo}") { actual.totalAmount shouldBe expected.totalAmount }
        actual.status shouldBe expected.status
      }
    }
    test("convert decimal payment amounts to cents correctly") {
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

      convertedPayments.size shouldBe expectedPayments.size
      expectedPayments.zip(convertedPayments).forEach { (expected, actual) ->
        actual.id shouldBe expected.id
        withClue("Payment amount conversion failed for ${expected.transactionId}") { actual.amount shouldBe expected.amount }
        actual.transactionId shouldBe expected.transactionId
      }
    }
    test("handle null values correctly during migration") {
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

      withClue("Null price should remain null after conversion") { convertedProducts[0].price.shouldBeNull() }
      withClue("Non-null price should be converted") { convertedProducts[1].price shouldBe 1000L }
      withClue("Null price should remain null after conversion") { convertedProducts[2].price.shouldBeNull() }
    }
    test("validate monetary precision after migration") {
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

        withClue("Precision loss detected during round-trip conversion of $yuanAmount") { convertedBack shouldBe yuanAmount }

        // Verify the exact cents value
        val expectedCents = (yuanAmount * BigDecimal(100)).toLong()
        withClue("Incorrect cents calculation for $yuanAmount") { cents shouldBe expectedCents }
      }
    }
    test("handle boundary values correctly during migration") {
      val boundaryCases =
        listOf(
          BigDecimal("0.00"), // Zero
          BigDecimal("0.01"), // Minimum positive
          BigDecimal("0.99"), // Just under 1 yuan
          BigDecimal("99999.99"), // Large amount
        )

      boundaryCases.forEach { yuanAmount ->
        withClue("Should handle boundary value: $yuanAmount") {
          shouldNotThrowAny {
            val cents = MoneyUtils.toCents(yuanAmount)
            val convertedBack = MoneyUtils.fromCents(cents)
            convertedBack shouldBe yuanAmount
          }
        }
      }
    }
    test("reject invalid monetary values during migration") {
      val invalidCases =
        listOf(
          BigDecimal("-1.00"), // Negative amount
          BigDecimal("1.001"), // Too many decimal places
          BigDecimal("1.2345"), // Too many decimal places
        )

      invalidCases.forEach { invalidAmount ->
        withClue("Should reject invalid amount: $invalidAmount") {
          shouldThrow<IllegalArgumentException> {
            MoneyUtils.toCents(invalidAmount)
          }
        }
      }
    }
    test("verify database migration integrity") {
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
        withClue("Product price should be non-negative: ${product.name}") { (product.price == null || product.price >= 0) shouldBe true }
      }

      migratedOrders.forEach { order ->
        withClue("Order amount should be non-negative: ${order.orderNo}") {
          (order.totalAmount == null || order.totalAmount >= 0) shouldBe
            true
        }
      }

      migratedPayments.forEach { payment ->
        withClue(
          "Payment amount should be positive: ${payment.transactionId}",
        ) { (payment.amount == null || payment.amount > 0) shouldBe true }
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

      withClue("Total product value should be preserved") { totalProductValue shouldBe expectedTotalProductValue }

      // Step 4: Verify business logic still works
      val expensiveProducts = migratedProducts.filter { it.price != null && it.price > 2000L }
      withClue("Should find correct number of expensive products") { expensiveProducts.size shouldBe 3 }

      val largeOrders = migratedOrders.filter { it.totalAmount != null && it.totalAmount > 3000L }
      withClue("Should find correct number of large orders") { largeOrders.size shouldBe 2 }
    }
    test("verify rollback capability") {
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
      rolledBackProducts.size shouldBe preMigrationProducts.size
      preMigrationProducts.zip(rolledBackProducts).forEach { (original, rolledBack) ->
        rolledBack.id shouldBe original.id
        rolledBack.name shouldBe original.name
        withClue("Rollback failed for ${original.name}: expected ${original.price}, got ${rolledBack.price}") {
          rolledBack.price shouldBe
            original.price
        }
        rolledBack.status shouldBe original.status
      }
    }
    test("test migration performance with large datasets") {
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
      convertedDataset.size shouldBe largeDataset.size

      // Performance check: should complete 10,000 conversions in reasonable time
      withClue("Migration should complete quickly. Took ${conversionTime}ms for 10,000 records") { (conversionTime < 5000) shouldBe true }

      // Spot check some conversions
      val sampleProducts =
        listOf(1L, 5000L, 9999L).map { id ->
          convertedDataset.find { it.id == id }
        }

      sampleProducts.forEach { product ->
        product.shouldNotBeNull()
        withClue("Should preserve .99 pattern") { 99L shouldBe product!!.price!! % 100 }
      }
    }
  })
