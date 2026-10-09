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

import dev.yidafu.aqua.api.dto.ReorderDirection
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.graphql.generated.CreateBannerInput
import dev.yidafu.aqua.common.graphql.generated.UpdateBannerInput

/**
 * 轮播图变更服务接口(管理后台)
 * 操作人显式传参,由实现落审计日志(R-OPS-01)。
 */
interface BannerMutationService {
  /**
   * 新建轮播图
   */
  fun createBanner(
    adminId: Long,
    adminName: String,
    input: CreateBannerInput,
  ): BannerModel

  /**
   * 编辑轮播图(全量更新)
   */
  fun updateBanner(
    adminId: Long,
    adminName: String,
    id: Long,
    input: UpdateBannerInput,
  ): BannerModel

  /**
   * 删除轮播图(软删除)
   */
  fun deleteBanner(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean

  /**
   * 上移/下移调整排序,返回重排后的全量列表(按 sortOrder 升序)
   */
  fun reorderBanner(
    adminId: Long,
    adminName: String,
    id: Long,
    direction: ReorderDirection,
  ): List<BannerModel>
}
