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

import graphql.language.*
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.forEach

object BigDecimalScalar {
  private val coercing =
    object : Coercing<BigDecimal, String> {
      override fun serialize(dataFetcherResult: Any): String =
        when (dataFetcherResult) {
          is BigDecimal -> dataFetcherResult.toString()
          is Double -> dataFetcherResult.toBigDecimal().toString()
          is Float -> dataFetcherResult.toBigDecimal().toString()
          is String -> {
            try {
              BigDecimal(dataFetcherResult).toString()
            } catch (e: NumberFormatException) {
              throw CoercingSerializeException("Expected BigDecimal but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected BigDecimal but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): BigDecimal =
        when (input) {
          is BigDecimal -> input
          is String -> {
            try {
              BigDecimal(input)
            } catch (e: NumberFormatException) {
              throw CoercingParseValueException("Expected valid BigDecimal string but got: $input")
            }
          }
          is Number -> BigDecimal(input.toDouble())
          else -> throw CoercingParseValueException("Expected BigDecimal but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): BigDecimal {
        if (input is String) {
          try {
            return BigDecimal(input)
          } catch (e: NumberFormatException) {
            throw CoercingParseLiteralException("Expected valid BigDecimal string but got: $input")
          }
        }
        throw CoercingParseLiteralException("Expected BigDecimal string but got: $input")
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("BigDecimal")
      .description("Custom BigDecimal scalar type")
      .coercing(coercing)
      .build()
}

object LocalDateTimeScalar {
  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  private val coercing =
    object : Coercing<LocalDateTime, String> {
      override fun serialize(dataFetcherResult: Any): String =
        when (dataFetcherResult) {
          is LocalDateTime -> dataFetcherResult.format(formatter)
          is String -> {
            try {
              LocalDateTime.parse(dataFetcherResult, formatter).format(formatter)
            } catch (e: Exception) {
              throw CoercingSerializeException("Expected LocalDateTime but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected LocalDateTime but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): LocalDateTime =
        when (input) {
          is String -> {
            try {
              LocalDateTime.parse(input, formatter)
            } catch (e: Exception) {
              throw CoercingParseValueException("Expected valid ISO datetime string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected LocalDateTime string but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): LocalDateTime {
        if (input is String) {
          try {
            return LocalDateTime.parse(input, formatter)
          } catch (e: Exception) {
            throw CoercingParseLiteralException("Expected valid ISO datetime string but got: $input")
          }
        }
        throw CoercingParseLiteralException("Expected LocalDateTime string but got: $input")
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("LocalDateTime")
      .description("Custom LocalDateTime scalar type using ISO format")
      .coercing(coercing)
      .build()
}

object UUIDScalar {
  private val coercing =
    object : Coercing<UUID, String> {
      override fun serialize(dataFetcherResult: Any): String =
        when (dataFetcherResult) {
          is UUID -> dataFetcherResult.toString()
          is String -> {
            try {
              UUID.fromString(dataFetcherResult).toString()
            } catch (e: IllegalArgumentException) {
              throw CoercingSerializeException("Expected UUID but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected UUID but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): UUID =
        when (input) {
          is UUID -> input
          is String -> {
            try {
              UUID.fromString(input)
            } catch (e: IllegalArgumentException) {
              throw CoercingParseValueException("Expected valid UUID string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected UUID but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): UUID {
        if (input is String) {
          try {
            return UUID.fromString(input)
          } catch (e: IllegalArgumentException) {
            throw CoercingParseLiteralException("Expected valid UUID string but got: $input")
          }
        }
        throw CoercingParseLiteralException("Expected UUID string but got: $input")
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("UUID")
      .description("Custom UUID scalar type")
      .coercing(coercing)
      .build()
}

object LongScalar {
  private val coercing =
    object : Coercing<Long, Any> {
      override fun serialize(dataFetcherResult: Any): Any =
        when (dataFetcherResult) {
          is Long -> dataFetcherResult.toString()
          is Int -> dataFetcherResult.toString()
          is String -> {
            try {
              dataFetcherResult
            } catch (e: NumberFormatException) {
              throw CoercingSerializeException("Expected Long but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected Long but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): Long =
        when (input) {
          is Long -> input
          is Int -> input.toLong()
          is String -> {
            try {
              input.toLong()
            } catch (e: NumberFormatException) {
              throw CoercingParseValueException("Expected valid Long string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected Long but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): Long {
        return when (input) {
          is IntValue -> input.value.toLong()
          is StringValue -> {
            try {
              input.value?.toLong() ?: throw CoercingParseLiteralException("String value is null")
            } catch (e: NumberFormatException) {
              throw CoercingParseLiteralException("Expected valid Long string but got: ${input.value}")
            }
          }
          else -> throw CoercingParseLiteralException("Expected Long value but got: $input")
        }
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("Long")
      .description("Custom Long scalar type")
      .coercing(coercing)
      .build()
}

object MapScalar {
  private val objectMapper = jacksonObjectMapper()

  private val coercing =
    object : Coercing<Map<String, Any>, Any> {
      override fun serialize(dataFetcherResult: Any): Any =
        when (dataFetcherResult) {
          is Map<*, *> -> dataFetcherResult
          is JsonNode -> {
            try {
              objectMapper.convertValue(dataFetcherResult, Map::class.java)
            } catch (e: Exception) {
              throw CoercingSerializeException("Expected Map but got invalid JsonNode: $dataFetcherResult")
            }
          }
          is String -> {
            try {
              objectMapper.readValue(dataFetcherResult, Map::class.java)
            } catch (e: Exception) {
              throw CoercingSerializeException("Expected Map but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected Map but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): Map<String, Any> =
        when (input) {
          is Map<*, *> -> input as Map<String, Any>
          is String -> {
                try {
                  objectMapper.readValue(input, Map::class.java)
                } catch (e: Exception) {
                  throw CoercingParseValueException("Expected valid Map JSON string but got: $input")
                }
          }

          else -> throw CoercingParseValueException("Expected Map but got ${input::class.simpleName}")
        } as Map<String, Any>

      override fun parseLiteral(input: Any): Map<String, Any> {
        return when (input) {
          is ObjectValue -> {
                val result = mutableMapOf<String, Any>()
                input.objectFields
                  .filterNot { it.value is NullValue  }
                  .forEach { field ->
                  result[field.name] =
                    when (val value = field.value) {
                      is StringValue -> value.value as Any
                      is IntValue -> value.value as Any
                      is FloatValue -> value.value as Any
                      is BooleanValue -> value.isValue as Any
//                      is NullValue -> null as Any?
                      else -> value.toString()
                    }
                }
                result
          }

          is StringValue -> {
                try {
                  objectMapper.readValue(input.value, Map::class.java)
                } catch (e: Exception) {
                  throw CoercingParseLiteralException("Expected valid Map JSON string but got: ${input.value}")
                }
          }

          else -> throw CoercingParseLiteralException("Expected Map value but got: $input")
        } as Map<String, Any>
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("Map")
      .description("Custom Map scalar type")
      .coercing(coercing)
      .build()
}

object JsonObjectScalar {
  private val objectMapper = jacksonObjectMapper()

  private val coercing =
    object : Coercing<ObjectNode, Any> {
      override fun serialize(dataFetcherResult: Any): ObjectNode =
        when (dataFetcherResult) {
          is ObjectNode -> dataFetcherResult
          is JsonNode -> {
                if (dataFetcherResult.isObject) {
                  dataFetcherResult as ObjectNode
                } else {
                  throw CoercingSerializeException("Expected JsonObject but got ${dataFetcherResult.nodeType}")
                }
          }

          is Map<*, *> -> {
                try {
                  objectMapper.valueToTree<ObjectNode>(dataFetcherResult)
                } catch (e: Exception) {
                  throw CoercingSerializeException("Expected JsonObject but got invalid Map: $dataFetcherResult")
                }
          }

          is String -> {
                try {
                  val jsonNode = objectMapper.readTree(dataFetcherResult)
                  if (jsonNode.isObject) {
                    jsonNode as ObjectNode
                  } else {
                    throw CoercingSerializeException("Expected JsonObject but got non-object JSON: $dataFetcherResult")
                  }
                } catch (e: Exception) {
                  throw CoercingSerializeException("Expected JsonObject but got invalid string: $dataFetcherResult")
                }
          }

          else -> throw CoercingSerializeException("Expected JsonObject but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): ObjectNode =
        when (input) {
          is ObjectNode -> input
          is JsonNode -> {
            if (input.isObject) {
              input as ObjectNode
            } else {
              throw CoercingParseValueException("Expected JsonObject but got ${input.nodeType}")
            }
          }
          is Map<*, *> -> {
            try {
              objectMapper.valueToTree<ObjectNode>(input)
            } catch (e: Exception) {
              throw CoercingParseValueException("Expected valid JsonObject but got invalid Map: $input")
            }
          }
          is String -> {
            try {
              val jsonNode = objectMapper.readTree(input)
              if (jsonNode.isObject) {
                jsonNode as ObjectNode
              } else {
                throw CoercingParseValueException("Expected JsonObject but got non-object JSON: $input")
              }
            } catch (e: Exception) {
              throw CoercingParseValueException("Expected valid JsonObject JSON string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected JsonObject but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): ObjectNode {
        return when (input) {
          is ObjectValue -> {
            try {
              val map = mutableMapOf<String, Any>()
              input.objectFields
                .filterNot { it.value is NullValue }
                .forEach { field ->
                map[field.name] =
                  when (val value = field.value) {
                    is StringValue -> value.value as Any
                    is IntValue -> value.value as Any
                    is FloatValue -> value.value as Any
                    is BooleanValue -> value.isValue as Any
//                    is NullValue -> null
                    else -> value.toString()
                  }
              }
              objectMapper.valueToTree<ObjectNode>(map)
            } catch (e: Exception) {
              throw CoercingParseLiteralException("Failed to convert ObjectValue to JsonObject: $input")
            }
          }
          is StringValue -> {
            try {
              val jsonNode = objectMapper.readTree(input.value)
              if (jsonNode.isObject) {
                jsonNode as ObjectNode
              } else {
                throw CoercingParseLiteralException("Expected JsonObject but got non-object JSON: ${input.value}")
              }
            } catch (e: Exception) {
              throw CoercingParseLiteralException("Expected valid JsonObject JSON string but got: ${input.value}")
            }
          }
          else -> throw CoercingParseLiteralException("Expected JsonObject value but got: $input")
        }
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("JsonObject")
      .description("Custom JsonObject scalar type for JSON objects")
      .coercing(coercing)
      .build()
}

object JsonArrayScalar {
  private val objectMapper = jacksonObjectMapper()

  private val coercing =
    object : Coercing<ArrayNode, Any> {
      override fun serialize(dataFetcherResult: Any): Any =
        when (dataFetcherResult) {
          is ArrayNode -> dataFetcherResult
          is JsonNode -> {
            if (dataFetcherResult.isArray) {
              dataFetcherResult
            } else {
              throw CoercingSerializeException("Expected JsonArray but got ${dataFetcherResult.nodeType}")
            }
          }
          is List<*> -> {
            try {
              objectMapper.valueToTree<ArrayNode>(dataFetcherResult)
            } catch (e: Exception) {
              throw CoercingSerializeException("Expected JsonArray but got invalid List: $dataFetcherResult")
            }
          }
          is String -> {
            try {
              val jsonNode = objectMapper.readTree(dataFetcherResult)
              if (jsonNode.isArray) {
                jsonNode as ArrayNode
              } else {
                throw CoercingSerializeException("Expected JsonArray but got non-array JSON: $dataFetcherResult")
              }
            } catch (e: Exception) {
              throw CoercingSerializeException("Expected JsonArray but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected JsonArray but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): ArrayNode =
        when (input) {
          is ArrayNode -> input
          is JsonNode -> {
            if (input.isArray) {
              input as ArrayNode
            } else {
              throw CoercingParseValueException("Expected JsonArray but got ${input.nodeType}")
            }
          }
          is List<*> -> {
            try {
              objectMapper.valueToTree<ArrayNode>(input)
            } catch (e: Exception) {
              throw CoercingParseValueException("Expected valid JsonArray but got invalid List: $input")
            }
          }
          is String -> {
            try {
              val jsonNode = objectMapper.readTree(input)
              if (jsonNode.isArray) {
                jsonNode as ArrayNode
              } else {
                throw CoercingParseValueException("Expected JsonArray but got non-array JSON: $input")
              }
            } catch (e: Exception) {
              throw CoercingParseValueException("Expected valid JsonArray JSON string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected JsonArray but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): ArrayNode {
        return when (input) {
          is ArrayValue -> {
            try {
              val list = mutableListOf<Any>()
              input.values.forEach { value ->
                list.add(
                  when (value) {
                    is StringValue -> value.value as Any
                    is IntValue -> value.value as Any
                    is FloatValue -> value.value as Any
                    is BooleanValue -> value.isValue as Any
//                    is NullValue -> null
                    else -> value.toString()
                  }
                )
              }
              objectMapper.valueToTree<ArrayNode>(list)
            } catch (e: Exception) {
              throw CoercingParseLiteralException("Failed to convert ArrayValue to JsonArray: $input")
            }
          }
          is StringValue -> {
            try {
              val jsonNode = objectMapper.readTree(input.value)
              if (jsonNode.isArray) {
                jsonNode as ArrayNode
              } else {
                throw CoercingParseLiteralException("Expected JsonArray but got non-array JSON: ${input.value}")
              }
            } catch (e: Exception) {
              throw CoercingParseLiteralException("Expected valid JsonArray JSON string but got: ${input.value}")
            }
          }
          else -> throw CoercingParseLiteralException("Expected JsonArray value but got: $input")
        }
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("JsonArray")
      .description("Custom JsonArray scalar type for JSON arrays")
      .coercing(coercing)
      .build()
}

object MoneyScalar {
  private val coercing =
    object : Coercing<Long, String> {
      override fun serialize(dataFetcherResult: Any): String =
        when (dataFetcherResult) {
          is Long -> dataFetcherResult.toString()
          is Int -> dataFetcherResult.toLong().toString()
          is String -> {
            try {
              val longValue = dataFetcherResult.toLong()
              if (longValue < 0) {
                throw CoercingSerializeException("Money value cannot be negative: $dataFetcherResult")
              }
              longValue.toString()
            } catch (e: NumberFormatException) {
              throw CoercingSerializeException("Expected Money (cents) but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected Money (cents) but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): Long =
        when (input) {
          is Long -> {
            if (input < 0) {
              throw CoercingParseValueException("Money value cannot be negative: $input")
            }
            input
          }
          is Int -> {
            if (input < 0) {
              throw CoercingParseValueException("Money value cannot be negative: $input")
            }
            input.toLong()
          }
          is String -> {
            try {
              val longValue = input.toLong()
              if (longValue < 0) {
                throw CoercingParseValueException("Money value cannot be negative: $input")
              }
              longValue
            } catch (e: NumberFormatException) {
              throw CoercingParseValueException("Expected valid Money (cents) string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected Money (cents) but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): Long {
        return when (input) {
          is IntValue -> {
            val longValue = input.value.toLong()
            if (longValue < 0) {
              throw CoercingParseLiteralException("Money value cannot be negative: $longValue")
            }
            longValue
          }
          is StringValue -> {
            try {
              val longValue = input.value?.toLong() ?: throw CoercingParseLiteralException("String value is null")
              if (longValue < 0) {
                throw CoercingParseLiteralException("Money value cannot be negative: $longValue")
              }
              longValue
            } catch (e: NumberFormatException) {
              throw CoercingParseLiteralException("Expected valid Money (cents) string but got: ${input.value}")
            }
          }
          else -> throw CoercingParseLiteralException("Expected Money (cents) value but got: $input")
        }
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("Money")
      .description("Custom Money scalar type representing monetary value in cents (分). 1 yuan = 100 cents.")
      .coercing(coercing)
      .build()
}

object PrimaryIdScalar {
  private val coercing =
    object : Coercing<Long, String> {
      override fun serialize(dataFetcherResult: Any): String =
        when (dataFetcherResult) {
          is Long -> dataFetcherResult.toString()
          is Int -> dataFetcherResult.toLong().toString()
          is String -> {
            try {
              val longValue = dataFetcherResult.toLong()
              if (longValue <= 0) {
                throw CoercingSerializeException("PrimaryId must be positive but got: $longValue")
              }
              longValue.toString()
            } catch (e: NumberFormatException) {
              throw CoercingSerializeException("Expected valid PrimaryId but got invalid string: $dataFetcherResult")
            }
          }
          else -> throw CoercingSerializeException("Expected PrimaryId but got ${dataFetcherResult::class.simpleName}")
        }

      override fun parseValue(input: Any): Long =
        when (input) {
          is Long -> {
            if (input <= 0) {
              throw CoercingParseValueException("PrimaryId must be positive but got: $input")
            }
            input
          }
          is Int -> {
            if (input <= 0) {
              throw CoercingParseValueException("PrimaryId must be positive but got: $input")
            }
            input.toLong()
          }
          is String -> {
            try {
              val longValue = input.toLong()
              if (longValue <= 0) {
                throw CoercingParseValueException("PrimaryId must be positive but got: $longValue")
              }
              longValue
            } catch (e: NumberFormatException) {
              throw CoercingParseValueException("Expected valid PrimaryId string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected PrimaryId but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): Long {
        return when (input) {
          is IntValue -> {
            val value = input.value.toLong()
            if (value <= 0) {
              throw CoercingParseLiteralException("PrimaryId must be positive but got: $value")
            }
            value
          }
          is StringValue -> {
            try {
              val longValue = input.value?.toLong() ?: throw CoercingParseLiteralException("PrimaryId string value is null")
              if (longValue <= 0) {
                throw CoercingParseLiteralException("PrimaryId must be positive but got: $longValue")
              }
              longValue
            } catch (e: NumberFormatException) {
              throw CoercingParseLiteralException("Expected valid PrimaryId string but got: ${input.value}")
            }
          }
          else -> throw CoercingParseLiteralException("Expected PrimaryId literal but got: $input")
        }
      }
    }

  val GraphQL_TYPE: GraphQLScalarType =
    GraphQLScalarType
      .newScalar()
      .name("PrimaryId")
      .description("Custom primary ID scalar type - Long in backend, string in frontend. Must be positive.")
      .coercing(coercing)
      .build()
}
