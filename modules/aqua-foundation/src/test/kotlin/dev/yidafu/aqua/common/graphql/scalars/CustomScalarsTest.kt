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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class CustomScalarsTest :
  FunSpec({
    fun objectValue(vararg fields: Pair<String, Any>): ObjectValue {
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

    // ========== LocalDateTime ==========

    // ========== UUID ==========

    // ========== Long ==========

    // ========== Map ==========

    // ========== JsonObject ==========

    // ========== JsonArray ==========

    // ========== PrimaryId ==========

    test("BigDecimal serialize should convert supported types") {
      val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
      coercing.serialize(BigDecimal("1.50")) shouldBe "1.50"
      coercing.serialize(1.5) shouldBe "1.5"
      coercing.serialize(2.0f) shouldBe "2.0"
      coercing.serialize("3.14") shouldBe "3.14"
    }
    test("BigDecimal serialize should throw for invalid types") {
      val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
      shouldThrow<CoercingSerializeException> { coercing.serialize(42) }
      shouldThrow<CoercingSerializeException> { coercing.serialize("not-a-number") }
    }
    test("BigDecimal parseValue should handle supported types") {
      val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
      coercing.parseValue(BigDecimal("1.5")) shouldBe BigDecimal("1.5")
      coercing.parseValue("1.5") shouldBe BigDecimal("1.5")
      coercing.parseValue(2) shouldBe BigDecimal("2")
    }
    test("BigDecimal parseValue should throw for invalid input") {
      val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
      shouldThrow<CoercingParseValueException> { coercing.parseValue("abc") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(emptyList<Any>()) }
    }
    test("BigDecimal parseLiteral should parse string literal") {
      val coercing = BigDecimalScalar.GraphQL_TYPE.coercing
      coercing.parseLiteral("9.99") shouldBe BigDecimal("9.99")
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("x")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(IntValue.of(1)) }
    }
    test("LocalDateTime serialize should format supported values") {
      val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
      val dt = LocalDateTime.of(2024, 1, 2, 3, 4, 5)
      coercing.serialize(dt) shouldBe "2024-01-02T03:04:05"
      coercing.serialize("2024-01-02T03:04:05") shouldBe "2024-01-02T03:04:05"
    }
    test("LocalDateTime serialize should throw for invalid values") {
      val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
      shouldThrow<CoercingSerializeException> { coercing.serialize("not-a-date") }
      shouldThrow<CoercingSerializeException> { coercing.serialize(42) }
    }
    test("LocalDateTime parseValue should parse strings") {
      val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
      coercing.parseValue("2024-01-02T03:04:05") shouldBe LocalDateTime.of(2024, 1, 2, 3, 4, 5)
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(42) }
    }
    test("LocalDateTime parseLiteral should handle string and StringValue") {
      val coercing = LocalDateTimeScalar.GraphQL_TYPE.coercing
      coercing.parseLiteral("2024-01-02T03:04:05") shouldBe LocalDateTime.of(2024, 1, 2, 3, 4, 5)
      coercing.parseLiteral(StringValue.of("2024-01-02T03:04:05")) shouldBe LocalDateTime.of(2024, 1, 2, 3, 4, 5)
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(42) }
    }
    test("UUID serialize should format supported values") {
      val coercing = UUIDScalar.GraphQL_TYPE.coercing
      val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      coercing.serialize(uuid) shouldBe uuid.toString()
      coercing.serialize(uuid.toString()) shouldBe uuid.toString()
    }
    test("UUID serialize should throw for invalid values") {
      val coercing = UUIDScalar.GraphQL_TYPE.coercing
      shouldThrow<CoercingSerializeException> { coercing.serialize("not-a-uuid") }
      shouldThrow<CoercingSerializeException> { coercing.serialize(1) }
    }
    test("UUID parseValue should parse supported values") {
      val coercing = UUIDScalar.GraphQL_TYPE.coercing
      val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      coercing.parseValue(uuid) shouldBe uuid
      coercing.parseValue(uuid.toString()) shouldBe uuid
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(1) }
    }
    test("UUID parseLiteral should parse string literal") {
      val coercing = UUIDScalar.GraphQL_TYPE.coercing
      val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      coercing.parseLiteral(uuid.toString()) shouldBe uuid
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral("bad") }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(1) }
    }
    test("Long serialize should handle supported types") {
      val coercing = LongScalar.GraphQL_TYPE.coercing
      coercing.serialize(42L) shouldBe "42"
      coercing.serialize(42) shouldBe "42"
      coercing.serialize("42") shouldBe "42"
    }
    test("Long serialize should throw for invalid type") {
      val coercing = LongScalar.GraphQL_TYPE.coercing
      shouldThrow<CoercingSerializeException> { coercing.serialize(emptyList<Any>()) }
    }
    test("Long parseValue should handle supported types") {
      val coercing = LongScalar.GraphQL_TYPE.coercing
      coercing.parseValue(42L) shouldBe 42L
      coercing.parseValue(42) shouldBe 42L
      coercing.parseValue("42") shouldBe 42L
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(1.5) }
    }
    test("Long parseLiteral should handle IntValue and StringValue") {
      val coercing = LongScalar.GraphQL_TYPE.coercing
      coercing.parseLiteral(IntValue.of(42)) shouldBe 42L
      coercing.parseLiteral(StringValue.of("42")) shouldBe 42L
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(42) }
    }
    test("Map serialize should handle map and string input") {
      val coercing = MapScalar.GraphQL_TYPE.coercing
      coercing.serialize(mapOf("a" to 1)) shouldBe mapOf("a" to 1)
      coercing.serialize("{\"a\":1}") shouldBe mapOf("a" to 1)
    }
    test("Map serialize should throw for invalid input") {
      val coercing = MapScalar.GraphQL_TYPE.coercing
      shouldThrow<CoercingSerializeException> { coercing.serialize("not-json") }
      shouldThrow<CoercingSerializeException> { coercing.serialize(42) }
    }
    test("Map parseValue should handle map and string input") {
      val coercing = MapScalar.GraphQL_TYPE.coercing
      coercing.parseValue(mapOf("a" to 1)) shouldBe mapOf("a" to 1)
      coercing.parseValue("{\"a\":1}") shouldBe mapOf("a" to 1)
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(42) }
    }
    test("Map parseLiteral should build map from ObjectValue") {
      val coercing = MapScalar.GraphQL_TYPE.coercing
      val ast = objectValue("name" to "alice", "age" to 30, "score" to 9.5, "active" to true)

      @Suppress("UNCHECKED_CAST")
      val result = coercing.parseLiteral(ast) as Map<String, Any>
      result["name"] shouldBe "alice"
      result["age"] shouldBe java.math.BigInteger.valueOf(30)
      result["score"] shouldBe java.math.BigDecimal("9.5")
      result["active"] shouldBe true
    }
    test("Map parseLiteral should parse StringValue json") {
      val coercing = MapScalar.GraphQL_TYPE.coercing
      coercing.parseLiteral(StringValue.of("{\"a\":1}")) shouldBe mapOf("a" to 1)
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(42) }
    }
    test("JsonObject serialize should handle supported types") {
      val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
      val mapper =
        tools.jackson.module.kotlin
          .jacksonObjectMapper()
      val node = mapper.createObjectNode().put("k", "v")
      coercing.serialize(node) shouldBe node
      coercing.serialize(mapOf("k" to "v")) shouldBe node
      coercing.serialize("{\"k\":\"v\"}") shouldBe node
    }
    test("JsonObject serialize should throw for invalid input") {
      val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
      val mapper =
        tools.jackson.module.kotlin
          .jacksonObjectMapper()
      shouldThrow<CoercingSerializeException> { coercing.serialize(mapper.createArrayNode()) }
      shouldThrow<CoercingSerializeException> { coercing.serialize("not-json") }
      shouldThrow<CoercingSerializeException> { coercing.serialize("[1,2]") }
      shouldThrow<CoercingSerializeException> { coercing.serialize(42) }
    }
    test("JsonObject parseValue should handle supported types") {
      val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
      val mapper =
        tools.jackson.module.kotlin
          .jacksonObjectMapper()
      val node = mapper.createObjectNode().put("k", "v")
      coercing.parseValue(node) shouldBe node
      coercing.parseValue(mapOf("k" to "v")) shouldBe node
      coercing.parseValue("{\"k\":\"v\"}") shouldBe node
      shouldThrow<CoercingParseValueException> { coercing.parseValue("[1]") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(42) }
    }
    test("JsonObject parseLiteral should handle ObjectValue and StringValue") {
      val coercing = JsonObjectScalar.GraphQL_TYPE.coercing
      val result = coercing.parseLiteral(objectValue("name" to "alice")) as ObjectNode
      (result is ObjectNode) shouldBe true
      result.get("name").asText() shouldBe "alice"
      val fromString = coercing.parseLiteral(StringValue.of("{\"a\":1}")) as ObjectNode
      fromString.get("a").asInt() shouldBe 1
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("[1]")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(42) }
    }
    test("JsonArray serialize should handle supported types") {
      val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
      val mapper =
        tools.jackson.module.kotlin
          .jacksonObjectMapper()
      val node = mapper.createArrayNode().add("a")
      coercing.serialize(node) shouldBe node
      coercing.serialize(listOf("a")) shouldBe node
      coercing.serialize("[\"a\"]") shouldBe node
    }
    test("JsonArray serialize should throw for invalid input") {
      val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
      val mapper =
        tools.jackson.module.kotlin
          .jacksonObjectMapper()
      shouldThrow<CoercingSerializeException> { coercing.serialize(mapper.createObjectNode()) }
      shouldThrow<CoercingSerializeException> { coercing.serialize("bad") }
      shouldThrow<CoercingSerializeException> { coercing.serialize("{}") }
      shouldThrow<CoercingSerializeException> { coercing.serialize(42) }
    }
    test("JsonArray parseValue should handle supported types") {
      val coercing = JsonArrayScalar.GraphQL_TYPE.coercing
      val mapper =
        tools.jackson.module.kotlin
          .jacksonObjectMapper()
      val node = mapper.createArrayNode().add(1)
      coercing.parseValue(node) shouldBe node
      coercing.parseValue(listOf(1)) shouldBe node
      coercing.parseValue("[1]") shouldBe node
      shouldThrow<CoercingParseValueException> { coercing.parseValue("{\"a\":1}") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(42) }
    }
    test("JsonArray parseLiteral should handle ArrayValue and StringValue") {
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
      (result is ArrayNode) shouldBe true
      result.size() shouldBe 4
      val fromString = coercing.parseLiteral(StringValue.of("[1,2]")) as ArrayNode
      fromString.size() shouldBe 2
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("{}")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(42) }
    }
    test("PrimaryId serialize should handle supported types") {
      val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
      coercing.serialize(42L) shouldBe "42"
      coercing.serialize(42) shouldBe "42"
      coercing.serialize("42") shouldBe "42"
      shouldThrow<CoercingSerializeException> { coercing.serialize("0") }
      shouldThrow<CoercingSerializeException> { coercing.serialize("bad") }
      shouldThrow<CoercingSerializeException> { coercing.serialize(emptyList<Any>()) }
    }
    test("PrimaryId parseValue should handle supported types") {
      val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
      coercing.parseValue(42L) shouldBe 42L
      coercing.parseValue(42) shouldBe 42L
      coercing.parseValue("42") shouldBe 42L
      shouldThrow<CoercingParseValueException> { coercing.parseValue(0L) }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(-1) }
      shouldThrow<CoercingParseValueException> { coercing.parseValue("bad") }
      shouldThrow<CoercingParseValueException> { coercing.parseValue(emptyList<Any>()) }
    }
    test("PrimaryId parseLiteral should handle IntValue and StringValue") {
      val coercing = PrimaryIdScalar.GraphQL_TYPE.coercing
      coercing.parseLiteral(IntValue.of(42)) shouldBe 42L
      coercing.parseLiteral(StringValue.of("42")) shouldBe 42L
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(IntValue.of(0)) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(StringValue.of("bad")) }
      shouldThrow<CoercingParseLiteralException> { coercing.parseLiteral(42) }
    }
    test("scalar names should match conventions") {
      BigDecimalScalar.GraphQL_TYPE.name shouldBe "BigDecimal"
      LocalDateTimeScalar.GraphQL_TYPE.name shouldBe "LocalDateTime"
      UUIDScalar.GraphQL_TYPE.name shouldBe "UUID"
      LongScalar.GraphQL_TYPE.name shouldBe "Long"
      MapScalar.GraphQL_TYPE.name shouldBe "Map"
      JsonObjectScalar.GraphQL_TYPE.name shouldBe "JsonObject"
      JsonArrayScalar.GraphQL_TYPE.name shouldBe "JsonArray"
      PrimaryIdScalar.GraphQL_TYPE.name shouldBe "PrimaryId"
    }
    test("Map serialize should throw GraphQLException subclass") {
      shouldThrow<GraphQLException> { MapScalar.GraphQL_TYPE.coercing.serialize("bad") }
    }
  })
