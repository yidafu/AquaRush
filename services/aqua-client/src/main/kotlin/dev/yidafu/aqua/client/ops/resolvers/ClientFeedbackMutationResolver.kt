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
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.FeedbackSubmitInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.feedback.mapper.FeedbackSubmitInputMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 用户端意见反馈提交 Resolver(F-USR-005)
 * 提交落库(G04 目标形态);userId 取自登录主体。
 */
@ClientService
@Controller
class ClientFeedbackMutationResolver(
  private val feedbackMutationApiService: FeedbackMutationApiService,
) {
  private val logger = LoggerFactory.getLogger(ClientFeedbackMutationResolver::class.java)

  /**
   * 提交反馈,成功返回 true
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun submitFeedback(
    @Argument @Valid input: FeedbackSubmitInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean {
    logger.info("User {} submitting feedback type {}", userPrincipal.id, input.feedbackType)
    feedbackMutationApiService.submitFeedback(
      userId = userPrincipal.id,
      request = FeedbackSubmitInputMapper.map(input),
    )
    return true
  }
}
