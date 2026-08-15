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
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnhancedJsonConvertersTest {
  private val mapper = jacksonObjectMapper()

  // ========== ObjectNodeConverter ==========

  @Test
  fun `ObjectNodeConverter should round trip object node`() {
    val converter = ObjectNodeConverter()
    val node = mapper.createObjectNode().put("name", "alice").put("age", 30)
    val column = converter.convertToDatabaseColumn(node)
    assertEquals("""{"name":"alice","age":30}""", column)
    val restored = converter.convertToEntityAttribute(column)
    assertEquals("alice", restored!!.get("name").asText())
  }

  @Test
  fun `ObjectNodeConverter should return null for null attribute`() {
    val converter = ObjectNodeConverter()
    assertNull(converter.convertToDatabaseColumn(null))
    assertNull(converter.convertToEntityAttribute(null))
    assertNull(converter.convertToEntityAttribute("   "))
  }

  @Test
  fun `ObjectNodeConverter should throw for non-object json`() {
    val converter = ObjectNodeConverter()
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("[1,2]") }
  }

  @Test
  fun `ObjectNodeConverter should throw for invalid json`() {
    val converter = ObjectNodeConverter()
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("not-json") }
  }

  // ========== ArrayNodeConverter ==========

  @Test
  fun `ArrayNodeConverter should round trip array node`() {
    val converter = ArrayNodeConverter()
    val node = mapper.createArrayNode().add("a").add(1)
    val column = converter.convertToDatabaseColumn(node)
    assertEquals("""["a",1]""", column)
    val restored = converter.convertToEntityAttribute(column)
    assertEquals(2, restored!!.size())
  }

  @Test
  fun `ArrayNodeConverter should return null for null attribute`() {
    val converter = ArrayNodeConverter()
    assertNull(converter.convertToDatabaseColumn(null))
    assertNull(converter.convertToEntityAttribute(null))
    assertNull(converter.convertToEntityAttribute(""))
  }

  @Test
  fun `ArrayNodeConverter should throw for non-array json`() {
    val converter = ArrayNodeConverter()
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("{}") }
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("bad") }
  }

  // ========== JsonNodeConverter ==========

  @Test
  fun `JsonNodeConverter should round trip object json`() {
    val converter = JsonNodeConverter()
    val node = mapper.createObjectNode().put("k", "v")
    val column = converter.convertToDatabaseColumn(node)
    assertEquals("""{"k":"v"}""", column)
    val restored = converter.convertToEntityAttribute(column)
    assertEquals("v", restored!!.get("k").asText())
  }

  @Test
  fun `JsonNodeConverter should round trip array json`() {
    val converter = JsonNodeConverter()
    val node = mapper.createArrayNode().add(1)
    val column = converter.convertToDatabaseColumn(node)
    val restored = converter.convertToEntityAttribute(column)
    assertEquals(1, restored!!.size())
  }

  @Test
  fun `JsonNodeConverter should return null for null attribute`() {
    val converter = JsonNodeConverter()
    assertNull(converter.convertToDatabaseColumn(null))
    assertNull(converter.convertToEntityAttribute(null))
  }

  @Test
  fun `JsonNodeConverter should throw for invalid json`() {
    val converter = JsonNodeConverter()
    assertThrows<IllegalArgumentException> { converter.convertToEntityAttribute("bad-json") }
  }
}
