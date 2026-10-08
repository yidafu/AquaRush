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
import org.springframework.data.domain.Page

/**
 * 意见反馈查询服务接口(用户端)
 * 与 PRD 07-user.md §F-USR-005 保持一致:我的-反馈记录(feedback-records)分页查询,
 * userId 由登录主体强制注入(防越权),按提交时间倒序。
 */
interface FeedbackQueryApiService {
  /**
   * 分页查询当前用户的反馈记录(仅本人数据)
   */
  fun findMyFeedbacks(
    userId: Long,
    page: Int,
    size: Int,
  ): Page<FeedbackModel>
}
