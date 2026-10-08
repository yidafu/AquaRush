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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.NoticeQueryService
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime

class AdminNoticeQueryResolverTest :
  FunSpec({
    var noticeQueryService: NoticeQueryService = mockk()
    var resolver: AdminNoticeQueryResolver = mockk()

    fun notice(): NoticeModel =
      NoticeModel(
        id = 1L,
        title = "系统维护通知",
        content = "本周六凌晨维护",
        type = NoticeType.PLATFORM_NOTICE,
        status = NoticeStatus.PUBLISHED,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      noticeQueryService = mockk(relaxed = true)
      resolver = AdminNoticeQueryResolver(noticeQueryService)
    }
    test("notices should list with default request") {
      // Given
      every { noticeQueryService.listNotices(any()) } returns PageImpl(listOf(notice()))

      // When
      val result = resolver.notices(null)

      // Then
      result.content.size shouldBe 1
      result.content[0].title shouldBe "系统维护通知"
      verify { noticeQueryService.listNotices(any()) }
    }
    test("notice should return null when missing") {
      // Given
      every { noticeQueryService.getNotice(999L) } returns null

      // When
      val result = resolver.notice(999L)

      // Then
      result.shouldBeNull()
    }
    test("notice should map by id") {
      // Given
      every { noticeQueryService.getNotice(1L) } returns notice()

      // When
      val result = resolver.notice(1L)

      // Then
      result!!.title shouldBe "系统维护通知"
      verify { noticeQueryService.getNotice(1L) }
    }
  })
