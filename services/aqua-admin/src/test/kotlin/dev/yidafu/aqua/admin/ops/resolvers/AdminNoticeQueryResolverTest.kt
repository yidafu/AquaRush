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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminNoticeQueryResolverTest {
  private lateinit var noticeQueryService: NoticeQueryService
  private lateinit var resolver: AdminNoticeQueryResolver

  private fun notice(): NoticeModel =
    NoticeModel(
      id = 1L,
      title = "系统维护通知",
      content = "本周六凌晨维护",
      type = NoticeType.PLATFORM_NOTICE,
      status = NoticeStatus.PUBLISHED,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    noticeQueryService = mockk(relaxed = true)
    resolver = AdminNoticeQueryResolver(noticeQueryService)
  }

  @Test
  fun `notices should list with default request`() {
    // Given
    every { noticeQueryService.listNotices(any()) } returns PageImpl(listOf(notice()))

    // When
    val result = resolver.notices(null)

    // Then
    assertEquals(1, result.content.size)
    assertEquals("系统维护通知", result.content[0].title)
    verify { noticeQueryService.listNotices(any()) }
  }

  @Test
  fun `notice should return null when missing`() {
    // Given
    every { noticeQueryService.getNotice(999L) } returns null

    // When
    val result = resolver.notice(999L)

    // Then
    assertNull(result)
  }

  @Test
  fun `notice should map by id`() {
    // Given
    every { noticeQueryService.getNotice(1L) } returns notice()

    // When
    val result = resolver.notice(1L)

    // Then
    assertEquals("系统维护通知", result!!.title)
    verify { noticeQueryService.getNotice(1L) }
  }
}
