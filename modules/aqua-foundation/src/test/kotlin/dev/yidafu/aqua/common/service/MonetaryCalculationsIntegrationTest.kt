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

package dev.yidafu.aqua.common.service

import dev.yidafu.aqua.common.utils.MoneyUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("Monetary Calculations Integration Tests")
class MonetaryCalculationsIntegrationTest {
  // Mock service layer classes for testing monetary calculations
  class OrderCalculationService {
    fun calculateOrderTotal(
      unitPriceCents: Long,
      quantity: Int,
    ): Long = unitPriceCents * quantity

    fun applyDiscount(
      originalAmountCents: Long,
      discountPercentage: BigDecimal,
    ): Long {
      val discountAmount = MoneyUtils.calculatePercentage(originalAmountCents, discountPercentage)
      return MoneyUtils.subtractCents(originalAmountCents, discountAmount)
    }

    fun calculateTax(
      baseAmountCents: Long,
      taxRatePercentage: BigDecimal,
    ): Long = MoneyUtils.calculatePercentage(baseAmountCents, taxRatePercentage)

    fun calculateFinalAmount(
      baseAmountCents: Long,
      discountPercentage: BigDecimal,
      taxRatePercentage: BigDecimal,
    ): Long {
      val discountedAmount = applyDiscount(baseAmountCents, discountPercentage)
      val taxAmount = calculateTax(discountedAmount, taxRatePercentage)
      return MoneyUtils.addCents(discountedAmount, taxAmount)
    }

    fun splitAmount(
      amountCents: Long,
      parts: Int,
    ): List<Long> {
      require(parts > 0) { "Number of parts must be positive" }
      val basePart = amountCents / parts
      val remainder = amountCents % parts

      return (0 until parts).map { index ->
        if (index < remainder) basePart + 1 else basePart
      }
    }
  }

  class PricingService {
    fun calculateBulkPrice(
      unitPriceCents: Long,
      quantity: Int,
      bulkDiscountThreshold: Int,
      bulkDiscountPercentage: BigDecimal,
    ): Long {
      val totalPrice = unitPriceCents * quantity
      return if (quantity >= bulkDiscountThreshold) {
        MoneyUtils.calculatePercentage(totalPrice, bulkDiscountPercentage)
      } else {
        totalPrice
      }
    }

    fun calculateLoyaltyPoints(
      amountCents: Long,
      pointsRate: BigDecimal,
    ): Long = MoneyUtils.calculatePercentage(amountCents, pointsRate)

    fun calculateCommission(
      orderAmountCents: Long,
      commissionRate: BigDecimal,
    ): Long = MoneyUtils.calculatePercentage(orderAmountCents, commissionRate)
  }

  class PaymentProcessingService {
    fun calculatePaymentFee(
      amountCents: Long,
      feeRate: BigDecimal,
      minFeeCents: Long = 0L,
    ): Long {
      val calculatedFee = MoneyUtils.calculatePercentage(amountCents, feeRate)
      return maxOf(calculatedFee, minFeeCents)
    }

    fun calculateRefundAmount(
      originalAmountCents: Long,
      refundPercentage: BigDecimal,
    ): Long = MoneyUtils.calculatePercentage(originalAmountCents, refundPercentage)

    fun validatePaymentAmount(amountCents: Long): Boolean = amountCents > 0 && amountCents <= MoneyUtils.toCents(BigDecimal("999999.99"))
  }

  private lateinit var orderCalculationService: OrderCalculationService
  private lateinit var pricingService: PricingService
  private lateinit var paymentProcessingService: PaymentProcessingService

  @BeforeEach
  fun setUp() {
    orderCalculationService = OrderCalculationService()
    pricingService = PricingService()
    paymentProcessingService = PaymentProcessingService()
  }

  // ========== Order Calculation Tests ==========

