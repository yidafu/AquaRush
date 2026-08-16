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

class JsonStringConverterTest :
  FunSpec({

    test("StringJsonConverter should pass through values") {
      val converter = StringJsonConverter()
      converter.convertToDatabaseColumn("abc") shouldBe "abc"
      converter.convertToEntityAttribute("abc") shouldBe "abc"
      converter.convertToDatabaseColumn(null).shouldBeNull()
      converter.convertToEntityAttribute(null).shouldBeNull()
    }
    test("StringListJsonConverter should round trip list") {
      val converter = StringListJsonConverter()
      val column = converter.convertToDatabaseColumn(listOf("a", "b"))
      column shouldBe """["a","b"]"""
      converter.convertToEntityAttribute(column) shouldBe listOf("a", "b")
    }
    test("StringListJsonConverter should return null for null") {
      val converter = StringListJsonConverter()
      converter.convertToDatabaseColumn(null).shouldBeNull()
      converter.convertToEntityAttribute(null).shouldBeNull()
    }
    test("StringListJsonConverter should handle empty list") {
      val converter = StringListJsonConverter()
      converter.convertToDatabaseColumn(emptyList()) shouldBe "[]"
      converter.convertToEntityAttribute("[]") shouldBe (emptyList<String>())
    }
    test("StringListJsonConverter should throw for invalid json") {
      val converter = StringListJsonConverter()
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("not-json") }
    }
    test("JsonMapConverter should round trip map") {
      val converter = JsonMapConverter()
      val column = converter.convertToDatabaseColumn(mapOf("k" to "v", "n" to 1))
      column shouldBe """{"k":"v","n":1}"""
      converter.convertToEntityAttribute(column) shouldBe mapOf("k" to "v", "n" to 1)
    }
    test("JsonMapConverter should return null for null") {
      val converter = JsonMapConverter()
      converter.convertToDatabaseColumn(null).shouldBeNull()
      converter.convertToEntityAttribute(null).shouldBeNull()
    }
    test("JsonMapConverter should handle empty map") {
      val converter = JsonMapConverter()
      converter.convertToDatabaseColumn(emptyMap()) shouldBe "{}"
      converter.convertToEntityAttribute("{}") shouldBe (emptyMap<String, Any>())
    }
    test("JsonMapConverter should throw for invalid json") {
      val converter = JsonMapConverter()
      shouldThrow<IllegalArgumentException> { converter.convertToEntityAttribute("bad") }
    }
  })
