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

import dev.yidafu.aqua.api.dto.CreateBarrelFeeRequest
import dev.yidafu.aqua.api.dto.UpdateBarrelFeeRequest
import dev.yidafu.aqua.common.domain.model.BarrelFeeModel

/**
 * 水桶费价目变更服务(G06 水桶费模块,实现位于 aqua-marketing;CLAUDE.md 规则 4)
 */
interface BarrelFeeMutationService {
  /** 新增价目(规格唯一,重复拦截) */
  fun create(request: CreateBarrelFeeRequest): BarrelFeeModel

  /** 编辑价目(规格/价格/排序) */
  fun update(
    id: Long,
    request: UpdateBarrelFeeRequest,
  ): BarrelFeeModel

  /** 停用价目(停用后用户端不可购;不影响已生成购桶单) */
  fun disable(id: Long): BarrelFeeModel
}
