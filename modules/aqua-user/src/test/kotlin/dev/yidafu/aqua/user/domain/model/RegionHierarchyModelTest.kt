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
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class RegionHierarchyModelTest {
  private fun region(
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

  @Test
  fun `RegionHierarchyModel should hold default selections and options`() {
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

    assertEquals(province, hierarchy.province)
    assertEquals(city, hierarchy.city)
    assertEquals(district, hierarchy.district)
    assertEquals(1, hierarchy.provinces.size)
    assertEquals(1, hierarchy.cities.size)
    assertEquals(1, hierarchy.districts.size)
  }

  @Test
  fun `RegionHierarchyModel should be data class with equality`() {
    val p = region(1L, "广东省", "440000")
    val c = region(2L, "深圳市", "440300")
    val d = region(3L, "南山区", "440305")
    val a = RegionHierarchyModel(p, c, d, listOf(p), listOf(c), listOf(d))
    val b = RegionHierarchyModel(p, c, d, listOf(p), listOf(c), listOf(d))
    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
  }
}