  @Test
  @DisplayName("Should calculate order totals correctly with cents-based amounts")
  fun `calculate order totals correctly with cents`() {
    val testCases =
      listOf(
        Triple(2500L, 1, 2500L), // ¥25.00 × 1 = ¥25.00
        Triple(2500L, 3, 7500L), // ¥25.00 × 3 = ¥75.00
        Triple(999L, 5, 4995L), // ¥9.99 × 5 = ¥49.95
        Triple(100L, 100, 10000L), // ¥1.00 × 100 = ¥100.00
        Triple(1L, 1, 1L), // ¥0.01 × 1 = ¥0.01
      )

    testCases.forEach { (unitPriceCents, quantity, expectedTotal) ->
      val actualTotal = orderCalculationService.calculateOrderTotal(unitPriceCents, quantity)
      assertEquals(
        expectedTotal,
        actualTotal,
        "Order total calculation failed for unit price: $unitPriceCents, quantity: $quantity",
      )
    }
  }

  @Test
  @DisplayName("Should apply discounts correctly")
  fun `apply discounts correctly`() {
    val testCases =
      listOf(
        Triple(10000L, BigDecimal("10.0"), 9000L), // ¥100.00 with 10% discount = ¥90.00
        Triple(25000L, BigDecimal("25.0"), 18750L), // ¥250.00 with 25% discount = ¥187.50
        Triple(9999L, BigDecimal("50.0"), 4999L), // ¥99.99 with 50% discount = ¥49.99
        Triple(5000L, BigDecimal("0.0"), 5000L), // ¥50.00 with 0% discount = ¥50.00
        Triple(12345L, BigDecimal("33.33"), 8230L), // ¥123.45 with 33.33% discount = ¥82.30
      )

    testCases.forEach { (originalAmount, discountPercentage, expectedDiscounted) ->
      val actualDiscounted = orderCalculationService.applyDiscount(originalAmount, discountPercentage)
      assertEquals(
        expectedDiscounted,
        actualDiscounted,
        "Discount calculation failed for amount: $originalAmount, discount: $discountPercentage",
      )
    }
  }

  @Test
  @DisplayName("Should calculate tax correctly")
  fun `calculate tax correctly`() {
    val testCases =
      listOf(
        Triple(10000L, BigDecimal("10.0"), 1000L), // ¥100.00 with 10% tax = ¥10.00
        Triple(25000L, BigDecimal("8.0"), 2000L), // ¥250.00 with 8% tax = ¥20.00
        Triple(9999L, BigDecimal("15.0"), 1500L), // ¥99.99 with 15% tax = ¥15.00
        Triple(5000L, BigDecimal("0.0"), 0L), // ¥50.00 with 0% tax = ¥0.00
        Triple(12345L, BigDecimal("13.5"), 1667L), // ¥123.45 with 13.5% tax = ¥16.67
      )

    testCases.forEach { (baseAmount, taxRate, expectedTax) ->
      val actualTax = orderCalculationService.calculateTax(baseAmount, taxRate)
      assertEquals(
        expectedTax,
        actualTax,
        "Tax calculation failed for amount: $baseAmount, rate: $taxRate",
      )
    }
  }

  @Test
  @DisplayName("Should calculate final amount with discount and tax correctly")
  fun `calculate final amount with discount and tax correctly`() {
    val baseAmount = 10000L // ¥100.00
    val discountPercentage = BigDecimal("10.0") // 10% discount
    val taxRatePercentage = BigDecimal("8.0") // 8% tax

    val finalAmount = orderCalculationService.calculateFinalAmount(baseAmount, discountPercentage, taxRatePercentage)

    // Expected calculation:
    // Original: ¥100.00
    // After 10% discount: ¥90.00
    // Tax on discounted amount (8% of ¥90.00): ¥7.20
    // Final amount: ¥90.00 + ¥7.20 = ¥97.20
    val expectedFinalAmount = 9720L

    assertEquals(
      expectedFinalAmount,
      finalAmount,
      "Final amount calculation failed",
    )

    // Verify by manual calculation
    val discountedAmount =
      MoneyUtils.subtractCents(baseAmount, MoneyUtils.calculatePercentage(baseAmount, discountPercentage))
    val taxAmount = MoneyUtils.calculatePercentage(discountedAmount, taxRatePercentage)
    val manualCalculation = MoneyUtils.addCents(discountedAmount, taxAmount)

    assertEquals(manualCalculation, finalAmount)
  }

