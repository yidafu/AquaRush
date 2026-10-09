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

import dev.yidafu.aqua.api.service.ops.FeedbackQueryService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.graphql.generated.FeedbackListInput
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackFilter
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import dev.yidafu.aqua.ops.feedback.mapper.toDomain
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * 意见反馈查询服务实现(管理后台)
 * 列表排序固定为 created_at 倒序，由仓储层保证(原 `Sort.by(DESC, "createdAt")` 已随
 * JPA Specification 一并迁移进 [FeedbackRepository.findPage])。
 */
@Service
class FeedbackQueryServiceImpl(
  private val feedbackRepository: FeedbackRepository,
) : FeedbackQueryService {
  override fun listFeedbacks(input: FeedbackListInput): Page<FeedbackModel> {
    // 与原实现一致：时间范围必须两端同时给才生效
    val dateFrom = input.dateFrom
    val dateTo = input.dateTo
    val hasDateRange = dateFrom != null && dateTo != null
    val filter =
      FeedbackFilter(
        feedbackType = input.feedbackType?.toDomain(),
        status = input.status?.toDomain(),
        userId = input.userId,
        contactKeyword = input.contactKeyword,
        dateFrom = if (hasDateRange) dateFrom else null,
        dateTo = if (hasDateRange) dateTo else null,
      )
    val pageable = PageRequest.of(input.page ?: 0, input.size ?: 20)
    return feedbackRepository.findPage(filter, pageable)
  }

  override fun getFeedback(id: Long): FeedbackModel? = feedbackRepository.findById(id).orElse(null)
}
