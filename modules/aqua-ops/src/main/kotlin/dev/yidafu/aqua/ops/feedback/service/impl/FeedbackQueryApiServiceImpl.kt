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

import dev.yidafu.aqua.api.service.ops.FeedbackQueryApiService
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * 意见反馈查询服务实现(用户端)
 * 与 PRD 07-user.md §F-USR-005 保持一致:我的-反馈记录仅返回本人数据(userId 来自登录主体),
 * 按提交时间倒序分页(与后台列表排序口径一致)。
 */
@Service
class FeedbackQueryApiServiceImpl(
  private val feedbackRepository: FeedbackRepository,
) : FeedbackQueryApiService {
  override fun findMyFeedbacks(
    userId: Long,
    page: Int,
    size: Int,
  ): Page<FeedbackModel> {
    val pageable =
      PageRequest.of(
        page.coerceAtLeast(0),
        size.coerceIn(1, 50),
        Sort.by(Sort.Direction.DESC, "createdAt"),
      )
    return feedbackRepository.findByUserId(userId, pageable)
  }
}
