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

package dev.yidafu.aqua.common.graphql.scalars

import graphql.GraphQLException
import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomScalarsTest {
  private fun objectValue(vararg fields: Pair<String, Any>): ObjectValue {
    val builder = ObjectValue.newObjectValue()
    fields.forEach { (name, value) ->
      val astValue =
        when (value) {
          is String -> StringValue.of(value)
          is Int -> IntValue.of(value)
          is Double -> FloatValue.of(value)
          is Boolean -> BooleanValue.of(value)
          else -> StringValue.of(value.toString())
        }
      builder.objectField(
        ObjectField
          .newObjectField()
          .name(name)
          .value(astValue)
          .build(),
      )
    }
    return builder.build()
  }

  // ========== BigDecimal ==========

  @Test
  fun `BigDecimal serialize should convert supported types`() {
    val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
    assertEquals("1.50", coercing.serialize(BigDecimal("1.50")))
    assertEquals("1.5", coercing.serialize(1.5))
    assertEquals("2.0", coercing.serialize(2.0f))
    assertEquals("3.14", coercing.serialize("3.14"))
  }

  @Test
  fun `BigDecimal serialize should throw for invalid types`() {
    val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
    assertThrows<CoercingSerializeException> { coercing.serialize(42) }
    assertThrows<CoercingSerializeException> { coercing.serialize("not-a-number") }
  }

  @Test
  fun `BigDecimal parseValue should handle supported types`() {
    val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
    assertEquals(BigDecimal("1.5"), coercing.parseValue(BigDecimal("1.5")))
    assertEquals(BigDecimal("1.5"), coercing.parseValue("1.5"))
    assertEquals(BigDecimal("2"), coercing.parseValue(2))
  }

  @Test
  fun `BigDecimal parseValue should throw for invalid input`() {
    val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
    assertThrows<CoercingParseValueException> { coercing.parseValue("abc") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(emptyList<Any>()) }
  }

  @Test
  fun `BigDecimal parseLiteral should parse string literal`() {
    val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
    assertEquals(BigDecimal("9.99"), coercing.parseLiteral("9.99"))
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("x")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(IntValue.of(1)) }
  }

  // ========== LocalDateTime ==========

  @Test
  fun `LocalDateTime serialize should format supported values`() {
    val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
    val dt = LocalDateTime.of(2024, 1, 2, 3, 4, 5)
    assertEquals("2024-01-02T03:04:05", coercing.serialize(dt))
    assertEquals("2024-01-02T03:04:05", coercing.serialize("2024-01-02T03:04:05"))
  }

  @Test
  fun `LocalDateTime serialize should throw for invalid values`() {
    val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
    assertThrows<CoercingSerializeException> { coercing.serialize("not-a-date") }
    assertThrows<CoercingSerializeException> { coercing.serialize(42) }
  }

  @Test
  fun `LocalDateTime parseValue should parse strings`() {
    val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
    assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4, 5), coercing.parseValue("2024-01-02T03:04:05"))
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(42) }
  }

  @Test
  fun `LocalDateTime parseLiteral should handle string and StringValue`() {
    val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
    assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4, 5), coercing.parseLiteral("2024-01-02T03:04:05"))
    assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4, 5), coercing.parseLiteral(StringValue.of("2024-01-02T03:04:05")))
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(42) }
  }

  // ========== UUID ==========

  @Test
  fun `UUID serialize should format supported values`() {
    val coercing = UUIDScalar.GraphQL_TYPE.coercing
    val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    assertEquals(uuid.toString(), coercing.serialize(uuid))
    assertEquals(uuid.toString(), coercing.serialize(uuid.toString()))
  }

  @Test
  fun `UUID serialize should throw for invalid values`() {
    val coercing = UUIDScalar.GraphQL_TYPE.coercing
    assertThrows<CoercingSerializeException> { coercing.serialize("not-a-uuid") }
    assertThrows<CoercingSerializeException> { coercing.serialize(1) }
  }

  @Test
  fun `UUID parseValue should parse supported values`() {
    val coercing = UUIDScalar.GraphQL_TYPE.coercing
    val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    assertEquals(uuid, coercing.parseValue(uuid))
    assertEquals(uuid, coercing.parseValue(uuid.toString()))
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(1) }
  }

  @Test
  fun `UUID parseLiteral should parse string literal`() {
    val coercing = UUIDScalar.GraphQL_TYPE.coercing
    val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    assertEquals(uuid, coercing.parseLiteral(uuid.toString()))
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral("bad") }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(1) }
  }

  // ========== Long ==========

  @Test
  fun `Long serialize should handle supported types`() {
    val coercing = LongScalar.GraphQL_TYPE.coercing
    assertEquals("42", coercing.serialize(42L))
    assertEquals("42", coercing.serialize(42))
    assertEquals("42", coercing.serialize("42"))
  }

  @Test
  fun `Long serialize should throw for invalid type`() {
    val coercing = LongScalar.GraphQL_TYPE.coercing
    assertThrows<CoercingSerializeException> { coercing.serialize(emptyList<Any>()) }
  }

  @Test
  fun `Long parseValue should handle supported types`() {
    val coercing = LongScalar.GraphQL_TYPE.coercing
    assertEquals(42L, coercing.parseValue(42L))
    assertEquals(42L, coercing.parseValue(42))
    assertEquals(42L, coercing.parseValue("42"))
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(1.5) }
  }

  @Test
  fun `Long parseLiteral should handle IntValue and StringValue`() {
    val coercing = LongScalar.GraphQL_TYPE.coercing
    assertEquals(42L, coercing.parseLiteral(IntValue.of(42)))
    assertEquals(42L, coercing.parseLiteral(StringValue.of("42")))
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(42) }
  }

  // ========== Map ==========

  @Test
  fun `Map serialize should handle map and string input`() {
    val coercing = MapScalar.GraphQL_TYPE.coercing
    assertEquals(mapOf("a" to 1), coercing.serialize(mapOf("a" to 1)))
    assertEquals(mapOf("a" to 1), coercing.serialize("{\"a\":1}"))
  }

  @Test
  fun `Map serialize should throw for invalid input`() {
    val coercing = MapScalar.GraphQL_TYPE.coercing
    assertThrows<CoercingSerializeException> { coercing.serialize("not-json") }
    assertThrows<CoercingSerializeException> { coercing.serialize(42) }
  }

  @Test
  fun `Map parseValue should handle map and string input`() {
    val coercing = MapScalar.GraphQL_TYPE.coercing
    assertEquals(mapOf("a" to 1), coercing.parseValue(mapOf("a" to 1)))
    assertEquals(mapOf("a" to 1), coercing.parseValue("{\"a\":1}"))
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(42) }
  }

  @Test
  fun `Map parseLiteral should build map from ObjectValue`() {
    val coercing = MapScalar.GraphQL_TYPE.coercing
    val ast = objectValue("name" to "alice", "age" to 30, "score" to 9.5, "active" to true)

    @Suppress("UNCHECKED_CAST")
    val result = coercing.parseLiteral(ast) as Map<String, Any>
    assertEquals("alice", result["name"])
    assertEquals(java.math.BigInteger.valueOf(30), result["age"])
    assertEquals(java.math.BigDecimal("9.5"), result["score"])
    assertEquals(true, result["active"])
  }

  @Test
  fun `Map parseLiteral should parse StringValue json`() {
    val coercing = MapScalar.GraphQL_TYPE.coercing
    assertEquals(mapOf("a" to 1), coercing.parseLiteral(StringValue.of("{\"a\":1}")))
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(42) }
  }

  // ========== JsonObject ==========

  @Test
  fun `JsonObject serialize should handle supported types`() {
    val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
    val mapper =
      tools.jackson.module.kotlin
        .jacksonObjectMapper()
    val node = mapper.createObjectNode().put("k", "v")
    assertEquals(node, coercing.serialize(node))
    assertEquals(node, coercing.serialize(mapOf("k" to "v")))
    assertEquals(node, coercing.serialize("{\"k\":\"v\"}"))
  }

  @Test
  fun `JsonObject serialize should throw for invalid input`() {
    val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
    val mapper =
      tools.jackson.module.kotlin
        .jacksonObjectMapper()
    assertThrows<CoercingSerializeException> { coercing.serialize(mapper.createArrayNode()) }
    assertThrows<CoercingSerializeException> { coercing.serialize("not-json") }
    assertThrows<CoercingSerializeException> { coercing.serialize("[1,2]") }
    assertThrows<CoercingSerializeException> { coercing.serialize(42) }
  }

  @Test
  fun `JsonObject parseValue should handle supported types`() {
    val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
    val mapper =
      tools.jackson.module.kotlin
        .jacksonObjectMapper()
    val node = mapper.createObjectNode().put("k", "v")
    assertEquals(node, coercing.parseValue(node))
    assertEquals(node, coercing.parseValue(mapOf("k" to "v")))
    assertEquals(node, coercing.parseValue("{\"k\":\"v\"}"))
    assertThrows<CoercingParseValueException> { coercing.parseValue("[1]") }
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(42) }
  }

  @Test
  fun `JsonObject parseLiteral should handle ObjectValue and StringValue`() {
    val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
    val result = coercing.parseLiteral(objectValue("name" to "alice")) as ObjectNode
    assertTrue(result is ObjectNode)
    assertEquals("alice", result.get("name").asText())
    val fromString = coercing.parseLiteral(StringValue.of("{\"a\":1}")) as ObjectNode
    assertEquals(1, fromString.get("a").asInt())
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("[1]")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(42) }
  }

  // ========== JsonArray ==========

  @Test
  fun `JsonArray serialize should handle supported types`() {
    val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
    val mapper =
      tools.jackson.module.kotlin
        .jacksonObjectMapper()
    val node = mapper.createArrayNode().add("a")
    assertEquals(node, coercing.serialize(node))
    assertEquals(node, coercing.serialize(listOf("a")))
    assertEquals(node, coercing.serialize("[\"a\"]"))
  }

  @Test
  fun `JsonArray serialize should throw for invalid input`() {
    val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
    val mapper =
      tools.jackson.module.kotlin
        .jacksonObjectMapper()
    assertThrows<CoercingSerializeException> { coercing.serialize(mapper.createObjectNode()) }
    assertThrows<CoercingSerializeException> { coercing.serialize("bad") }
    assertThrows<CoercingSerializeException> { coercing.serialize("{}") }
    assertThrows<CoercingSerializeException> { coercing.serialize(42) }
  }

  @Test
  fun `JsonArray parseValue should handle supported types`() {
    val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
    val mapper =
      tools.jackson.module.kotlin
        .jacksonObjectMapper()
    val node = mapper.createArrayNode().add(1)
    assertEquals(node, coercing.parseValue(node))
    assertEquals(node, coercing.parseValue(listOf(1)))
    assertEquals(node, coercing.parseValue("[1]"))
    assertThrows<CoercingParseValueException> { coercing.parseValue("{\"a\":1}") }
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(42) }
  }

  @Test
  fun `JsonArray parseLiteral should handle ArrayValue and StringValue`() {
    val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
    val arrayAst =
      ArrayValue
        .newArrayValue()
        .value(StringValue.of("a"))
        .value(IntValue.of(1))
        .value(FloatValue.of(1.5))
        .value(BooleanValue.of(true))
        .build()
    val result = coercing.parseLiteral(arrayAst) as ArrayNode
    assertTrue(result is ArrayNode)
    assertEquals(4, result.size())
    val fromString = coercing.parseLiteral(StringValue.of("[1,2]")) as ArrayNode
    assertEquals(2, fromString.size())
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("{}")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(42) }
  }

  // ========== PrimaryId ==========

  @Test
  fun `PrimaryId serialize should handle supported types`() {
    val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
    assertEquals("42", coercing.serialize(42L))
    assertEquals("42", coercing.serialize(42))
    assertEquals("42", coercing.serialize("42"))
    assertThrows<CoercingSerializeException> { coercing.serialize("0") }
    assertThrows<CoercingSerializeException> { coercing.serialize("bad") }
    assertThrows<CoercingSerializeException> { coercing.serialize(emptyList<Any>()) }
  }

  @Test
  fun `PrimaryId parseValue should handle supported types`() {
    val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
    assertEquals(42L, coercing.parseValue(42L))
    assertEquals(42L, coercing.parseValue(42))
    assertEquals(42L, coercing.parseValue("42"))
    assertThrows<CoercingParseValueException> { coercing.parseValue(0L) }
    assertThrows<CoercingParseValueException> { coercing.parseValue(-1) }
    assertThrows<CoercingParseValueException> { coercing.parseValue("bad") }
    assertThrows<CoercingParseValueException> { coercing.parseValue(emptyList<Any>()) }
  }

  @Test
  fun `PrimaryId parseLiteral should handle IntValue and StringValue`() {
    val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
    assertEquals(42L, coercing.parseLiteral(IntValue.of(42)))
    assertEquals(42L, coercing.parseLiteral(StringValue.of("42")))
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(IntValue.of(0)) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
    assertThrows<CoercingParseLiteralException> { coercing.parseLiteral(42) }
  }

  @Test
  fun `scalar names should match conventions`() {
    assertEquals("BigDecimal", BigDecimalScalar.GraphQL_TYPE.name)
    assertEquals("LocalDateTime", LocalDateTimeScalar.GraphQL_TYPE.name)
    assertEquals("UUID", UUIDScalar.GraphQL_TYPE.name)
    assertEquals("Long", LongScalar.GraphQL_TYPE.name)
    assertEquals("Map", MapScalar.GraphQL_TYPE.name)
    assertEquals("JsonObject", JsonObjectScalar.GraphQL_TYPE.name)
    assertEquals("JsonArray", JsonArrayScalar.GraphQL_TYPE.name)
    assertEquals("PrimaryId", PrimaryIdScalar.GraphQL_TYPE.name)
  }

  @Test
  fun `Map serialize should throw GraphQLException subclass`() {
    assertThrows<GraphQLException> { MapScalar.GraphQL_TYPE.coercing.serialize("bad") }
  }
}
