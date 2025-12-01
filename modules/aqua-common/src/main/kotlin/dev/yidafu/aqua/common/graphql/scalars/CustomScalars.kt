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
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
          is Long -> dataFetcherResult
          is Int -> dataFetcherResult.toLong()
          is String -> {
            try {
              dataFetcherResult.toLong()
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
  private val coercing =
    object : Coercing<Map<String, Any>, Any> {
      override fun serialize(dataFetcherResult: Any): Any =
        when (dataFetcherResult) {
          is Map<*, *> -> dataFetcherResult
          is String -> {
            try {
              // Simple JSON string parsing - in a real implementation, you might want to use a proper JSON parser
              // For now, just return the string as-is
              dataFetcherResult
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
              // Simple JSON string parsing - in a real implementation, you might want to use a proper JSON parser
              // For now, return empty map for strings
              emptyMap()
            } catch (e: Exception) {
              throw CoercingParseValueException("Expected valid Map string but got: $input")
            }
          }
          else -> throw CoercingParseValueException("Expected Map but got ${input::class.simpleName}")
        }

      override fun parseLiteral(input: Any): Map<String, Any> {
        return when (input) {
          is ObjectValue -> {
            val result = mutableMapOf<String, Any>()
            input.objectFields.forEach { field ->
              result[field.name] =
                when (val value = field.value) {
                  is StringValue -> value.value as Any
                  is IntValue -> value.value as Any
                  else -> value.toString()
                }
            }
            result
          }
          is StringValue -> {
            try {
              // Simple JSON string parsing
              emptyMap()
            } catch (e: Exception) {
              throw CoercingParseLiteralException("Expected valid Map string but got: ${input.value}")
            }
          }
          else -> throw CoercingParseLiteralException("Expected Map value but got: $input")
        }
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
