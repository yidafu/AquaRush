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

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.databind.ObjectMapper

/**
 * String-based JSON converter for simple String to JSONB conversion
 *
 * This converter is specifically designed for cases where the entity
 * field is a String that needs to be stored in a JSONB column.
 */
@Converter(autoApply = false)
class StringJsonConverter : AttributeConverter<String?, String?> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData
    }
}

/**
 * List<String> converter for JSON arrays stored in JSONB columns
 */
@Converter(autoApply = false)
class StringListJsonConverter : AttributeConverter<List<String>?, String?> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        return try {
            if (attribute == null) {
                null
            } else {
                objectMapper.writeValueAsString(attribute)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting list to JSON string", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<String>? {
        return try {
            if (dbData == null) {
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(dbData, List::class.java) as List<String>
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting JSON string to list", e)
        }
    }
}

/**
 * Map<String, Any> converter for JSON objects stored in JSONB columns
 */
@Converter(autoApply = false)
class JsonMapConverter : AttributeConverter<Map<String, Any>?, String?> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Any>?): String? {
        return try {
            if (attribute == null) {
                null
            } else {
                objectMapper.writeValueAsString(attribute)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting map to JSON string", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any>? {
        return try {
            if (dbData == null) {
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(dbData, Map::class.java) as Map<String, Any>
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting JSON string to map", e)
        }
    }
}