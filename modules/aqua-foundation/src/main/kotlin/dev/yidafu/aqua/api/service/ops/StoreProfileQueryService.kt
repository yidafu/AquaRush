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

import dev.yidafu.aqua.common.domain.model.StoreProfileModel

/**
 * 店铺信息查询服务接口(管理后台)
 * 与 PRD 12-operations-config.md §F-OPS-004 保持一致(单行配置)。
 */
interface StoreProfileQueryService {
  /**
   * 获取当前店铺配置(未配置返回 null,用户端按默认值兜底)
   */
  fun getStoreProfile(): StoreProfileModel?
}
