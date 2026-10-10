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

import dev.yidafu.aqua.common.graphql.generated.PageInfoVo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class PaginationUtilsTest :
  FunSpec({

    test("toPageInfo with mapper should map content and build page info") {
      val pageable = PageRequest.of(0, 2)
      val page = PageImpl(listOf("a", "b"), pageable, 5)
      val (items, pageInfo) = page.toPageInfo { it.uppercase() }
      items shouldBe listOf("A", "B")
      pageInfo.hasNext shouldBe true
      pageInfo.hasPrevious shouldBe false
      pageInfo.pageNum shouldBe 0
      pageInfo.pageSize shouldBe 2
      pageInfo.total shouldBe 5
      pageInfo.totalPages shouldBe 3
    }
    test("toPageInfo without mapper should build page info") {
      val pageable = PageRequest.of(1, 2)
      val page = PageImpl(listOf("c"), pageable, 3)
      val pageInfo = page.toPageInfo()
      pageInfo.hasNext shouldBe false
      pageInfo.hasPrevious shouldBe true
      pageInfo.pageNum shouldBe 1
      pageInfo.pageSize shouldBe 2
      pageInfo.total shouldBe 3
      pageInfo.totalPages shouldBe 2
    }
    test("emptyPageInfo should report empty page") {
      val pageInfo = emptyPageInfo()
      pageInfo.hasNext shouldBe false
      pageInfo.hasPrevious shouldBe false
      pageInfo.pageNum shouldBe 0
      pageInfo.pageSize shouldBe 20
      pageInfo.total shouldBe 0
      pageInfo.totalPages shouldBe 0
    }
    test("emptyPageInfo with pageNum should report previous") {
      val pageInfo = emptyPageInfo(pageNum = 2, pageSize = 50)
      pageInfo.hasPrevious shouldBe true
      pageInfo.pageNum shouldBe 2
      pageInfo.pageSize shouldBe 50
    }
    test("toPageInfo on empty page should report no next") {
      val pageable = PageRequest.of(0, 10)
      val page = PageImpl(emptyList<String>(), pageable, 0)
      val pageInfo = page.toPageInfo()
      pageInfo.hasNext shouldBe false
      pageInfo.total shouldBe 0
    }
    test("PageInfoVo should be data class with fields") {
      val info = PageInfoVo(hasNext = true, hasPrevious = false, pageNum = 0, pageSize = 10, total = 1, totalPages = 1)
      PageInfoVo(hasNext = true, hasPrevious = false, pageNum = 0, pageSize = 10, total = 1, totalPages = 1) shouldBe info
    }
  })