  @Test
  @DisplayName("Should split amounts correctly")
  fun `split amounts correctly`() {
    val testCases =
      listOf(
        Triple(10000L, 4, listOf(2500L, 2500L, 2500L, 2500L)), // ¥100.00 ÷ 4 = ¥25.00 each
        Triple(10001L, 4, listOf(2501L, 2500L, 2500L, 2500L)), // ¥100.01 ÷ 4 = 3×¥25.00 + 1×¥25.01
        Triple(333L, 3, listOf(111L, 111L, 111L)), // ¥3.33 ÷ 3 = ¥1.11 each
        Triple(1L, 1, listOf(1L)), // ¥0.01 ÷ 1 = ¥0.01
        Triple(999L, 7, listOf(143L, 143L, 143L, 143L, 143L, 142L, 142L)), // ¥9.99 ÷ 7
      )

    testCases.forEach { (amount, parts, expectedSplit) ->
      val actualSplit = orderCalculationService.splitAmount(amount, parts)
      assertEquals(expectedSplit.size, actualSplit.size)
      assertEquals(expectedSplit.sum(), actualSplit.sum())
      assertEquals(amount, actualSplit.sum(), "Split amounts must sum to original amount")
    }
  }

  // ========== Pricing Service Tests ==========

  @Test
  @DisplayName("Should calculate bulk pricing correctly")
  fun `calculate bulk pricing correctly`() {
    val unitPrice = 2500L // ¥25.00
    val bulkDiscountThreshold = 5
    val bulkDiscountPercentage = BigDecimal("20.0") // 20% discount

    // Below threshold - no discount
    val normalPrice = pricingService.calculateBulkPrice(unitPrice, 3, bulkDiscountThreshold, bulkDiscountPercentage)
    assertEquals(7500L, normalPrice) // ¥25.00 × 3 = ¥75.00

    // At threshold - discount applies
    val bulkPrice = pricingService.calculateBulkPrice(unitPrice, 5, bulkDiscountThreshold, bulkDiscountPercentage)
    assertEquals(2500L, bulkPrice) // (¥25.00 × 5) × 20% = ¥25.00

    // Above threshold - discount applies
    val largeBulkPrice = pricingService.calculateBulkPrice(unitPrice, 10, bulkDiscountThreshold, bulkDiscountPercentage)
    assertEquals(5000L, largeBulkPrice) // (¥25.00 × 10) × 20% = ¥50.00
  }

  @Test
  @DisplayName("Should calculate loyalty points correctly")
  fun `calculate loyalty points correctly`() {
    val testCases =
      listOf(
        Triple(10000L, BigDecimal("1.0"), 100L), // ¥100.00 × 1% = 100 points
        Triple(25000L, BigDecimal("2.0"), 500L), // ¥250.00 × 2% = 500 points
        Triple(9999L, BigDecimal("0.5"), 50L), // ¥99.99 × 0.5% = 50 points
        Triple(5000L, BigDecimal("10.0"), 500L), // ¥50.00 × 10% = 500 points
      )

    testCases.forEach { (amountCents, pointsRate, expectedPoints) ->
      val actualPoints = pricingService.calculateLoyaltyPoints(amountCents, pointsRate)
      assertEquals(
        expectedPoints,
        actualPoints,
        "Loyalty points calculation failed for amount: $amountCents, rate: $pointsRate",
      )
    }
  }

