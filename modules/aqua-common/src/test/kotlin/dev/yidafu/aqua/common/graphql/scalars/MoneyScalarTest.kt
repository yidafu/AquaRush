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

package dev.yidafu.aqua.common.graphql.scalars

import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("MoneyScalar GraphQL Tests")
class MoneyScalarTest {

  private val coercing = MoneyScalar.GraphQL_TYPE.coercing

  // ========== Serialization Tests ==========

  @Test
  @DisplayName("Should serialize Long values correctly")
  fun `serialize Long correctly`() {
    assertEquals("0", coercing.serialize(0L))
    assertEquals("100", coercing.serialize(100L))
    assertEquals("999999", coercing.serialize(999999L))
    assertEquals(Long.MAX_VALUE.toString(), coercing.serialize(Long.MAX_VALUE))
  }

  @Test
  @DisplayName("Should serialize Int values correctly")
  fun `serialize Int correctly`() {
    assertEquals("0", coercing.serialize(0))
    assertEquals("100", coercing.serialize(100))
    assertEquals("999999", coercing.serialize(999999))
    assertEquals(Int.MAX_VALUE.toString(), coercing.serialize(Int.MAX_VALUE))
  }

  @Test
  @DisplayName("Should serialize valid String values correctly")
  fun `serialize valid String correctly`() {
    assertEquals("0", coercing.serialize("0"))
    assertEquals("100", coercing.serialize("100"))
    assertEquals("999999", coercing.serialize("999999"))
  }

