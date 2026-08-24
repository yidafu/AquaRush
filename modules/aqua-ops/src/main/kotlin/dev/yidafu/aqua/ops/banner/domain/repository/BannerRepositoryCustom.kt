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
import java.time.LocalDateTime

/**
 * 轮播图仓库 QueryDSL 自定义查询(BannerRepository 中手写 @Query 迁移至此)。
 */
interface BannerRepositoryCustom {
  /**
   * 当前最大排序号(用于新建时缺省排序号 = max + 1)
   */
  fun findMaxSortOrder(): Int?

  /**
   * 按排序号升序取全量并加悲观写锁(用于上移/下移交换排序号,防并发)
   */
  fun findAllOrderedForUpdate(): List<BannerModel>

  /**
   * 当前生效轮播图:启用且处于时间窗内,按排序号升序(时间窗为空=长期)
   */
  fun findActiveBanners(now: LocalDateTime): List<BannerModel>
}
