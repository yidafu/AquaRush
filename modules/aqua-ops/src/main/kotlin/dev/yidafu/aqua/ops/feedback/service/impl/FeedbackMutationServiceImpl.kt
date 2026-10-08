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

import dev.yidafu.aqua.api.dto.FeedbackReviewRequest
import dev.yidafu.aqua.api.service.ops.FeedbackMutationService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.feedback.domain.exception.FeedbackNotFoundException
import dev.yidafu.aqua.ops.feedback.domain.exception.InvalidFeedbackStatusTransitionException
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 意见反馈处理服务实现(管理后台)
 * 状态仅允许 待处理 → 处理中 → 已处理 前进(同级重设允许,如补填处理结果);处理人/时间留痕,落审计日志。
 */
@Service
class FeedbackMutationServiceImpl(
  private val feedbackRepository: FeedbackRepository,
  private val opsConfigAuditService: OpsConfigAuditService,
) : FeedbackMutationService {
  @Transactional
  override fun reviewFeedback(
    adminId: Long,
    adminName: String,
    id: Long,
    request: FeedbackReviewRequest,
  ): FeedbackModel {
    val feedback = feedbackRepository.findById(id).orElseThrow { FeedbackNotFoundException(id) }
    validateStatusTransition(feedback.status, request.status)

    val before = feedbackSnapshot(feedback)
    feedback.status = request.status
    if (request.handleResult != null) {
      feedback.handleResult = request.handleResult
    }
    if (request.status == FeedbackStatus.RESOLVED) {
      feedback.handledById = adminId
      feedback.handledByName = adminName
      feedback.handledAt = LocalDateTime.now()
    }
    val saved = feedbackRepository.save(feedback)

    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_FEEDBACK,
      targetId = saved.id,
      actionType = OpsConfigAuditService.ACTION_FEEDBACK_PROCESS,
      operatorId = adminId,
      operatorName = adminName,
      before = before,
      after = feedbackSnapshot(saved),
    )
    return saved
  }

  /**
   * 状态只允许前进(ordinal 序:待处理 < 处理中 < 已处理),同级重设允许
   */
  private fun validateStatusTransition(
    current: FeedbackStatus,
    target: FeedbackStatus,
  ) {
    if (target.ordinal < current.ordinal) {
      throw InvalidFeedbackStatusTransitionException(current, target)
    }
  }

  private fun feedbackSnapshot(feedback: FeedbackModel): Map<String, Any?> =
    linkedMapOf(
      "feedbackType" to feedback.feedbackType.name,
      "content" to feedback.content,
      "images" to feedback.getImagesAsList(),
      "contactInfo" to feedback.contactInfo,
      "status" to feedback.status.name,
      "handleResult" to feedback.handleResult,
      "handledByName" to feedback.handledByName,
      "handledAt" to feedback.handledAt,
    )
}
