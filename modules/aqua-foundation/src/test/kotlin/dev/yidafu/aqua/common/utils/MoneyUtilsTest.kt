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

package dev.yidafu.aqua.common.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class MoneyUtilsTest :
  FunSpec({
    // ========== Conversion Function Tests ==========

    // ========== Formatting Function Tests ==========

    // ========== Validation Function Tests ==========

    // ========== Arithmetic Operation Tests ==========

    // ========== Utility Function Tests ==========

    // ========== Performance and Edge Case Tests ==========

    test("toCents should convert valid yuan amounts correctly") {
      // Test basic conversions
      MoneyUtils.toCents(BigDecimal("1.00")) shouldBe 100L
      MoneyUtils.toCents(BigDecimal("0.00")) shouldBe 0L
      MoneyUtils.toCents(BigDecimal("1.50")) shouldBe 150L
      MoneyUtils.toCents(BigDecimal("9.99")) shouldBe 999L
      MoneyUtils.toCents(BigDecimal("1234567.89")) shouldBe 123456789L
    }
    test("toCents should handle edge cases correctly") {
      // Test maximum safe values
      MoneyUtils.toCents(BigDecimal("92233720368547758.07")) shouldBe Long.MAX_VALUE

      // More than 2 decimal places is rejected by strict validation
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.toCents(BigDecimal("1.245"))
        }
      exception.message!!.contains("more than 2 decimal places") shouldBe true
    }
    test("toCents should throw exception for negative amounts") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.toCents(BigDecimal("-1.00"))
        }
      exception.message!!.contains("Yuan amount cannot be negative") shouldBe true
    }
    test("toCents should throw exception for amounts with too many decimal places") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.toCents(BigDecimal("1.001"))
        }
      exception.message!!.contains("cannot have more than 2 decimal places") shouldBe true
    }
    test("toCents should throw exception for amounts that would cause precision loss") {
      val exception =
        shouldThrow<ArithmeticException> {
          MoneyUtils.toCents(BigDecimal("92233720368547758.08")) // Would overflow
        }
      exception.message!!.contains("Conversion from yuan to cents would lose precision") shouldBe true
    }
    test("fromCents should convert valid cents correctly") {
      MoneyUtils.fromCents(0L) shouldBe BigDecimal("0.00")
      MoneyUtils.fromCents(100L) shouldBe BigDecimal("1.00")
      MoneyUtils.fromCents(150L) shouldBe BigDecimal("1.50")
      MoneyUtils.fromCents(999L) shouldBe BigDecimal("9.99")
      MoneyUtils.fromCents(123456789L) shouldBe BigDecimal("1234567.89")
    }
    test("fromCents should handle large values correctly") {
      val largeCents = Long.MAX_VALUE
      val result = MoneyUtils.fromCents(largeCents)
      result.shouldNotBeNull()
      (result.scale() <= 2) shouldBe true
    }
    test("fromCents should throw exception for negative amounts") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.fromCents(-1L)
        }
      exception.message!!.contains("Cents amount cannot be negative") shouldBe true
    }
    test("conversion should be bidirectional and accurate") {
      val testAmounts =
        listOf(
          "0.00",
          "0.01",
          "0.10",
          "1.00",
          "1.23",
          "9.99",
          "100.00",
          "1234.56",
          "999999.99",
        )

      for (amountStr in testAmounts) {
        val yuan = BigDecimal(amountStr)
        val cents = MoneyUtils.toCents(yuan)
        val convertedBack = MoneyUtils.fromCents(cents)
        withClue("Round-trip conversion failed for $amountStr") { convertedBack shouldBe yuan }
      }
    }
    test("formatCents should format amounts correctly") {
      MoneyUtils.formatCents(0L) shouldBe "¥0.00"
      MoneyUtils.formatCents(100L) shouldBe "¥1.00"
      MoneyUtils.formatCents(150L) shouldBe "¥1.50"
      MoneyUtils.formatCents(12345L) shouldBe "¥123.45"
    }
    test("formatCents should throw exception for negative amounts") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.formatCents(-1L)
        }
      exception.message!!.contains("Cents amount cannot be negative") shouldBe true
    }
    test("formatYuan should format amounts correctly") {
      MoneyUtils.formatYuan(BigDecimal("0.00")) shouldBe "¥0.00"
      MoneyUtils.formatYuan(BigDecimal("1.00")) shouldBe "¥1.00"
      MoneyUtils.formatYuan(BigDecimal("1.50")) shouldBe "¥1.50"
      MoneyUtils.formatYuan(BigDecimal("123.45")) shouldBe "¥123.45"
    }
    test("formatYuan should throw exception for negative amounts") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.formatYuan(BigDecimal("-1.00"))
        }
      exception.message!!.contains("Yuan amount cannot be negative") shouldBe true
    }
    test("canConvertToCents should validate correctly") {
      // Valid cases
      MoneyUtils.canConvertToCents(BigDecimal("0.00")) shouldBe true
      MoneyUtils.canConvertToCents(BigDecimal("1.00")) shouldBe true
      MoneyUtils.canConvertToCents(BigDecimal("1.23")) shouldBe true
      MoneyUtils.canConvertToCents(BigDecimal("999999.99")) shouldBe true

      // Invalid cases
      MoneyUtils.canConvertToCents(BigDecimal("-1.00")) shouldBe false
      MoneyUtils.canConvertToCents(BigDecimal("1.001")) shouldBe false // Too many decimal places
    }
    test("validateCentsConversion should validate correctly") {
      // Valid cases
      MoneyUtils.validateCentsConversion(100L, BigDecimal("1.00")) shouldBe true
      MoneyUtils.validateCentsConversion(150L, BigDecimal("1.50")) shouldBe true
      MoneyUtils.validateCentsConversion(0L, BigDecimal("0.00")) shouldBe true

      // Invalid cases
      MoneyUtils.validateCentsConversion(-1L, BigDecimal("1.00")) shouldBe false
      MoneyUtils.validateCentsConversion(100L, BigDecimal("-1.00")) shouldBe false
      MoneyUtils.validateCentsConversion(100L, BigDecimal("2.00")) shouldBe false // Mismatch
    }
    test("addCents should add amounts correctly") {
      MoneyUtils.addCents(100L, 100L) shouldBe 200L
      MoneyUtils.addCents(0L, 0L) shouldBe 0L
      MoneyUtils.addCents(100L, 50L) shouldBe 150L
      MoneyUtils.addCents(Long.MAX_VALUE - 100L, 100L) shouldBe Long.MAX_VALUE
    }
    test("addCents should throw exception for overflow") {
      shouldThrow<ArithmeticException> {
        MoneyUtils.addCents(Long.MAX_VALUE, 1L)
      }
      shouldThrow<ArithmeticException> {
        MoneyUtils.addCents(Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE / 2 + 1)
      }
    }
    test("subtractCents should subtract amounts correctly") {
      MoneyUtils.subtractCents(200L, 100L) shouldBe 100L
      MoneyUtils.subtractCents(100L, 100L) shouldBe 0L
      MoneyUtils.subtractCents(100L, 50L) shouldBe 50L
      MoneyUtils.subtractCents(Long.MAX_VALUE, Long.MAX_VALUE - 1L) shouldBe 1L
    }
    test("subtractCents should throw exception for negative result") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.subtractCents(100L, 200L)
        }
      exception.message!!.contains("Result would be negative") shouldBe true
    }
    test("multiplyCents should multiply amounts correctly") {
      MoneyUtils.multiplyCents(100L, BigDecimal("2.0")) shouldBe 200L
      MoneyUtils.multiplyCents(100L, BigDecimal("1.5")) shouldBe 150L
      MoneyUtils.multiplyCents(100L, BigDecimal("0.5")) shouldBe 50L
      MoneyUtils.multiplyCents(100L, BigDecimal("0.333")) shouldBe 33L // Should round
      MoneyUtils.multiplyCents(100L, BigDecimal("0.0")) shouldBe 0L
    }
    test("multiplyCents should throw exception for negative multiplier") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.multiplyCents(100L, BigDecimal("-1.0"))
        }
      exception.message!!.contains("Multiplier cannot be negative") shouldBe true
    }
    test("calculatePercentage should calculate correctly") {
      MoneyUtils.calculatePercentage(1000L, BigDecimal("1.0")) shouldBe 10L // 1%
      MoneyUtils.calculatePercentage(1000L, BigDecimal("5.0")) shouldBe 50L // 5%
      MoneyUtils.calculatePercentage(1000L, BigDecimal("10.0")) shouldBe 100L // 10%
      MoneyUtils.calculatePercentage(1000L, BigDecimal("0.0")) shouldBe 0L // 0%
      MoneyUtils.calculatePercentage(1000L, BigDecimal("100.0")) shouldBe 1000L // 100%
    }
    test("calculatePercentage should throw exception for negative percentage") {
      val exception =
        shouldThrow<IllegalArgumentException> {
          MoneyUtils.calculatePercentage(1000L, BigDecimal("-1.0"))
        }
      exception.message!!.contains("Percentage cannot be negative") shouldBe true
    }
    test("roundToMonetaryScale should round correctly") {
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.00")) shouldBe BigDecimal("1.00")
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.235")) shouldBe BigDecimal("1.24") // Banker's rounding
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.255")) shouldBe BigDecimal("1.26") // Banker's rounding
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.234")) shouldBe BigDecimal("1.23")
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.236")) shouldBe BigDecimal("1.24")

      // Test that it always returns exactly 2 decimal places
      MoneyUtils.roundToMonetaryScale(BigDecimal("1")).scale() shouldBe 2
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.2")).scale() shouldBe 2
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.23")).scale() shouldBe 2
      MoneyUtils.roundToMonetaryScale(BigDecimal("1.234")).scale() shouldBe 2
    }
    test("conversion should handle all valid 2-decimal amounts correctly") {
      for (amountStr in listOf("0.01", "0.10", "0.99", "1.00", "1.01", "10.00", "99.99", "100.00")) {
        val yuan = BigDecimal(amountStr)
        val cents = MoneyUtils.toCents(yuan)
        val convertedBack = MoneyUtils.fromCents(cents)

        withClue("Failed for amount: $amountStr") { convertedBack shouldBe yuan }

        // Also verify the cents value is correct
        val expectedCents = (BigDecimal(amountStr) * BigDecimal(100)).toLong()
        withClue("Incorrect cents for amount: $amountStr") { cents shouldBe expectedCents }
      }
    }
    test("large number operations should not overflow unexpectedly") {
      val largeAmount = Long.MAX_VALUE / 2

      // These should work without overflow
      MoneyUtils.fromCents(largeAmount).shouldNotBeNull()
      MoneyUtils.formatCents(largeAmount).shouldNotBeNull()

      // Addition should work within limits
      val result = MoneyUtils.addCents(largeAmount, 1L)
      result shouldBe largeAmount + 1
    }
    test("arithmetic operations should maintain precision") {
      // Test precision is maintained in complex operations
      val base = 12345L // ¥123.45

      val doubled = MoneyUtils.multiplyCents(base, BigDecimal("2.0"))
      doubled shouldBe 24690L

      val halved = MoneyUtils.multiplyCents(base, BigDecimal("0.5"))
      halved shouldBe 6172L // HALF_EVEN rounds 6172.5 -> 6172

      val percentage = MoneyUtils.calculatePercentage(base, BigDecimal("10.0"))
      percentage shouldBe 1234L // 10% of 12345, HALF_EVEN rounds 1234.5 -> 1234

      // Verify by converting back to yuan
      MoneyUtils.fromCents(doubled) shouldBe BigDecimal("246.90")
      MoneyUtils.fromCents(halved) shouldBe BigDecimal("61.72")
      MoneyUtils.fromCents(percentage) shouldBe BigDecimal("12.34")
    }
    test("formatting should be consistent with Chinese locale") {
      val testCents = listOf(0L, 1L, 100L, 1234L, 999999L)

      for (cents in testCents) {
        val formattedFromCents = MoneyUtils.formatCents(cents)
        val yuan = MoneyUtils.fromCents(cents)
        val formattedFromYuan = MoneyUtils.formatYuan(yuan)

        withClue("Formatting inconsistency for cents: $cents") { formattedFromYuan shouldBe formattedFromCents }

        withClue("Should start with ¥ symbol: $formattedFromCents") { formattedFromCents.startsWith("¥") shouldBe true }
      }
    }
  })
