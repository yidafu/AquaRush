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

package dev.yidafu.aqua.client.ops.resolvers

import dev.yidafu.aqua.api.service.ops.FeedbackMutationApiService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.graphql.generated.FeedbackSubmitInput
import dev.yidafu.aqua.common.graphql.generated.FeedbackType
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 用户端意见反馈提交 Resolver 测试(F-USR-005:submitFeedback 透传 userId)
 */
@DisplayName("ClientFeedbackMutationResolver Tests")
class ClientFeedbackMutationResolverTest {
  private lateinit var feedbackMutationApiService: FeedbackMutationApiService
  private lateinit var resolver: ClientFeedbackMutationResolver
  private lateinit var userPrincipal: UserPrincipal

  @BeforeEach
  fun setUp() {
    feedbackMutationApiService = mockk(relaxed = true)
    resolver = ClientFeedbackMutationResolver(feedbackMutationApiService)
    userPrincipal =
      UserPrincipal(
        id = 100L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  @Test
  @DisplayName("submitFeedback should pass userId and mapped input to service")
  fun `submitFeedback passes userId and input`() {
    every { feedbackMutationApiService.submitFeedback(any(), any()) } returns FeedbackModel()
    val requestSlot = slot<dev.yidafu.aqua.api.dto.FeedbackSubmitRequest>()

    val input =
      FeedbackSubmitInput(
        feedbackType = FeedbackType.SUGGESTION,
        content = "希望增加小程序夜间模式",
        images = listOf("http://x/1.png"),
        contactInfo = "13800138000",
      )

    val result = resolver.submitFeedback(input, userPrincipal)

    assertTrue(result)
    verify {
      feedbackMutationApiService.submitFeedback(
        userId = 100L,
        request = capture(requestSlot),
      )
    }
    assertEquals(dev.yidafu.aqua.common.domain.model.enums.FeedbackType.SUGGESTION, requestSlot.captured.feedbackType)
    assertEquals(listOf("http://x/1.png"), requestSlot.captured.images)
    assertEquals("13800138000", requestSlot.captured.contactInfo)
  }

  @Test
  @DisplayName("submitFeedback without images maps to empty list")
  fun `submitFeedback handles empty images`() {
    every { feedbackMutationApiService.submitFeedback(any(), any()) } returns FeedbackModel()
    val requestSlot = slot<dev.yidafu.aqua.api.dto.FeedbackSubmitRequest>()

    val input = FeedbackSubmitInput(feedbackType = FeedbackType.COMPLAINT, content = "配送服务问题反馈内容")

    resolver.submitFeedback(input, userPrincipal)

    verify { feedbackMutationApiService.submitFeedback(100L, capture(requestSlot)) }
    assertTrue(requestSlot.captured.images.isEmpty())
  }
}
