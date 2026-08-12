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

package dev.yidafu.aqua.ops.feedback.domain.repository

import dev.yidafu.aqua.common.domain.model.FeedbackModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * 意见反馈仓库
 */
@Repository
interface FeedbackRepository :
  JpaRepository<FeedbackModel, Long>,
  JpaSpecificationExecutor<FeedbackModel> {
  /**
   * 按内容模糊查询首条(测试种子数据定位用)
   */
  fun findFirstByContentContaining(content: String): FeedbackModel?
}
