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

package dev.yidafu.aqua.ops.feedback.service.impl

import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 用户端我的-反馈记录查询服务单元测试(F-USR-005 处理结果回传)
 * 验证 userId 透传、分页参数收敛、按提交时间倒序。
 */
class FeedbackQueryApiServiceImplTest {
  private lateinit var feedbackRepository: FeedbackRepository
  private lateinit var service: FeedbackQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    feedbackRepository = mockk(relaxed = true)
    service = FeedbackQueryApiServiceImpl(feedbackRepository)
  }

  @Test
  @DisplayName("findMyFeedbacks 按登录用户分页查询并按提交时间倒序")
  fun `findMyFeedbacks queries by userId with createdAt desc paging`() {
    val feedback =
      FeedbackModel(
        id = 1L,
        userId = 100L,
        feedbackType = FeedbackType.SUGGESTION,
        content = "希望增加夜间模式",
        createdAt = LocalDateTime.of(2026, 8, 13, 9, 0),
      )
    every { feedbackRepository.findByUserId(any(), any()) } returns PageImpl(listOf(feedback))

    val result = service.findMyFeedbacks(userId = 100L, page = 1, size = 10)

    assertEquals(1, result.content.size)
    val pageableSlot = slot<PageRequest>()
    verify {
      feedbackRepository.findByUserId(userId = 100L, pageable = capture(pageableSlot))
    }
    assertEquals(1, pageableSlot.captured.pageNumber)
    assertEquals(10, pageableSlot.captured.pageSize)
    val createdAtSortOrder = pageableSlot.captured.sort.getOrderFor("createdAt")
    assertEquals(Sort.Direction.DESC, createdAtSortOrder?.direction)
  }

  @Test
  @DisplayName("findMyFeedbacks 收敛非法分页参数(page<0 归零,size 限制 1~50)")
  fun `findMyFeedbacks clamps paging params`() {
    every { feedbackRepository.findByUserId(any(), any()) } returns PageImpl(emptyList())
    val pageableSlot = slot<PageRequest>()

    service.findMyFeedbacks(userId = 100L, page = -5, size = 999)

    verify { feedbackRepository.findByUserId(any(), capture(pageableSlot)) }
    assertEquals(0, pageableSlot.captured.pageNumber)
    assertEquals(50, pageableSlot.captured.pageSize)
  }
}
