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

package dev.yidafu.aqua.ops.banner.domain.repository

import dev.yidafu.aqua.common.domain.model.BannerModel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 轮播图仓库
 * 注意:@SoftDelete 自动过滤已删行,派生查询/JPQL 均不含软删数据。
 */
@Repository
interface BannerRepository :
  JpaRepository<BannerModel, Long>,
  JpaSpecificationExecutor<BannerModel> {
  /**
   * 当前最大排序号(用于新建时缺省排序号 = max + 1)
   */
  @Query("select max(b.sortOrder) from BannerModel b")
  fun findMaxSortOrder(): Int?

  /**
   * 按排序号查询(用于编辑时唯一性校验)
   */
  fun findBySortOrder(sortOrder: Int): BannerModel?

  /**
   * 按排序号升序取全量并加悲观写锁(用于上移/下移交换排序号,防并发)
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from BannerModel b order by b.sortOrder asc")
  fun findAllOrderedForUpdate(): List<BannerModel>

  /**
   * 当前生效轮播图:启用且处于时间窗内,按排序号升序(时间窗为空=长期)
   */
  @Query(
    """
    select b from BannerModel b
    where b.enabled = true
      and (b.startAt is null or b.startAt <= :now)
      and (b.endAt is null or b.endAt >= :now)
    order by b.sortOrder asc
    """,
  )
  fun findActiveBanners(
    @Param("now") now: LocalDateTime,
  ): List<BannerModel>
}
