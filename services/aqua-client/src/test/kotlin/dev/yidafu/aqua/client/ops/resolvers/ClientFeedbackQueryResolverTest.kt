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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

/**
 * 用户端我的-反馈记录查询 Resolver 测试(F-USR-005 处理结果回传:myFeedbacks 透传 userId)
 */
class ClientFeedbackQueryResolverTest :
  FunSpec({
    var feedbackQueryApiService: FeedbackQueryApiService = mockk()
    var resolver: ClientFeedbackQueryResolver = mockk()
    var userPrincipal: UserPrincipal = mockk()

    beforeTest {
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
    test("myFeedbacks passes userId and paging") {
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
    test("myFeedbacks maps records and page info") {
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

      result.content.size shouldBe 1
      result.totalElements shouldBe 1
      result.totalPages shouldBe 1
      result.size shouldBe 10
      result.number shouldBe 0

      val vo = result.content[0]
      vo.id shouldBe 11L
      vo.feedbackType shouldBe dev.yidafu.aqua.common.graphql.generated.FeedbackType.COMPLAINT
      vo.status shouldBe dev.yidafu.aqua.common.graphql.generated.FeedbackStatus.RESOLVED
      vo.images shouldBe listOf("http://img/1.png")
      vo.handleResult shouldBe "已联系用户致歉并补偿"
      vo.handledByName shouldBe "运营小王"
    }
  })
