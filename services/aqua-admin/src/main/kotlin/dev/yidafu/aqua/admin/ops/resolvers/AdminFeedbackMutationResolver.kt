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
import dev.yidafu.aqua.common.graphql.generated.Feedback
import dev.yidafu.aqua.common.graphql.generated.FeedbackReviewInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.feedback.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 意见反馈处理 Resolver(管理后台,PERMISSION_FEEDBACK_REVIEW)
 * 处理人取自登录主体,处理人/时间由服务留痕并落审计日志。
 */
@Controller
class AdminFeedbackMutationResolver(
  private val feedbackMutationService: FeedbackMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminFeedbackMutationResolver::class.java)

  /**
   * 处理反馈(填写处理结果并流转状态,仅允许前进)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_FEEDBACK_REVIEW')")
  fun reviewFeedback(
    @Argument id: Long,
    @Argument @Valid input: FeedbackReviewInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Feedback {
    logger.info("Admin {} reviewing feedback {} status {}", principal.username, id, input.status)
    val feedback =
      feedbackMutationService.reviewFeedback(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return feedback.toGraphQL()
  }
}