  @Test
  @DisplayName("Should throw exception for negative Long values")
  fun `serialize negative Long throws exception`() {
    val exception = assertThrows<CoercingSerializeException> {
      coercing.serialize(-1L)
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for negative Int values")
  fun `serialize negative Int throws exception`() {
    val exception = assertThrows<CoercingSerializeException> {
      coercing.serialize(-1)
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for negative String values")
  fun `serialize negative String throws exception`() {
    val exception = assertThrows<CoercingSerializeException> {
      coercing.serialize("-1")
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for invalid String values")
  fun `serialize invalid String throws exception`() {
    assertThrows<CoercingSerializeException> {
      coercing.serialize("abc")
    }
    assertThrows<CoercingSerializeException> {
      coercing.serialize("12.34")
    }
    assertThrows<CoercingSerializeException> {
      coercing.serialize("")
    }
  }

  @Test
  @DisplayName("Should throw exception for unsupported types")
  fun `serialize unsupported types throws exception`() {
    assertThrows<CoercingSerializeException> {
      coercing.serialize(12.34)
    }
    assertThrows<CoercingSerializeException> {
      coercing.serialize(true)
    }
    assertThrows<CoercingSerializeException> {
      coercing.serialize(listOf(1, 2, 3))
    }
    assertThrows<CoercingSerializeException> {
      coercing.serialize(mapOf("value" to 100))
    }
  }

  // ========== Parse Value Tests ==========

  @Test
  @DisplayName("Should parse Long values correctly")
  fun `parseValue Long correctly`() {
    assertEquals(0L, coercing.parseValue(0L))
    assertEquals(100L, coercing.parseValue(100L))
    assertEquals(999999L, coercing.parseValue(999999L))
    assertEquals(Long.MAX_VALUE, coercing.parseValue(Long.MAX_VALUE))
  }

  @Test
  @DisplayName("Should parse Int values correctly")
  fun `parseValue Int correctly`() {
    assertEquals(0L, coercing.parseValue(0))
    assertEquals(100L, coercing.parseValue(100))
    assertEquals(999999L, coercing.parseValue(999999))
    assertEquals(Int.MAX_VALUE.toLong(), coercing.parseValue(Int.MAX_VALUE))
  }

  @Test
  @DisplayName("Should parse valid String values correctly")
  fun `parseValue valid String correctly`() {
    assertEquals(0L, coercing.parseValue("0"))
    assertEquals(100L, coercing.parseValue("100"))
    assertEquals(999999L, coercing.parseValue("999999"))
    assertEquals(Long.MAX_VALUE, coercing.parseValue(Long.MAX_VALUE.toString()))
  }

  @Test
  @DisplayName("Should throw exception for negative Long values in parseValue")
  fun `parseValue negative Long throws exception`() {
    val exception = assertThrows<CoercingParseValueException> {
      coercing.parseValue(-1L)
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for negative Int values in parseValue")
  fun `parseValue negative Int throws exception`() {
    val exception = assertThrows<CoercingParseValueException> {
      coercing.parseValue(-1)
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for negative String values in parseValue")
  fun `parseValue negative String throws exception`() {
    val exception = assertThrows<CoercingParseValueException> {
      coercing.parseValue("-1")
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for invalid String values in parseValue")
  fun `parseValue invalid String throws exception`() {
    assertThrows<CoercingParseValueException> {
      coercing.parseValue("abc")
    }
    assertThrows<CoercingParseValueException> {
      coercing.parseValue("12.34")
    }
    assertThrows<CoercingParseValueException> {
      coercing.parseValue("")
    }
    assertThrows<CoercingParseValueException> {
      coercing.parseValue("1e10")
    }
  }

  @Test
  @DisplayName("Should throw exception for unsupported types in parseValue")
  fun `parseValue unsupported types throws exception`() {
    assertThrows<CoercingParseValueException> {
      coercing.parseValue(12.34)
    }
    assertThrows<CoercingParseValueException> {
      coercing.parseValue(true)
    }
    assertThrows<CoercingParseValueException> {
      coercing.parseValue(listOf(1, 2, 3))
    }
    assertThrows<CoercingParseValueException> {
      coercing.parseValue(mapOf("value" to 100))
    }
  }

  // ========== Parse Literal Tests ==========

  @Test
  @DisplayName("Should parse IntValue literals correctly")
  fun `parseLiteral IntValue correctly`() {
    assertEquals(0L, coercing.parseLiteral(IntValue.newIntValue(0).build()))
    assertEquals(100L, coercing.parseLiteral(IntValue.newIntValue(100).build()))
    assertEquals(999999L, coercing.parseLiteral(IntValue.newIntValue(999999).build()))
  }

  @Test
  @DisplayName("Should parse valid StringValue literals correctly")
  fun `parseLiteral valid StringValue correctly`() {
    assertEquals(0L, coercing.parseLiteral(StringValue.newStringValue("0").build()))
    assertEquals(100L, coercing.parseLiteral(StringValue.newStringValue("100").build()))
    assertEquals(999999L, coercing.parseLiteral(StringValue.newStringValue("999999").build()))
  }

  @Test
  @DisplayName("Should throw exception for negative IntValue literals")
  fun `parseLiteral negative IntValue throws exception`() {
    val exception = assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(IntValue.newIntValue(-1).build())
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for negative StringValue literals")
  fun `parseLiteral negative StringValue throws exception`() {
    val exception = assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(StringValue.newStringValue("-1").build())
    }
    assertTrue(exception.message!!.contains("Money value cannot be negative"))
  }

  @Test
  @DisplayName("Should throw exception for invalid StringValue literals")
  fun `parseLiteral invalid StringValue throws exception`() {
    assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(StringValue.newStringValue("abc").build())
    }
    assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(StringValue.newStringValue("12.34").build())
    }
    assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(StringValue.newStringValue("").build())
    }
    assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(StringValue.newStringValue("1e10").build())
    }
  }

  @Test
  @DisplayName("Should throw exception for null StringValue")
  fun `parseLiteral null StringValue throws exception`() {
    val exception = assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(StringValue.newStringValue(null).build())
    }
    assertTrue(exception.message!!.contains("String value is null"))
  }

  @Test
  @DisplayName("Should throw exception for unsupported literal types")
  fun `parseLiteral unsupported types throws exception`() {
    assertThrows<CoercingParseLiteralException> {
      coercing.parseLiteral(object : graphql.language.Value() {})
    }
  }

  // ========== Round-trip Tests ==========

  @Test
  @DisplayName("Should maintain value integrity through serialization and parsing")
  fun `round-trip conversion maintains integrity`() {
    val testValues = listOf(0L, 1L, 100L, 999999L, Long.MAX_VALUE)

    for (value in testValues) {
      // Serialize then parseValue
      val serialized = coercing.serialize(value)
      val parsedFromValue = coercing.parseValue(serialized)
      assertEquals(value, parsedFromValue, "Round-trip failed for value: $value")

      // Serialize then parseLiteral (as IntValue)
      val intValue = IntValue.newIntValue(value).build()
      val parsedFromLiteral = coercing.parseLiteral(intValue)
      assertEquals(value, parsedFromLiteral, "Literal round-trip failed for value: $value")

      // Serialize then parseLiteral (as StringValue)
      val stringValue = StringValue.newStringValue(value.toString()).build()
      val parsedFromStringLiteral = coercing.parseLiteral(stringValue)
      assertEquals(value, parsedFromStringLiteral, "String literal round-trip failed for value: $value")
    }
  }

  // ========== Type Configuration Tests ==========

  @Test
  @DisplayName("Money scalar should have correct type configuration")
  fun `scalar type configuration is correct`() {
    assertEquals("Money", MoneyScalar.GraphQL_TYPE.name)
    assertNotNull(MoneyScalar.GraphQL_TYPE.description)
    assertTrue(MoneyScalar.GraphQL_TYPE.description!!.contains("cents"))
    assertTrue(MoneyScalar.GraphQL_TYPE.description!!.contains("分"))
  }

  // ========== Edge Case Tests ==========

  @Test
  @DisplayName("Should handle boundary values correctly")
  fun `handle boundary values correctly`() {
    // Test maximum safe value
    val maxValue = Long.MAX_VALUE
    assertEquals(maxValue.toString(), coercing.serialize(maxValue))
    assertEquals(maxValue, coercing.parseValue(maxValue))
    assertEquals(maxValue, coercing.parseLiteral(IntValue.newIntValue(maxValue).build()))

    // Test zero
    val zero = 0L
    assertEquals("0", coercing.serialize(zero))
    assertEquals(zero, coercing.parseValue(zero))
    assertEquals(zero, coercing.parseLiteral(IntValue.newIntValue(zero).build()))

    // Test minimum positive value
    val minPositive = 1L
    assertEquals("1", coercing.serialize(minPositive))
    assertEquals(minPositive, coercing.parseValue(minPositive))
    assertEquals(minPositive, coercing.parseLiteral(IntValue.newIntValue(minPositive).build()))
  }

  @Test
  @DisplayName("Should handle large number strings correctly")
  fun `handle large number strings correctly`() {
    val largeNumberString = Long.MAX_VALUE.toString()
    assertEquals(Long.MAX_VALUE, coercing.parseValue(largeNumberString))
    assertEquals(Long.MAX_VALUE, coercing.parseLiteral(StringValue.newStringValue(largeNumberString).build()))
  }
}
