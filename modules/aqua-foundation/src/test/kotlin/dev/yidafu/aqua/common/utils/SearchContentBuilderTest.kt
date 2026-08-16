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

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class SearchContentBuilderTest :
  FunSpec({
    fun address(
      detail: String? = "深圳市南山区科技园",
      receiver: String? = "张三",
      phone: String? = "13800138000",
    ): AddressModel =
      AddressModel(
        id = 1L,
        userId = 1L,
        receiverName = receiver ?: "",
        phone = phone ?: "",
        province = "广东省",
        city = "深圳市",
        district = "南山区",
        detailAddress = detail ?: "",
        isDefault = true,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun product(name: String?): ProductModel =
      ProductModel(
        id = 1L,
        name = name ?: "",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("buildSearchContent should join address and product parts") {
      val result =
        SearchContentBuilder.buildSearchContent(
          address("深圳市南山区科技园", "张三", "13800138000"),
          listOf(product("农夫山泉"), product("怡宝")),
        )
      result shouldBe "深圳市南山区科技园 张三 13800138000 农夫山泉 怡宝"
    }
    test("buildSearchContent should skip null address fields") {
      val result =
        SearchContentBuilder.buildSearchContent(
          address(detail = null, receiver = null, phone = null),
          listOf(product("娃哈哈")),
        )
      result.contains("娃哈哈") shouldBe true
    }
    test("buildSearchContent should handle null address") {
      val result = SearchContentBuilder.buildSearchContent(null, listOf(product("可口可乐")))
      result shouldBe "可口可乐"
    }
    test("buildSearchContent should handle null product names") {
      val result = SearchContentBuilder.buildSearchContent(address(), listOf(product(null)))
      result.startsWith("深圳市南山区科技园 张三 13800138000") shouldBe true
    }
    test("buildSearchContent should return empty string for all null inputs") {
      val result = SearchContentBuilder.buildSearchContent(null, emptyList())
      result shouldBe ""
    }
    test("buildSearchContent with primitives should join non-null parts") {
      val result = SearchContentBuilder.buildSearchContent("地址", "李四", "13900139000", "产品A")
      result shouldBe "地址 李四 13900139000 产品A"
    }
    test("buildSearchContent with primitives should skip null parts") {
      val result = SearchContentBuilder.buildSearchContent(null, "李四", null, "产品A")
      result shouldBe "李四 产品A"
    }
    test("buildSearchContent with primitives should return empty for all null") {
      val result = SearchContentBuilder.buildSearchContent(null, null, null, null)
      result shouldBe ""
    }
  })
