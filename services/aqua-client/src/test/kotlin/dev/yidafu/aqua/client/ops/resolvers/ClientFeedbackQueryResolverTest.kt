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

import dev.yidafu.aqua.api.service.ops.FeedbackQueryApiService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 用户端我的-反馈记录查询 Resolver 测试(F-USR-005 处理结果回传:myFeedbacks 透传 userId)
 */
@DisplayName("ClientFeedbackQueryResolver Tests")
class ClientFeedbackQueryResolverTest {
  private lateinit var feedbackQueryApiService: FeedbackQueryApiService
  private lateinit var resolver: ClientFeedbackQueryResolver
  private lateinit var userPrincipal: UserPrincipal

  @BeforeEach
  fun setUp() {
    feedbackQueryApiService = mockk(relaxed = true)
    resolver = ClientFeedbackQueryResolver(feedbackQueryApiService)
    userPrincipal =
      UserPrincipal(
        id = 100L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  @Test
  @DisplayName("myFeedbacks should pass principal userId and page/size to service")
  fun `myFeedbacks passes userId and paging`() {
    every { feedbackQueryApiService.findMyFeedbacks(any(), any(), any()) } returns PageImpl(emptyList())

    resolver.myFeedbacks(page = 1, size = 10, userPrincipal)

    verify {
      feedbackQueryApiService.findMyFeedbacks(
        userId = 100L,
        page = 1,
        size = 10,
      )
    }
  }

  @Test
  @DisplayName("myFeedbacks should map records and page info into FeedbackRecordVoPage")
  fun `myFeedbacks maps records and page info`() {
    val feedback =
      FeedbackModel(
        id = 11L,
        userId = 100L,
        feedbackType = FeedbackType.COMPLAINT,
        content = "配送迟到一小时,请尽快处理",
        status = FeedbackStatus.RESOLVED,
        handleResult = "已联系用户致歉并补偿",
        handledByName = "运营小王",
        handledAt = LocalDateTime.of(2026, 8, 14, 10, 30),
        createdAt = LocalDateTime.of(2026, 8, 13, 9, 0),
      )
    feedback.setImagesFromList(listOf("http://img/1.png"))
    val feedbackPage =
      PageImpl(
        listOf(feedback),
        PageRequest.of(0, 10),
        1,
      )
    every {
      feedbackQueryApiService.findMyFeedbacks(any(), any(), any())
    } returns feedbackPage

    val result = resolver.myFeedbacks(page = 0, size = 10, userPrincipal)

    assertEquals(1, result.content.size)
    assertEquals(1, result.totalElements)
    assertEquals(1, result.totalPages)
    assertEquals(10, result.size)
    assertEquals(0, result.number)

    val vo = result.content[0]
    assertEquals(11L, vo.id)
    assertEquals(dev.yidafu.aqua.common.graphql.generated.FeedbackType.COMPLAINT, vo.feedbackType)
    assertEquals(dev.yidafu.aqua.common.graphql.generated.FeedbackStatus.RESOLVED, vo.status)
    assertEquals(listOf("http://img/1.png"), vo.images)
    assertEquals("已联系用户致歉并补偿", vo.handleResult)
    assertEquals("运营小王", vo.handledByName)
  }
}
