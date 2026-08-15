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

package dev.yidafu.aqua.common.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonHelperTest {
  private val jsonHelper = JsonHelper()

  @Test
  fun `createArrayNode should create empty array node`() {
    val node = jsonHelper.createArrayNode()
    assertTrue(node.isEmpty)
  }

  @Test
  fun `createArrayNode from strings should add all strings`() {
    val node = jsonHelper.createArrayNode(listOf("a", "b", "c"))
    assertEquals(3, node.size())
    assertEquals("a", node[0].asText())
    assertEquals("c", node[2].asText())
  }

  @Test
  fun `createObjectNode should create empty object node`() {
    val node = jsonHelper.createObjectNode()
    assertTrue(node.isEmpty)
  }

  @Test
  fun `createObjectNode from map should populate fields`() {
    val node = jsonHelper.createObjectNode(mapOf("name" to "test", "age" to 30))
    assertEquals("test", node.get("name").asText())
    assertEquals(30, node.get("age").asInt())
  }

  @Test
  fun `arrayNodeToList should convert textual values`() {
    val node = jsonHelper.createArrayNode(listOf("x", "y"))
    val list = jsonHelper.arrayNodeToList(node)
    assertEquals(listOf("x", "y"), list)
  }

  @Test
  fun `arrayNodeToList should ignore non-textual values`() {
    val node = jsonHelper.createArrayNode()
    node.add(123)
    node.add("text")
    val list = jsonHelper.arrayNodeToList(node)
    assertEquals(listOf("text"), list)
  }

  @Test
  fun `arrayNodeToList should return empty list for null`() {
    assertEquals(emptyList<String>(), jsonHelper.arrayNodeToList(null))
  }

  @Test
  fun `arrayNodeToList typed should convert to target type`() {
    data class Sample(
      val name: String,
    )
    val node = jsonHelper.createArrayNode()
    node.addObject().put("name", "hello")
    val list = jsonHelper.arrayNodeToList(node, Sample::class.java)
    assertEquals(1, list.size)
    assertEquals("hello", list[0].name)
  }

  @Test
  fun `arrayNodeToList typed should return empty list for null`() {
    assertEquals(emptyList<String>(), jsonHelper.arrayNodeToList(null, String::class.java))
  }

  @Test
  fun `objectNodeToMap should convert object node`() {
    val node = jsonHelper.createObjectNode(mapOf("key" to "value", "num" to 1))
    val map = jsonHelper.objectNodeToMap(node)
    assertEquals("value", map["key"])
    assertEquals(1, map["num"])
  }

  @Test
  fun `objectNodeToMap should return empty map for null`() {
    assertEquals(emptyMap<String, Any>(), jsonHelper.objectNodeToMap(null))
  }

  @Test
  fun `getObjectMapper should return configured mapper`() {
    assertNotNull(jsonHelper.getObjectMapper())
  }
}
