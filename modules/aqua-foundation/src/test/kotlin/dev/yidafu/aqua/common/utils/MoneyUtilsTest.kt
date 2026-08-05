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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class MoneyUtilsTest {
  // ========== Conversion Function Tests ==========

  @Test
  fun `toCents should convert valid yuan amounts correctly`() {
    // Test basic conversions
    assertEquals(100L, MoneyUtils.toCents(BigDecimal("1.00")))
    assertEquals(0L, MoneyUtils.toCents(BigDecimal("0.00")))
    assertEquals(150L, MoneyUtils.toCents(BigDecimal("1.50")))
    assertEquals(999L, MoneyUtils.toCents(BigDecimal("9.99")))
    assertEquals(123456789L, MoneyUtils.toCents(BigDecimal("1234567.89")))
  }

  @Test
  fun `toCents should handle edge cases correctly`() {
    // Test maximum safe values
    assertEquals(Long.MAX_VALUE, MoneyUtils.toCents(BigDecimal("92233720368547758.07")))

    // More than 2 decimal places is rejected by strict validation
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.toCents(BigDecimal("1.245"))
      }
    assertTrue(exception.message!!.contains("more than 2 decimal places"))
  }

  @Test
  fun `toCents should throw exception for negative amounts`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.toCents(BigDecimal("-1.00"))
      }
    assertTrue(exception.message!!.contains("Yuan amount cannot be negative"))
  }

  @Test
  fun `toCents should throw exception for amounts with too many decimal places`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.toCents(BigDecimal("1.001"))
      }
    assertTrue(exception.message!!.contains("cannot have more than 2 decimal places"))
  }

  @Test
  fun `toCents should throw exception for amounts that would cause precision loss`() {
    val exception =
      assertThrows<ArithmeticException> {
        MoneyUtils.toCents(BigDecimal("92233720368547758.08")) // Would overflow
      }
    assertTrue(exception.message!!.contains("Conversion from yuan to cents would lose precision"))
  }

  @Test
  fun `fromCents should convert valid cents correctly`() {
    assertEquals(BigDecimal("0.00"), MoneyUtils.fromCents(0L))
    assertEquals(BigDecimal("1.00"), MoneyUtils.fromCents(100L))
    assertEquals(BigDecimal("1.50"), MoneyUtils.fromCents(150L))
    assertEquals(BigDecimal("9.99"), MoneyUtils.fromCents(999L))
    assertEquals(BigDecimal("1234567.89"), MoneyUtils.fromCents(123456789L))
  }

  @Test
  fun `fromCents should handle large values correctly`() {
    val largeCents = Long.MAX_VALUE
    val result = MoneyUtils.fromCents(largeCents)
    assertNotNull(result)
    assertTrue(result.scale() <= 2)
  }

  @Test
  fun `fromCents should throw exception for negative amounts`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.fromCents(-1L)
      }
    assertTrue(exception.message!!.contains("Cents amount cannot be negative"))
  }

  @Test
  fun `conversion should be bidirectional and accurate`() {
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
      assertEquals(yuan, convertedBack, "Round-trip conversion failed for $amountStr")
    }
  }

  // ========== Formatting Function Tests ==========

  @Test
  fun `formatCents should format amounts correctly`() {
    assertEquals("¥0.00", MoneyUtils.formatCents(0L))
    assertEquals("¥1.00", MoneyUtils.formatCents(100L))
    assertEquals("¥1.50", MoneyUtils.formatCents(150L))
    assertEquals("¥123.45", MoneyUtils.formatCents(12345L))
  }

  @Test
  fun `formatCents should throw exception for negative amounts`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.formatCents(-1L)
      }
    assertTrue(exception.message!!.contains("Cents amount cannot be negative"))
  }

  @Test
  fun `formatYuan should format amounts correctly`() {
    assertEquals("¥0.00", MoneyUtils.formatYuan(BigDecimal("0.00")))
    assertEquals("¥1.00", MoneyUtils.formatYuan(BigDecimal("1.00")))
    assertEquals("¥1.50", MoneyUtils.formatYuan(BigDecimal("1.50")))
    assertEquals("¥123.45", MoneyUtils.formatYuan(BigDecimal("123.45")))
  }

  @Test
  fun `formatYuan should throw exception for negative amounts`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.formatYuan(BigDecimal("-1.00"))
      }
    assertTrue(exception.message!!.contains("Yuan amount cannot be negative"))
  }

  // ========== Validation Function Tests ==========

  @Test
  fun `canConvertToCents should validate correctly`() {
    // Valid cases
    assertTrue(MoneyUtils.canConvertToCents(BigDecimal("0.00")))
    assertTrue(MoneyUtils.canConvertToCents(BigDecimal("1.00")))
    assertTrue(MoneyUtils.canConvertToCents(BigDecimal("1.23")))
    assertTrue(MoneyUtils.canConvertToCents(BigDecimal("999999.99")))

    // Invalid cases
    assertFalse(MoneyUtils.canConvertToCents(BigDecimal("-1.00")))
    assertFalse(MoneyUtils.canConvertToCents(BigDecimal("1.001"))) // Too many decimal places
  }

  @Test
  fun `validateCentsConversion should validate correctly`() {
    // Valid cases
    assertTrue(MoneyUtils.validateCentsConversion(100L, BigDecimal("1.00")))
    assertTrue(MoneyUtils.validateCentsConversion(150L, BigDecimal("1.50")))
    assertTrue(MoneyUtils.validateCentsConversion(0L, BigDecimal("0.00")))

    // Invalid cases
    assertFalse(MoneyUtils.validateCentsConversion(-1L, BigDecimal("1.00")))
    assertFalse(MoneyUtils.validateCentsConversion(100L, BigDecimal("-1.00")))
    assertFalse(MoneyUtils.validateCentsConversion(100L, BigDecimal("2.00"))) // Mismatch
  }

  // ========== Arithmetic Operation Tests ==========

  @Test
  fun `addCents should add amounts correctly`() {
    assertEquals(200L, MoneyUtils.addCents(100L, 100L))
    assertEquals(0L, MoneyUtils.addCents(0L, 0L))
    assertEquals(150L, MoneyUtils.addCents(100L, 50L))
    assertEquals(Long.MAX_VALUE, MoneyUtils.addCents(Long.MAX_VALUE - 100L, 100L))
  }

  @Test
  fun `addCents should throw exception for overflow`() {
    assertThrows<ArithmeticException> {
      MoneyUtils.addCents(Long.MAX_VALUE, 1L)
    }
    assertThrows<ArithmeticException> {
      MoneyUtils.addCents(Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE / 2 + 1)
    }
  }

  @Test
  fun `subtractCents should subtract amounts correctly`() {
    assertEquals(100L, MoneyUtils.subtractCents(200L, 100L))
    assertEquals(0L, MoneyUtils.subtractCents(100L, 100L))
    assertEquals(50L, MoneyUtils.subtractCents(100L, 50L))
    assertEquals(1L, MoneyUtils.subtractCents(Long.MAX_VALUE, Long.MAX_VALUE - 1L))
  }

  @Test
  fun `subtractCents should throw exception for negative result`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.subtractCents(100L, 200L)
      }
    assertTrue(exception.message!!.contains("Result would be negative"))
  }

  @Test
  fun `multiplyCents should multiply amounts correctly`() {
    assertEquals(200L, MoneyUtils.multiplyCents(100L, BigDecimal("2.0")))
    assertEquals(150L, MoneyUtils.multiplyCents(100L, BigDecimal("1.5")))
    assertEquals(50L, MoneyUtils.multiplyCents(100L, BigDecimal("0.5")))
    assertEquals(33L, MoneyUtils.multiplyCents(100L, BigDecimal("0.333"))) // Should round
    assertEquals(0L, MoneyUtils.multiplyCents(100L, BigDecimal("0.0")))
  }

  @Test
  fun `multiplyCents should throw exception for negative multiplier`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.multiplyCents(100L, BigDecimal("-1.0"))
      }
    assertTrue(exception.message!!.contains("Multiplier cannot be negative"))
  }

  @Test
  fun `calculatePercentage should calculate correctly`() {
    assertEquals(10L, MoneyUtils.calculatePercentage(1000L, BigDecimal("1.0"))) // 1%
    assertEquals(50L, MoneyUtils.calculatePercentage(1000L, BigDecimal("5.0"))) // 5%
    assertEquals(100L, MoneyUtils.calculatePercentage(1000L, BigDecimal("10.0"))) // 10%
    assertEquals(0L, MoneyUtils.calculatePercentage(1000L, BigDecimal("0.0"))) // 0%
    assertEquals(1000L, MoneyUtils.calculatePercentage(1000L, BigDecimal("100.0"))) // 100%
  }

  @Test
  fun `calculatePercentage should throw exception for negative percentage`() {
    val exception =
      assertThrows<IllegalArgumentException> {
        MoneyUtils.calculatePercentage(1000L, BigDecimal("-1.0"))
      }
    assertTrue(exception.message!!.contains("Percentage cannot be negative"))
  }

  // ========== Utility Function Tests ==========

  @Test
  fun `roundToMonetaryScale should round correctly`() {
    assertEquals(BigDecimal("1.00"), MoneyUtils.roundToMonetaryScale(BigDecimal("1.00")))
    assertEquals(BigDecimal("1.24"), MoneyUtils.roundToMonetaryScale(BigDecimal("1.235"))) // Banker's rounding
    assertEquals(BigDecimal("1.26"), MoneyUtils.roundToMonetaryScale(BigDecimal("1.255"))) // Banker's rounding
    assertEquals(BigDecimal("1.23"), MoneyUtils.roundToMonetaryScale(BigDecimal("1.234")))
    assertEquals(BigDecimal("1.24"), MoneyUtils.roundToMonetaryScale(BigDecimal("1.236")))

    // Test that it always returns exactly 2 decimal places
    assertEquals(2, MoneyUtils.roundToMonetaryScale(BigDecimal("1")).scale())
    assertEquals(2, MoneyUtils.roundToMonetaryScale(BigDecimal("1.2")).scale())
    assertEquals(2, MoneyUtils.roundToMonetaryScale(BigDecimal("1.23")).scale())
    assertEquals(2, MoneyUtils.roundToMonetaryScale(BigDecimal("1.234")).scale())
  }

  // ========== Performance and Edge Case Tests ==========

  @ParameterizedTest
  @ValueSource(strings = ["0.01", "0.10", "0.99", "1.00", "1.01", "10.00", "99.99", "100.00"])
  fun `conversion should handle all valid 2-decimal amounts correctly`(amountStr: String) {
    val yuan = BigDecimal(amountStr)
    val cents = MoneyUtils.toCents(yuan)
    val convertedBack = MoneyUtils.fromCents(cents)

    assertEquals(yuan, convertedBack, "Failed for amount: $amountStr")

    // Also verify the cents value is correct
    val expectedCents = (BigDecimal(amountStr) * BigDecimal(100)).toLong()
    assertEquals(expectedCents, cents, "Incorrect cents for amount: $amountStr")
  }

  @Test
  fun `large number operations should not overflow unexpectedly`() {
    val largeAmount = Long.MAX_VALUE / 2

    // These should work without overflow
    assertNotNull(MoneyUtils.fromCents(largeAmount))
    assertNotNull(MoneyUtils.formatCents(largeAmount))

    // Addition should work within limits
    val result = MoneyUtils.addCents(largeAmount, 1L)
    assertEquals(largeAmount + 1, result)
  }

  @Test
  fun `arithmetic operations should maintain precision`() {
    // Test precision is maintained in complex operations
    val base = 12345L // ¥123.45

    val doubled = MoneyUtils.multiplyCents(base, BigDecimal("2.0"))
    assertEquals(24690L, doubled)

    val halved = MoneyUtils.multiplyCents(base, BigDecimal("0.5"))
    assertEquals(6172L, halved) // HALF_EVEN rounds 6172.5 -> 6172

    val percentage = MoneyUtils.calculatePercentage(base, BigDecimal("10.0"))
    assertEquals(1234L, percentage) // 10% of 12345, HALF_EVEN rounds 1234.5 -> 1234

    // Verify by converting back to yuan
    assertEquals(BigDecimal("246.90"), MoneyUtils.fromCents(doubled))
    assertEquals(BigDecimal("61.72"), MoneyUtils.fromCents(halved))
    assertEquals(BigDecimal("12.34"), MoneyUtils.fromCents(percentage))
  }

  @Test
  fun `formatting should be consistent with Chinese locale`() {
    val testCents = listOf(0L, 1L, 100L, 1234L, 999999L)

    for (cents in testCents) {
      val formattedFromCents = MoneyUtils.formatCents(cents)
      val yuan = MoneyUtils.fromCents(cents)
      val formattedFromYuan = MoneyUtils.formatYuan(yuan)

      assertEquals(
        formattedFromCents,
        formattedFromYuan,
        "Formatting inconsistency for cents: $cents",
      )

      assertTrue(
        formattedFromCents.startsWith("¥"),
        "Should start with ¥ symbol: $formattedFromCents",
      )
    }
  }
}