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

package dev.yidafu.aqua.common.converter

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.postgresql.util.PGBinaryObject
import org.postgresql.util.PGobject
import tools.jackson.core.JacksonException
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

/**
 * Enhanced JSON converters using Jackson for proper JsonNode handling
 */
object EnhancedJsonConverters {
  private val objectMapper = jacksonObjectMapper()
}

/**
 * Converter for ObjectNode (JSON objects) storage
 */
@Converter(autoApply = false)
class ObjectNodeConverter : AttributeConverter<ObjectNode?, String?> {

  private val objectMapper = jacksonObjectMapper()

  override fun convertToDatabaseColumn(attribute: ObjectNode?): String? {
    return if (attribute == null) {
      null
    } else {
      try {
          objectMapper.writeValueAsString(attribute)
      } catch (e: JacksonException) {
        throw IllegalArgumentException("Error converting ObjectNode to JSON string", e)
      }
    }
  }

  override fun convertToEntityAttribute(dbData: String?): ObjectNode? {
    return if (dbData == null || dbData.trim().isEmpty()) {
      null
    } else {
      try {
        val jsonNode = objectMapper.readTree(dbData)
        if (jsonNode.isObject) {
          jsonNode as ObjectNode
        } else {
          throw IllegalArgumentException("Database data is not a valid JSON object: $dbData")
        }
      } catch (e: JacksonException) {
        throw IllegalArgumentException("Error parsing JSON string to ObjectNode: $dbData", e)
      }
    }
  }
}

/**
 * Converter for ArrayNode (JSON arrays) storage
 */
@Converter(autoApply = false)
class ArrayNodeConverter : AttributeConverter<ArrayNode?, String?> {

  private val objectMapper = jacksonObjectMapper()

  override fun convertToDatabaseColumn(attribute: ArrayNode?): String? {
    return if (attribute == null) {
      null
    } else {
      try {
          objectMapper.writeValueAsString(attribute)
      } catch (e: JacksonException) {
        throw IllegalArgumentException("Error converting ArrayNode to JSON string", e)
      }
    }
  }

  override fun convertToEntityAttribute(dbData: String?): ArrayNode? {
    return if (dbData == null || dbData.trim().isEmpty()) {
      null
    } else {
      try {
        val jsonNode = objectMapper.readTree(dbData)
        if (jsonNode.isArray) {
          jsonNode as ArrayNode
        } else {
          throw IllegalArgumentException("Database data is not a valid JSON array: $dbData")
        }
      } catch (e: JacksonException) {
        throw IllegalArgumentException("Error parsing JSON string to ArrayNode: $dbData", e)
      }
    }
  }
}

/**
 * Converter for generic JsonNode storage (can be object, array, or primitive)
 */
@Converter(autoApply = false)
class JsonNodeConverter : AttributeConverter<JsonNode?, String?> {

  private val objectMapper = jacksonObjectMapper()

  override fun convertToDatabaseColumn(attribute: JsonNode?): String? {
    return if (attribute == null) {
      null
    } else {
      try {
        objectMapper.writeValueAsString(attribute)
      } catch (e: JacksonException) {
        throw IllegalArgumentException("Error converting JsonNode to JSON string", e)
      }
    }
  }

  override fun convertToEntityAttribute(dbData: String?): JsonNode? {
    return (if (dbData == null || dbData.trim().isEmpty()) {
      null
    } else {
      try {
        objectMapper.readTree(dbData) as JsonNode?
      } catch (e: JacksonException) {
        throw IllegalArgumentException("Error parsing JSON string to JsonNode: $dbData", e)
      }
    })
  }
}
