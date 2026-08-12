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

import dev.yidafu.aqua.api.dto.FeedbackListRequest
import dev.yidafu.aqua.api.service.ops.FeedbackQueryService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 意见反馈查询服务实现(管理后台)
 */
@Service
class FeedbackQueryServiceImpl(
  private val feedbackRepository: FeedbackRepository,
) : FeedbackQueryService {
  override fun listFeedbacks(request: FeedbackListRequest): Page<FeedbackModel> {
    var spec: Specification<FeedbackModel> = Specification { _, _, cb -> cb.conjunction() }
    request.feedbackType?.let { spec = spec.and(FeedbackSpecifications.feedbackTypeEquals(it)) }
    request.status?.let { spec = spec.and(FeedbackSpecifications.statusEquals(it)) }
    request.contactKeyword?.takeIf { it.isNotBlank() }?.let { spec = spec.and(FeedbackSpecifications.contactKeywordContains(it)) }
    val dateFrom = request.dateFrom
    val dateTo = request.dateTo
    if (dateFrom != null && dateTo != null) {
      spec = spec.and(FeedbackSpecifications.createdAtBetween(dateFrom, dateTo))
    }
    val pageable = PageRequest.of(request.page, request.size, Sort.by(Sort.Direction.DESC, "createdAt"))
    return feedbackRepository.findAll(spec, pageable)
  }

  override fun getFeedback(id: Long): FeedbackModel? = feedbackRepository.findById(id).orElse(null)
}
