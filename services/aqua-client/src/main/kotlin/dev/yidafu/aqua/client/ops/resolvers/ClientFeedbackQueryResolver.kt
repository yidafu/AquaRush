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
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.FeedbackRecordVoPage
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.feedback.mapper.toRecordVo
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端意见反馈查询 Resolver(F-USR-005 处理结果回传)
 * 我的-反馈记录:仅返回本人反馈(userId 强制取登录主体,防越权),按提交时间倒序分页。
 */
@ClientService
@Controller
class ClientFeedbackQueryResolver(
  private val feedbackQueryApiService: FeedbackQueryApiService,
) {
  /**
   * 我的-反馈记录(feedback-records):分页查询本人反馈,展示状态与处理结果
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myFeedbacks(
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FeedbackRecordVoPage {
    val result =
      feedbackQueryApiService.findMyFeedbacks(
        userId = principal.id,
        page = page,
        size = size,
      )
    return FeedbackRecordVoPage(
      content = result.content.map { it.toRecordVo() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }
}
