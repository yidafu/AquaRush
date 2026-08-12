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

package dev.yidafu.aqua.api.service.ops

import dev.yidafu.aqua.api.dto.FeedbackSubmitRequest
import dev.yidafu.aqua.common.domain.model.FeedbackModel

/**
 * 意见反馈提交服务接口(用户端)
 * 与 PRD 07-user.md §F-USR-005 保持一致:类型/内容/图片(≤3)/联系方式,提交落库(G04 目标形态)。
 */
interface FeedbackMutationApiService {
  /**
   * 提交反馈
   */
  fun submitFeedback(
    userId: Long,
    request: FeedbackSubmitRequest,
  ): FeedbackModel
}
