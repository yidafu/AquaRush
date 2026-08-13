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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 公告仓库
 * 注意:@SoftDelete 自动过滤已删行,派生查询/JPQL 均不含软删数据。
 */
@Repository
interface NoticeRepository :
  JpaRepository<NoticeModel, Long>,
  JpaSpecificationExecutor<NoticeModel> {
  /**
   * 当前生效公告:已发布且处于时间窗内(空=长期),置顶优先、发布时间倒序(AC-OPS-002-02)
   */
  @Query(
    """
    select n from NoticeModel n
    where n.status = 'PUBLISHED'
      and (n.startAt is null or n.startAt <= :now)
      and (n.endAt is null or n.endAt >= :now)
    order by n.pinned desc, n.publishedAt desc nulls last
    """,
  )
  fun findActiveNotices(
    @Param("now") now: LocalDateTime,
  ): List<NoticeModel>
}
