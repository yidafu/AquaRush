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

package dev.yidafu.aqua.common.graphql.util

import dev.yidafu.aqua.common.graphql.generated.PageInfo
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaginationUtilsTest {
  @Test
  fun `toPageInfo with mapper should map content and build page info`() {
    val pageable = PageRequest.of(0, 2)
    val page = PageImpl(listOf("a", "b"), pageable, 5)
    val (items, pageInfo) = page.toPageInfo { it.uppercase() }
    assertEquals(listOf("A", "B"), items)
    assertTrue(pageInfo.hasNext)
    assertFalse(pageInfo.hasPrevious)
    assertEquals(0, pageInfo.pageNum)
    assertEquals(2, pageInfo.pageSize)
    assertEquals(5, pageInfo.total)
    assertEquals(3, pageInfo.totalPages)
  }

  @Test
  fun `toPageInfo without mapper should build page info`() {
    val pageable = PageRequest.of(1, 2)
    val page = PageImpl(listOf("c"), pageable, 3)
    val pageInfo = page.toPageInfo()
    assertFalse(pageInfo.hasNext)
    assertTrue(pageInfo.hasPrevious)
    assertEquals(1, pageInfo.pageNum)
    assertEquals(2, pageInfo.pageSize)
    assertEquals(3, pageInfo.total)
    assertEquals(2, pageInfo.totalPages)
  }

  @Test
  fun `emptyPageInfo should report empty page`() {
    val pageInfo = emptyPageInfo()
    assertFalse(pageInfo.hasNext)
    assertFalse(pageInfo.hasPrevious)
    assertEquals(0, pageInfo.pageNum)
    assertEquals(20, pageInfo.pageSize)
    assertEquals(0, pageInfo.total)
    assertEquals(0, pageInfo.totalPages)
  }

  @Test
  fun `emptyPageInfo with pageNum should report previous`() {
    val pageInfo = emptyPageInfo(pageNum = 2, pageSize = 50)
    assertTrue(pageInfo.hasPrevious)
    assertEquals(2, pageInfo.pageNum)
    assertEquals(50, pageInfo.pageSize)
  }

  @Test
  fun `toPageInfo on empty page should report no next`() {
    val pageable = PageRequest.of(0, 10)
    val page = PageImpl(emptyList<String>(), pageable, 0)
    val pageInfo = page.toPageInfo()
    assertFalse(pageInfo.hasNext)
    assertEquals(0, pageInfo.total)
  }

  @Test
  fun `PageInfo should be data class with fields`() {
    val info = PageInfo(hasNext = true, hasPrevious = false, pageNum = 0, pageSize = 10, total = 1, totalPages = 1)
    assertEquals(info, PageInfo(hasNext = true, hasPrevious = false, pageNum = 0, pageSize = 10, total = 1, totalPages = 1))
  }
}
