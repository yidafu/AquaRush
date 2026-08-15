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

import dev.yidafu.aqua.api.service.ops.FeedbackMutationService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.common.graphql.generated.FeedbackReviewInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import dev.yidafu.aqua.common.graphql.generated.FeedbackStatus as GraphQLFeedbackStatus

class AdminFeedbackMutationResolverTest {
  private lateinit var feedbackMutationService: FeedbackMutationService
  private lateinit var resolver: AdminFeedbackMutationResolver

  private fun feedback(): FeedbackModel =
    FeedbackModel(
      id = 1L,
      userId = 1L,
      feedbackType = FeedbackType.OTHER,
      content = "建议增加送水时段",
      status = FeedbackStatus.RESOLVED,
      handleResult = "已处理",
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "admin",
      userType = "ADMIN",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
    )

  @BeforeEach
  fun setUp() {
    feedbackMutationService = mockk(relaxed = true)
    resolver = AdminFeedbackMutationResolver(feedbackMutationService)
  }

  @Test
  fun `reviewFeedback should delegate`() {
    // Given
    every { feedbackMutationService.reviewFeedback(1L, "admin", 1L, any()) } returns feedback()
    val input = FeedbackReviewInput(status = GraphQLFeedbackStatus.RESOLVED, handleResult = "已处理")

    // When
    val result = resolver.reviewFeedback(1L, input, principal())

    // Then
    assertEquals(FeedbackStatus.RESOLVED, FeedbackStatus.valueOf(result.status.name))
    verify { feedbackMutationService.reviewFeedback(1L, "admin", 1L, any()) }
  }
}
