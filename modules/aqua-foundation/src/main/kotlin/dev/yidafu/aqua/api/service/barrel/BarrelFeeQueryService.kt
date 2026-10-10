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

package dev.yidafu.aqua.api.service.barrel

import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import org.springframework.data.domain.Page

/**
 * 水桶费价目查询服务(G06 水桶费模块,实现位于 aqua-marketing;AGENTS.md 规则 3)
 */
interface BarrelFeeQueryService {
  /** 用户端买水桶页:启用价目(升序) */
  fun listEnabled(): List<BarrelFeeModel>

  /** 后台价目管理分页(状态/关键词筛选) */
  fun list(
    status: BarrelFeeStatus?,
    keyword: String?,
    page: Int,
    size: Int,
  ): Page<BarrelFeeModel>
}
