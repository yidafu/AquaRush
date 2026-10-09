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

import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.graphql.generated.Feedback
import dev.yidafu.aqua.common.graphql.generated.FeedbackRecordVo
import dev.yidafu.aqua.common.graphql.generated.FeedbackStatus
import dev.yidafu.aqua.common.graphql.generated.FeedbackType
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus as DomainFeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType as DomainFeedbackType

/**
 * 意见反馈映射器:域实体 → GraphQL 类型
 */
fun FeedbackModel.toGraphQL(): Feedback =
  Feedback(
    contactInfo = contactInfo,
    content = content,
    createdAt = createdAt,
    feedbackType = feedbackType.toGraphQL(),
    handleResult = handleResult,
    handledAt = handledAt,
    handledById = handledById,
    handledByName = handledByName,
    id = id ?: 0L,
    images = getImagesAsList(),
    status = status.toGraphQL(),
    userId = userId,
  )

/**
 * 我的-反馈记录映射器:域实体 → 用户端 GraphQL 类型(F-USR-005 处理结果回传)
 * 不含联系方式与 userId(用户端展示本人反馈,联系方式仅后台管理侧可见)。
 */
fun FeedbackModel.toRecordVo(): FeedbackRecordVo =
  FeedbackRecordVo(
    content = content,
    createdAt = createdAt,
    feedbackType = feedbackType.toGraphQL(),
    handleResult = handleResult,
    handledAt = handledAt,
    handledByName = handledByName,
    id = id ?: 0L,
    images = getImagesAsList(),
    status = status.toGraphQL(),
  )

/**
 * 反馈类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainFeedbackType.toGraphQL(): FeedbackType = FeedbackType.valueOf(name)

fun FeedbackType.toDomain(): DomainFeedbackType = DomainFeedbackType.valueOf(name)

/**
 * 反馈状态:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainFeedbackStatus.toGraphQL(): FeedbackStatus = FeedbackStatus.valueOf(name)

fun FeedbackStatus.toDomain(): DomainFeedbackStatus = DomainFeedbackStatus.valueOf(name)
