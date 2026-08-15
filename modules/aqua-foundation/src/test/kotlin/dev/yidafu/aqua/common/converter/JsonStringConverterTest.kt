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

package dev.yidafu.aqua.common.converter

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonStringConverterTest {
  @Test
  fun `StringJsonConverter should pass through values`() {
    val converter = StringJsonConverter()
    assertEquals("abc", converter.convertToDatabaseColumn("abc"))
    assertEquals("abc", converter.convertToEntityAttribute("abc"))
    assertNull(converter.convertToDatabaseColumn(null))
    assertNull(converter.convertToEntityAttribute(null))
  }

  @Test
  fun `StringListJsonConverter should round trip list`() {
    val converter = StringListJsonConverter()
    val column = converter.convertToDatabaseColumn(listOf("a", "b"))
    assertEquals("""["a","b"]""", column)
    assertEquals(listOf("a", "b"), converter.convertToEntityAttribute(column))
  }

  @Test
  fun `StringListJsonConverter should return null for null`() {
    val converter = StringListJsonConverter()
    assertNull(converter.convertToDatabaseColumn(null))
    assertNull(converter.convertToEntityAttribute(null))
  }

  @Test
  fun `StringListJsonConverter should handle empty list`() {
    val converter = StringListJsonConverter()
    assertEquals("[]", converter.convertToDatabaseColumn(emptyList()))
    assertEquals(emptyList<String>(), converter.convertToEntityAttribute("[]"))
  }

  @Test
  fun `StringListJsonConverter should throw for invalid json`() {
    val converter = StringListJsonConverter()
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("not-json") }
  }

  @Test
  fun `JsonMapConverter should round trip map`() {
    val converter = JsonMapConverter()
    val column = converter.convertToDatabaseColumn(mapOf("k" to "v", "n" to 1))
    assertEquals("""{"k":"v","n":1}""", column)
    assertEquals(mapOf("k" to "v", "n" to 1), converter.convertToEntityAttribute(column))
  }

  @Test
  fun `JsonMapConverter should return null for null`() {
    val converter = JsonMapConverter()
    assertNull(converter.convertToDatabaseColumn(null))
    assertNull(converter.convertToEntityAttribute(null))
  }

  @Test
  fun `JsonMapConverter should handle empty map`() {
    val converter = JsonMapConverter()
    assertEquals("{}", converter.convertToDatabaseColumn(emptyMap()))
    assertEquals(emptyMap<String, Any>(), converter.convertToEntityAttribute("{}"))
  }

  @Test
  fun `JsonMapConverter should throw for invalid json`() {
    val converter = JsonMapConverter()
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("bad") }
  }
}
