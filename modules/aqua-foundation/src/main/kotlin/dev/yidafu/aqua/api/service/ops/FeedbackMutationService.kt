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

import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.graphql.generated.FeedbackReviewInput

/**
 * 意见反馈处理服务接口(管理后台)
 * 处理人显式传参,由实现落审计日志(R-OPS-01);状态仅允许 待处理 → 处理中 → 已处理 前进。
 */
interface FeedbackMutationService {
  /**
   * 处理反馈(填写处理结果并流转状态),处理人/时间留痕
   */
  fun reviewFeedback(
    adminId: Long,
    adminName: String,
    id: Long,
    input: FeedbackReviewInput,
  ): FeedbackModel
}