  @Test
  @DisplayName("Should calculate commission correctly")
  fun `calculate commission correctly`() {
    val testCases =
      listOf(
        Triple(10000L, BigDecimal("5.0"), 500L), // ¥100.00 × 5% = ¥5.00
        Triple(50000L, BigDecimal("3.5"), 1750L), // ¥500.00 × 3.5% = ¥17.50
        Triple(25000L, BigDecimal("10.0"), 2500L), // ¥250.00 × 10% = ¥25.00
        Triple(9999L, BigDecimal("15.0"), 1500L), // ¥99.99 × 15% = ¥15.00
      )

    testCases.forEach { (orderAmount, commissionRate, expectedCommission) ->
      val actualCommission = pricingService.calculateCommission(orderAmount, commissionRate)
      assertEquals(
        expectedCommission,
        actualCommission,
        "Commission calculation failed for amount: $orderAmount, rate: $commissionRate",
      )
    }
  }

  // ========== Payment Processing Tests ==========

  @Test
  @DisplayName("Should calculate payment fees correctly")
  fun `calculate payment fees correctly`() {
    val testCases =
      listOf(
        Triple(10000L, BigDecimal("2.9"), 290L), // ¥100.00 × 2.9% = ¥2.90
        Triple(5000L, BigDecimal("3.0"), 150L), // ¥50.00 × 3.0% = ¥1.50
        Triple(1000L, BigDecimal("1.5"), 15L), // ¥10.00 × 1.5% = ¥0.15
        Triple(100L, BigDecimal("5.0"), 5L), // ¥1.00 × 5.0% = ¥0.05
      )

    testCases.forEach { (amount, feeRate, expectedFee) ->
      val actualFee = paymentProcessingService.calculatePaymentFee(amount, feeRate)
      assertEquals(
        expectedFee,
        actualFee,
        "Payment fee calculation failed for amount: $amount, rate: $feeRate",
      )
    }
  }

  @Test
  @DisplayName("Should respect minimum payment fees")
  fun `respect minimum payment fees`() {
    val smallAmount = 100L // ¥1.00
    val feeRate = BigDecimal("1.0") // 1% fee = ¥0.01
    val minFeeCents = 50L // ¥0.50 minimum fee

    val calculatedFee = paymentProcessingService.calculatePaymentFee(smallAmount, feeRate, minFeeCents)
    assertEquals(minFeeCents, calculatedFee, "Should use minimum fee when calculated fee is lower")
  }

  @Test
  @DisplayName("Should calculate refund amounts correctly")
  fun `calculate refund amounts correctly`() {
    val testCases =
      listOf(
        Triple(10000L, BigDecimal("100.0"), 10000L), // Full refund
        Triple(10000L, BigDecimal("50.0"), 5000L), // 50% refund
        Triple(25000L, BigDecimal("25.0"), 6250L), // 25% refund
        Triple(9999L, BigDecimal("10.0"), 1000L), // 10% refund
      )

    testCases.forEach { (originalAmount, refundPercentage, expectedRefund) ->
      val actualRefund = paymentProcessingService.calculateRefundAmount(originalAmount, refundPercentage)
      assertEquals(
        expectedRefund,
        actualRefund,
        "Refund calculation failed for amount: $originalAmount, percentage: $refundPercentage",
      )
    }
  }

  @Test
  @DisplayName("Should validate payment amounts correctly")
  fun `validate payment amounts correctly`() {
    // Valid amounts
    assertTrue(paymentProcessingService.validatePaymentAmount(1L)) // ¥0.01
    assertTrue(paymentProcessingService.validatePaymentAmount(100L)) // ¥1.00
    assertTrue(paymentProcessingService.validatePaymentAmount(99999999L)) // Large valid amount

    // Invalid amounts
    assertFalse(paymentProcessingService.validatePaymentAmount(0L)) // Zero amount
    assertFalse(paymentProcessingService.validatePaymentAmount(-1L)) // Negative amount
    assertFalse(paymentProcessingService.validatePaymentAmount(100000000L)) // Too large amount
  }

  // ========== Integration Tests ==========

