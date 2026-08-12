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

package dev.yidafu.aqua.ops.feedback.mapper

import dev.yidafu.aqua.api.dto.FeedbackListRequest
import dev.yidafu.aqua.api.dto.FeedbackReviewRequest
import dev.yidafu.aqua.api.dto.FeedbackSubmitRequest
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.graphql.generated.Feedback
import dev.yidafu.aqua.common.graphql.generated.FeedbackListInput
import dev.yidafu.aqua.common.graphql.generated.FeedbackReviewInput
import dev.yidafu.aqua.common.graphql.generated.FeedbackStatus
import dev.yidafu.aqua.common.graphql.generated.FeedbackSubmitInput
import dev.yidafu.aqua.common.graphql.generated.FeedbackType
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus as DomainFeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType as DomainFeedbackType

/**
 * 意见反馈映射器:域实体 → GraphQL 类型
 */
object FeedbackMapper : ObjectMappie<FeedbackModel, Feedback>() {
  override fun map(from: FeedbackModel): Feedback =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::feedbackType fromExpression { FeedbackTypeMapper.map(from.feedbackType) }
      to::status fromExpression { FeedbackStatusMapper.map(from.status) }
      to::images fromExpression { from.getImagesAsList() }
    }
}

object FeedbackTypeMapper : EnumMappie<DomainFeedbackType, FeedbackType>()

object FeedbackStatusMapper : EnumMappie<DomainFeedbackStatus, FeedbackStatus>()

/**
 * 反馈提交输入映射器:GraphQL 输入 → 服务请求
 */
object FeedbackSubmitInputMapper : ObjectMappie<FeedbackSubmitInput, FeedbackSubmitRequest>() {
  override fun map(from: FeedbackSubmitInput): FeedbackSubmitRequest =
    mapping {
      to::feedbackType fromExpression { FeedbackTypeInputMapper.map(from.feedbackType) }
      to::images fromExpression { from.images?.toList() ?: emptyList() }
    }
}

/**
 * 反馈处理输入映射器
 */
object FeedbackReviewInputMapper : ObjectMappie<FeedbackReviewInput, FeedbackReviewRequest>() {
  override fun map(from: FeedbackReviewInput): FeedbackReviewRequest =
    mapping {
      to::status fromExpression { FeedbackStatusInputMapper.map(from.status) }
    }
}

/**
 * 反馈列表查询输入映射器
 */
object FeedbackListInputMapper : ObjectMappie<FeedbackListInput, FeedbackListRequest>() {
  override fun map(from: FeedbackListInput): FeedbackListRequest =
    mapping {
      to::page fromValue (from.page ?: 0)
      to::size fromValue (from.size ?: 20)
      to::feedbackType fromExpression { from.feedbackType?.let { FeedbackTypeInputMapper.map(it) } }
      to::status fromExpression { from.status?.let { FeedbackStatusInputMapper.map(it) } }
    }
}

object FeedbackTypeInputMapper : EnumMappie<FeedbackType, DomainFeedbackType>()

object FeedbackStatusInputMapper : EnumMappie<FeedbackStatus, DomainFeedbackStatus>()
