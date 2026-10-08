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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

/**
 * 用户端我的-反馈记录查询服务单元测试(F-USR-005 处理结果回传)
 * 验证 userId 透传、分页参数收敛、按提交时间倒序。
 */
class FeedbackQueryApiServiceImplTest :
  FunSpec({
    var feedbackRepository: FeedbackRepository = mockk()
    var service: FeedbackQueryApiServiceImpl = mockk()

    beforeTest {
      feedbackRepository = mockk(relaxed = true)
      service = FeedbackQueryApiServiceImpl(feedbackRepository)
    }
    test("findMyFeedbacks queries by userId with createdAt desc paging") {
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

      result.content.size shouldBe 1
      val pageableSlot = slot<PageRequest>()
      verify {
        feedbackRepository.findByUserId(userId = 100L, pageable = capture(pageableSlot))
      }
      pageableSlot.captured.pageNumber shouldBe 1
      pageableSlot.captured.pageSize shouldBe 10
      val createdAtSortOrder = pageableSlot.captured.sort.getOrderFor("createdAt")
      createdAtSortOrder?.direction shouldBe Sort.Direction.DESC
    }
    test("findMyFeedbacks clamps paging params") {
      every { feedbackRepository.findByUserId(any(), any()) } returns PageImpl(emptyList())
      val pageableSlot = slot<PageRequest>()

      service.findMyFeedbacks(userId = 100L, page = -5, size = 999)

      verify { feedbackRepository.findByUserId(any(), capture(pageableSlot)) }
      pageableSlot.captured.pageNumber shouldBe 0
      pageableSlot.captured.pageSize shouldBe 50
    }
  })
