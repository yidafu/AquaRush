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

package dev.yidafu.aqua.ops.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.NoticeModel
import java.time.LocalDateTime

/**
 * 公告仓库 QueryDSL 自定义查询(NoticeRepository 中手写 @Query 迁移至此)。
 */
interface NoticeRepositoryCustom {
  /**
   * 当前生效公告:已发布且处于时间窗内(空=长期),置顶优先、发布时间倒序(AC-OPS-002-02)
   */
  fun findActiveNotices(now: LocalDateTime): List<NoticeModel>
}
