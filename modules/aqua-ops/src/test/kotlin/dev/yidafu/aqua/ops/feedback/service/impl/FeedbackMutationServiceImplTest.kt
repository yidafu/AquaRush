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

import dev.yidafu.aqua.api.dto.FeedbackReviewRequest
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.feedback.domain.exception.InvalidFeedbackStatusTransitionException
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 意见反馈处理服务单元测试(F-OPS-003 核心规则:状态前进流转、处理人/时间留痕、审计)
 */
class FeedbackMutationServiceImplTest {
  private lateinit var feedbackRepository: FeedbackRepository
  private lateinit var opsConfigAuditService: OpsConfigAuditService
  private lateinit var service: FeedbackMutationServiceImpl

  private fun feedback(
    id: Long = 1L,
    status: FeedbackStatus = FeedbackStatus.PENDING,
  ): FeedbackModel =
    FeedbackModel(
      id = id,
      userId = 100L,
      feedbackType = FeedbackType.PRODUCT_QUALITY,
      content = "水质发黄,请尽快处理",
      status = status,
    )

  @BeforeEach
  fun setUp() {
    feedbackRepository = mockk(relaxed = true)
    opsConfigAuditService = mockk(relaxed = true)
    service = FeedbackMutationServiceImpl(feedbackRepository, opsConfigAuditService)
  }

  @Test
  @DisplayName("reviewFeedback 待处理 → 已处理,处理人/时间留痕")
  fun `reviewFeedback resolves feedback with handler info`() {
    val existing = feedback()
    every { feedbackRepository.findById(1L) } returns Optional.of(existing)
    every { feedbackRepository.save(any()) } answers { firstArg() }

    val result = service.reviewFeedback(1L, "admin", 1L, FeedbackReviewRequest("已联系用户上门检测", FeedbackStatus.RESOLVED))

    assertEquals(FeedbackStatus.RESOLVED, result.status)
    assertEquals("已联系用户上门检测", result.handleResult)
    assertEquals(1L, result.handledById)
    assertEquals("admin", result.handledByName)
    assertNotNull(result.handledAt)
  }

  @Test
  @DisplayName("reviewFeedback 已处理 → 待处理 抛 InvalidFeedbackStatusTransitionException")
  fun `reviewFeedback rejects backward transition`() {
    val existing = feedback(status = FeedbackStatus.RESOLVED)
    every { feedbackRepository.findById(1L) } returns Optional.of(existing)

    assertFailsWith<InvalidFeedbackStatusTransitionException> {
      service.reviewFeedback(1L, "admin", 1L, FeedbackReviewRequest(null, FeedbackStatus.PENDING))
    }
  }

  @Test
  @DisplayName("reviewFeedback 同级重设允许(处理中 → 处理中 补填处理结果)")
  fun `reviewFeedback allows same status`() {
    val existing = feedback(status = FeedbackStatus.PROCESSING)
    every { feedbackRepository.findById(1L) } returns Optional.of(existing)
    every { feedbackRepository.save(any()) } answers { firstArg() }

    val result = service.reviewFeedback(1L, "admin", 1L, FeedbackReviewRequest("补充处理说明", FeedbackStatus.PROCESSING))

    assertEquals(FeedbackStatus.PROCESSING, result.status)
    assertEquals("补充处理说明", result.handleResult)
  }

  @Test
  @DisplayName("reviewFeedback 落审计日志(PROCESS),含前后值")
  fun `reviewFeedback writes audit`() {
    val existing = feedback()
    every { feedbackRepository.findById(1L) } returns Optional.of(existing)
    every { feedbackRepository.save(any()) } answers { firstArg() }

    service.reviewFeedback(1L, "admin", 1L, FeedbackReviewRequest("已处理", FeedbackStatus.RESOLVED))

    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_FEEDBACK,
        1L,
        OpsConfigAuditService.ACTION_FEEDBACK_PROCESS,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("反馈不存在抛异常")
  fun `reviewFeedback throws when feedback not found`() {
    every { feedbackRepository.findById(1L) } returns Optional.empty()

    assertFailsWith<RuntimeException> {
      service.reviewFeedback(1L, "admin", 1L, FeedbackReviewRequest(null, FeedbackStatus.PROCESSING))
    }
  }

  @Test
  @DisplayName("反馈提交图片数组写入实体")
  fun `submitFeedback stores images`() {
    every { feedbackRepository.save(any()) } answers { firstArg() }
    val apiService = FeedbackMutationApiServiceImpl(feedbackRepository)

    val created =
      apiService.submitFeedback(
        100L,
        dev.yidafu.aqua.api.dto.FeedbackSubmitRequest(
          FeedbackType.SUGGESTION,
          "希望增加小程序夜间模式",
          listOf("http://x/1.png", "http://x/2.png"),
          "13800138000",
        ),
      )

    assertEquals(FeedbackStatus.PENDING, created.status)
    assertEquals(listOf("http://x/1.png", "http://x/2.png"), created.getImagesAsList())
    assertTrue(created.images != null)
  }
}
