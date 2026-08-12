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

package dev.yidafu.aqua.ops.feedback.service.impl

import dev.yidafu.aqua.api.dto.FeedbackSubmitRequest
import dev.yidafu.aqua.api.service.ops.FeedbackMutationApiService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 意见反馈提交服务实现(用户端)
 * 与 PRD 07-user.md §F-USR-005 保持一致:提交落库(G04 目标形态),状态初始为待处理。
 */
@Service
class FeedbackMutationApiServiceImpl(
  private val feedbackRepository: FeedbackRepository,
) : FeedbackMutationApiService {
  @Transactional
  override fun submitFeedback(
    userId: Long,
    request: FeedbackSubmitRequest,
  ): FeedbackModel {
    val feedback =
      FeedbackModel(
        userId = userId,
        feedbackType = request.feedbackType,
        content = request.content,
        contactInfo = request.contactInfo,
        status = FeedbackStatus.PENDING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    feedback.setImagesFromList(request.images)
    return feedbackRepository.save(feedback)
  }
}
