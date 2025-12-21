/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.review.resolvers

import dev.yidafu.aqua.api.service.ReviewService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.dto.ReviewResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.common.dto.CreateReviewRequest as CommonCreateReviewRequest

/**
 * 客户端评价变更解析器
 * 提供用户评价创建功能
 */
@ClientService
@Controller
class ClientReviewMutationResolver(
  private val reviewService: ReviewService,
) {
  /**
   * 用户创建评价
   */
  @PreAuthorize("isAuthenticated()")
  fun createReview(request: CommonCreateReviewRequest): ReviewResponse {
    // 验证评价数据
    validateReviewRequest(request)

    return reviewService.createReview(getCurrentUserId(), request)
  }

  /**
   * 获取当前认证用户的ID
   * 在实际实现中应该从Spring Security Context获取
   */
  private fun getCurrentUserId(): Long {
    // TODO: 从Spring Security Context获取当前用户ID
    // 暂时返回占位符，实际实现需要：
    // val authentication = SecurityContextHolder.getContext().authentication
    // return (authentication.principal as UserDetails).id
    throw UnsupportedOperationException("需要从Spring Security Context获取用户ID")
  }

  /**
   * 验证评价请求数据
   */
  private fun validateReviewRequest(request: CommonCreateReviewRequest) {
    if (request.rating !in MIN_RATING..MAX_RATING) {
      throw IllegalArgumentException("评分必须在${MIN_RATING}-${MAX_RATING}之间")
    }

    if (request.comment?.length ?: 0 > MAX_COMMENT_LENGTH) {
      throw IllegalArgumentException("评论长度不能超过$MAX_COMMENT_LENGTH 个字符")
    }
  }

  companion object {
    const val MIN_RATING = 1
    const val MAX_RATING = 5
    const val MAX_COMMENT_LENGTH = 500
  }
}
