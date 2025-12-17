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

package dev.yidafu.aqua.common.util

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.core.type.TypeReference
import org.springframework.stereotype.Component

/**
 * Utility class for creating and manipulating JSON nodes (ArrayNode and ObjectNode)
 * Provides convenient methods for working with Jackson JSON types in a Spring application
 */
@Component
class JsonHelper {

  private val objectMapper: ObjectMapper = jacksonObjectMapper()

  /**
   * Create an empty ArrayNode
   */
  fun createArrayNode(): ArrayNode = objectMapper.createArrayNode()

  /**
   * Create an ArrayNode from a list of strings
   */
  fun createArrayNode(strings: List<String>): ArrayNode {
    val array = objectMapper.createArrayNode()
    strings.forEach { array.add(it) }
    return array
  }

  /**
   * Create an empty ObjectNode
   */
  fun createObjectNode(): ObjectNode = objectMapper.createObjectNode()

  /**
   * Create an ObjectNode from a map
   */
  fun createObjectNode(map: Map<String, Any>): ObjectNode {
    return objectMapper.valueToTree(map) as ObjectNode
  }

  /**
   * Convert ArrayNode to list of strings
   */
  fun arrayNodeToList(arrayNode: ArrayNode?): List<String> {
    return arrayNode?.mapNotNull { if (it.isTextual) it.asText() else null } ?: emptyList()
  }

  /**
   * Convert ArrayNode to list of objects of specified type
   */
  fun <T> arrayNodeToList(arrayNode: ArrayNode?, clazz: Class<T>): List<T> {
    return if (arrayNode == null) emptyList() else {
      val javaType = objectMapper.typeFactory.constructCollectionType(List::class.java, clazz)
      objectMapper.convertValue(arrayNode, javaType)
    }
  }

  /**
   * Convert ObjectNode to Map
   */
  fun objectNodeToMap(objectNode: ObjectNode?): Map<String, Any> {
    return objectNode?.let {
      objectMapper.convertValue(it, object : TypeReference<Map<String, Any>>() {})
    } ?: emptyMap()
  }

  /**
   * Get the underlying ObjectMapper for custom operations
   */
  fun getObjectMapper(): ObjectMapper = objectMapper
}