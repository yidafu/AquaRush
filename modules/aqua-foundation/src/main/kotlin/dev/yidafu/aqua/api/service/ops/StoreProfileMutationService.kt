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

import dev.yidafu.aqua.api.dto.SaveStoreProfileRequest
import dev.yidafu.aqua.common.domain.model.StoreProfileModel

/**
 * 店铺信息变更服务接口(管理后台)
 * 单行配置(固定 id = 1),整体覆盖保存;操作人显式传参,由实现落审计日志(R-OPS-01)。
 */
interface StoreProfileMutationService {
  /**
   * 保存店铺信息(首次保存创建单行配置,再次保存覆盖)
   */
  fun saveStoreProfile(
    adminId: Long,
    adminName: String,
    request: SaveStoreProfileRequest,
  ): StoreProfileModel
}
