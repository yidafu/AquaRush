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

package dev.yidafu.aqua.ops.feedback.domain.exception

import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus

/**
 * 意见反馈未找到异常
 */
class FeedbackNotFoundException(
  feedbackId: Long,
) : RuntimeException("Feedback not found with ID: $feedbackId")

/**
 * 反馈状态流转非法异常(仅允许 待处理 → 处理中 → 已处理 前进)
 */
class InvalidFeedbackStatusTransitionException(
  currentStatus: FeedbackStatus,
  targetStatus: FeedbackStatus,
) : RuntimeException(
    "Invalid feedback status transition from $currentStatus to $targetStatus",
  )