  @Test
  @DisplayName("Should handle complete order processing workflow")
  fun `handle complete order processing workflow`() {
    // Simulate a complete order processing scenario

    // Step 1: Order creation
    val unitPrice = 2599L // ¥25.99
    val quantity = 3
    val orderTotal = orderCalculationService.calculateOrderTotal(unitPrice, quantity) // ¥77.97
    assertEquals(7797L, orderTotal)

    // Step 2: Apply customer discount
    val discountPercentage = BigDecimal("10.0") // 10% discount
    val discountedAmount = orderCalculationService.applyDiscount(orderTotal, discountPercentage) // ¥70.17
    assertEquals(7017L, discountedAmount)

    // Step 3: Add tax
    val taxRate = BigDecimal("8.0") // 8% tax
    val taxAmount = orderCalculationService.calculateTax(discountedAmount, taxRate) // ¥5.61
    assertEquals(561L, taxAmount)

    // Step 4: Calculate final amount
    val finalAmount = MoneyUtils.addCents(discountedAmount, taxAmount) // ¥75.78
    assertEquals(7578L, finalAmount)

    // Step 5: Calculate payment processing fee
    val paymentFeeRate = BigDecimal("2.9") // 2.9% payment fee
    val paymentFee = paymentProcessingService.calculatePaymentFee(finalAmount, paymentFeeRate) // ¥2.20
    assertEquals(220L, paymentFee)

    // Step 6: Calculate total amount to charge
    val totalToCharge = MoneyUtils.addCents(finalAmount, paymentFee) // ¥77.98
    assertEquals(7798L, totalToCharge)

    // Step 7: Validate payment amount
    assertTrue(paymentProcessingService.validatePaymentAmount(totalToCharge))

    // Step 8: Calculate commission for delivery worker
    val commissionRate = BigDecimal("15.0") // 15% commission
    val commission = pricingService.calculateCommission(finalAmount, commissionRate) // ¥11.37
    assertEquals(1137L, commission)

    // Step 9: Calculate loyalty points
    val pointsRate = BigDecimal("1.0") // 1% points
    val loyaltyPoints = pricingService.calculateLoyaltyPoints(finalAmount, pointsRate) // 76 points
    assertEquals(76L, loyaltyPoints)

    // Verify all calculations are consistent with MoneyUtils
    assertEquals(BigDecimal("77.97"), MoneyUtils.fromCents(orderTotal))
    assertEquals(BigDecimal("70.17"), MoneyUtils.fromCents(discountedAmount))
    assertEquals(BigDecimal("5.61"), MoneyUtils.fromCents(taxAmount))
    assertEquals(BigDecimal("75.78"), MoneyUtils.fromCents(finalAmount))
    assertEquals(BigDecimal("2.20"), MoneyUtils.fromCents(paymentFee))
    assertEquals(BigDecimal("77.98"), MoneyUtils.fromCents(totalToCharge))
    assertEquals(BigDecimal("11.37"), MoneyUtils.fromCents(commission))
  }

  @Test
  @DisplayName("Should handle edge cases in monetary calculations")
  fun `handle edge cases in monetary calculations`() {
    // Test very small amounts
    val smallAmount = 1L // ¥0.01
    assertEquals(
      0L,
      orderCalculationService.calculateTax(smallAmount, BigDecimal("1.0")),
    ) // 1% of ¥0.01 rounds to ¥0.00
    assertEquals(
      1L,
      orderCalculationService.applyDiscount(smallAmount, BigDecimal("50.0")),
    ) // 50% of ¥0.01 rounds to ¥0.01

    // Test very large amounts
    val largeAmount = 99999999L // ¥999,999.99
    val largeDiscount = orderCalculationService.applyDiscount(largeAmount, BigDecimal("1.0")) // 1% discount
    assertEquals(98999999L, largeDiscount) // ¥989,999.99

    // Test rounding edge cases
    val roundingAmount = 333L // ¥3.33
    val third = orderCalculationService.splitAmount(roundingAmount, 3)
    assertEquals(listOf(111L, 111L, 111L), third) // Each gets ¥1.11

    val unevenAmount = 100L // ¥1.00
    val threeParts = orderCalculationService.splitAmount(unevenAmount, 3)
    assertEquals(listOf(34L, 33L, 33L), threeParts) // ¥0.34, ¥0.33, ¥0.33
    assertEquals(100L, threeParts.sum()) // Total is still ¥1.00
  }
}
