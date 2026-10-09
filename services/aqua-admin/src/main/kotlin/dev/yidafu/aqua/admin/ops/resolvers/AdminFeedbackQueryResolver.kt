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
import dev.yidafu.aqua.common.graphql.generated.Feedback
import dev.yidafu.aqua.common.graphql.generated.FeedbackListInput
import dev.yidafu.aqua.common.graphql.generated.FeedbackVoPage
import dev.yidafu.aqua.ops.feedback.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 意见反馈查询 Resolver(管理后台,PERMISSION_FEEDBACK_READ)
 * 与 PRD 12-operations-config.md §F-OPS-003 保持一致。
 */
@Controller
class AdminFeedbackQueryResolver(
  private val feedbackQueryService: FeedbackQueryService,
) {
  /**
   * 分页查询反馈列表(类型/状态/联系方式关键字/时间范围筛选,按提交时间倒序)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_FEEDBACK_READ')")
  fun feedbacks(
    @Argument input: FeedbackListInput?,
  ): FeedbackVoPage {
    val page = feedbackQueryService.listFeedbacks(input ?: FeedbackListInput())
    return FeedbackVoPage(
      content = page.content.map { it.toGraphQL() },
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
      size = page.size,
      number = page.number,
    )
  }

  /**
   * 根据 ID 查询反馈详情
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_FEEDBACK_READ')")
  fun feedback(
    @Argument id: Long,
  ): Feedback? = feedbackQueryService.getFeedback(id)?.let { it.toGraphQL() }
}
