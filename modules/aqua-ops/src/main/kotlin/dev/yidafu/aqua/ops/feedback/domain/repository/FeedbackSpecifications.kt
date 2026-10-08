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
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime

/**
 * 意见反馈列表筛选条件(JPA Criteria)
 * 与 PRD 12-operations-config.md §F-OPS-003 保持一致:类型/状态/联系方式关键字/时间范围。
 */
class FeedbackSpecifications {
  companion object {
    fun feedbackTypeEquals(feedbackType: FeedbackType): Specification<FeedbackModel> =
      Specification { root, _, cb ->
        cb.equal(root.get<Enum<*>>("feedbackType"), feedbackType)
      }

    fun statusEquals(status: FeedbackStatus): Specification<FeedbackModel> =
      Specification { root, _, cb ->
        cb.equal(root.get<Enum<*>>("status"), status)
      }

    /**
     * 按用户过滤(用户详情页「意见反馈」Tab)
     */
    fun userIdEquals(userId: Long): Specification<FeedbackModel> =
      Specification { root, _, cb ->
        cb.equal(root.get<Long>("userId"), userId)
      }

    /**
     * 联系方式关键字模糊匹配(大小写不敏感)
     */
    fun contactKeywordContains(keyword: String): Specification<FeedbackModel> =
      Specification { root, _, cb ->
        cb.like(cb.lower(root.get<String>("contactInfo")), "%${keyword.lowercase()}%")
      }

    fun createdAtBetween(
      dateFrom: LocalDateTime,
      dateTo: LocalDateTime,
    ): Specification<FeedbackModel> =
      Specification { root, _, cb ->
        cb.between(root.get<LocalDateTime>("createdAt"), dateFrom, dateTo)
      }
  }
}
