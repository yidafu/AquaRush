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

import dev.yidafu.aqua.api.service.ops.FeedbackQueryService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminFeedbackQueryResolverTest {
  private lateinit var feedbackQueryService: FeedbackQueryService
  private lateinit var resolver: AdminFeedbackQueryResolver

  private fun feedback(): FeedbackModel =
    FeedbackModel(
      id = 1L,
      userId = 1L,
      feedbackType = FeedbackType.OTHER,
      content = "建议增加送水时段",
      status = FeedbackStatus.PENDING,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    feedbackQueryService = mockk(relaxed = true)
    resolver = AdminFeedbackQueryResolver(feedbackQueryService)
  }

  @Test
  fun `feedbacks should list with default request`() {
    // Given
    every { feedbackQueryService.listFeedbacks(any()) } returns PageImpl(listOf(feedback()))

    // When
    val result = resolver.feedbacks(null)

    // Then
    assertEquals(1, result.content.size)
    assertEquals("建议增加送水时段", result.content[0].content)
    verify { feedbackQueryService.listFeedbacks(any()) }
  }

  @Test
  fun `feedback should return null when missing`() {
    // Given
    every { feedbackQueryService.getFeedback(999L) } returns null

    // When
    val result = resolver.feedback(999L)

    // Then
    assertNull(result)
  }

  @Test
  fun `feedback should map by id`() {
    // Given
    every { feedbackQueryService.getFeedback(1L) } returns feedback()

    // When
    val result = resolver.feedback(1L)

    // Then
    assertEquals(1L, result!!.id)
    verify { feedbackQueryService.getFeedback(1L) }
  }
}
