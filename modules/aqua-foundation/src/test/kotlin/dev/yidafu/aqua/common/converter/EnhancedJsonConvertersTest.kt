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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import tools.jackson.module.kotlin.jacksonObjectMapper

class EnhancedJsonConvertersTest :
  FunSpec({
    val mapper = jacksonObjectMapper()

    // ========== ObjectNodeConverter ==========

    // ========== ArrayNodeConverter ==========

    // ========== JsonNodeConverter ==========

    test("ObjectNodeConverter should round trip object node") {
      val converter = ObjectNodeConverter()
      val node = mapper.createObjectNode().put("name", "alice").put("age", 30)
      val column = converter.convertToDatabaseColumn(node)
      column shouldBe """{"name":"alice","age":30}"""
      val restored = converter.convertToEntityAttribute(column)
      restored!!.get("name").asText() shouldBe "alice"
    }
    test("ObjectNodeConverter should return null for null attribute") {
      val converter = ObjectNodeConverter()
      converter.convertToDatabaseColumn(null).shouldBeNull()
      converter.convertToEntityAttribute(null).shouldBeNull()
      converter.convertToEntityAttribute("   ").shouldBeNull()
    }
    test("ObjectNodeConverter should throw for non-object json") {
      val converter = ObjectNodeConverter()
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("[1,2]") }
    }
    test("ObjectNodeConverter should throw for invalid json") {
      val converter = ObjectNodeConverter()
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("not-json") }
    }
    test("ArrayNodeConverter should round trip array node") {
      val converter = ArrayNodeConverter()
      val node = mapper.createArrayNode().add("a").add(1)
      val column = converter.convertToDatabaseColumn(node)
      column shouldBe """["a",1]"""
      val restored = converter.convertToEntityAttribute(column)
      restored!!.size() shouldBe 2
    }
    test("ArrayNodeConverter should return null for null attribute") {
      val converter = ArrayNodeConverter()
      converter.convertToDatabaseColumn(null).shouldBeNull()
      converter.convertToEntityAttribute(null).shouldBeNull()
      converter.convertToEntityAttribute("").shouldBeNull()
    }
    test("ArrayNodeConverter should throw for non-array json") {
      val converter = ArrayNodeConverter()
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("{}") }
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("bad") }
    }
    test("JsonNodeConverter should round trip object json") {
      val converter = JsonNodeConverter()
      val node = mapper.createObjectNode().put("k", "v")
      val column = converter.convertToDatabaseColumn(node)
      column shouldBe """{"k":"v"}"""
      val restored = converter.convertToEntityAttribute(column)
      restored!!.get("k").asText() shouldBe "v"
    }
    test("JsonNodeConverter should round trip array json") {
      val converter = JsonNodeConverter()
      val node = mapper.createArrayNode().add(1)
      val column = converter.convertToDatabaseColumn(node)
      val restored = converter.convertToEntityAttribute(column)
      restored!!.size() shouldBe 1
    }
    test("JsonNodeConverter should return null for null attribute") {
      val converter = JsonNodeConverter()
      converter.convertToDatabaseColumn(null).shouldBeNull()
      converter.convertToEntityAttribute(null).shouldBeNull()
    }
    test("JsonNodeConverter should throw for invalid json") {
      val converter = JsonNodeConverter()
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("bad-json") }
    }
  })
