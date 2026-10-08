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

package dev.yidafu.aqua.user.domain.model

import dev.yidafu.aqua.common.domain.model.RegionModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class RegionHierarchyModelTest :
  FunSpec({
    fun region(
      id: Long,
      name: String,
      code: String,
    ): RegionModel =
      RegionModel(
        id = id,
        name = name,
        code = code,
        parentCode = null,
        level = 1,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    test("RegionHierarchyModel should hold default selections and options") {
      val province = region(1L, "广东省", "440000")
      val city = region(2L, "深圳市", "440300")
      val district = region(3L, "南山区", "440305")

      val hierarchy =
        RegionHierarchyModel(
          province = province,
          city = city,
          district = district,
          provinces = listOf(province),
          cities = listOf(city),
          districts = listOf(district),
        )

      hierarchy.province shouldBe province
      hierarchy.city shouldBe city
      hierarchy.district shouldBe district
      hierarchy.provinces.size shouldBe 1
      hierarchy.cities.size shouldBe 1
      hierarchy.districts.size shouldBe 1
    }
    test("RegionHierarchyModel should be data class with equality") {
      val p = region(1L, "广东省", "440000")
      val c = region(2L, "深圳市", "440300")
      val d = region(3L, "南山区", "440305")
      val a = RegionHierarchyModel(p, c, d, listOf(p), listOf(c), listOf(d))
      val b = RegionHierarchyModel(p, c, d, listOf(p), listOf(c), listOf(d))
      b shouldBe a
      b.hashCode() shouldBe a.hashCode()
    }
  })
