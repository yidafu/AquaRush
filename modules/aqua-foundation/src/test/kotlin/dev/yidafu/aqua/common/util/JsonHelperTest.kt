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

package dev.yidafu.aqua.common.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class JsonHelperTest :
  FunSpec({
    val jsonHelper = JsonHelper()

    test("createArrayNode should create empty array node") {
      val node = jsonHelper.createArrayNode()
      node.isEmpty shouldBe true
    }
    test("createArrayNode from strings should add all strings") {
      val node = jsonHelper.createArrayNode(listOf("a", "b", "c"))
      node.size() shouldBe 3
      node[0].asText() shouldBe "a"
      node[2].asText() shouldBe "c"
    }
    test("createObjectNode should create empty object node") {
      val node = jsonHelper.createObjectNode()
      node.isEmpty shouldBe true
    }
    test("createObjectNode from map should populate fields") {
      val node = jsonHelper.createObjectNode(mapOf("name" to "test", "age" to 30))
      node.get("name").asText() shouldBe "test"
      node.get("age").asInt() shouldBe 30
    }
    test("arrayNodeToList should convert textual values") {
      val node = jsonHelper.createArrayNode(listOf("x", "y"))
      val list = jsonHelper.arrayNodeToList(node)
      list shouldBe listOf("x", "y")
    }
    test("arrayNodeToList should ignore non-textual values") {
      val node = jsonHelper.createArrayNode()
      node.add(123)
      node.add("text")
      val list = jsonHelper.arrayNodeToList(node)
      list shouldBe listOf("text")
    }
    test("arrayNodeToList should return empty list for null") {
      jsonHelper.arrayNodeToList(null) shouldBe (emptyList<String>())
    }
    test("arrayNodeToList typed should convert to target type") {
      data class Sample(
        val name: String,
      )
      val node = jsonHelper.createArrayNode()
      node.addObject().put("name", "hello")
      val list = jsonHelper.arrayNodeToList(node, Sample::class.java)
      list.size shouldBe 1
      list[0].name shouldBe "hello"
    }
    test("arrayNodeToList typed should return empty list for null") {
      jsonHelper.arrayNodeToList(null, String::class.java) shouldBe (emptyList<String>())
    }
    test("objectNodeToMap should convert object node") {
      val node = jsonHelper.createObjectNode(mapOf("key" to "value", "num" to 1))
      val map = jsonHelper.objectNodeToMap(node)
      map["key"] shouldBe "value"
      map["num"] shouldBe 1
    }
    test("objectNodeToMap should return empty map for null") {
      jsonHelper.objectNodeToMap(null) shouldBe (emptyMap<String, Any>())
    }
    test("getObjectMapper should return configured mapper") {
      jsonHelper.getObjectMapper().shouldNotBeNull()
    }
  })
