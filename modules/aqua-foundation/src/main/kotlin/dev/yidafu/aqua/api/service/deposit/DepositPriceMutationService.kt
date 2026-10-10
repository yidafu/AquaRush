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

package dev.yidafu.aqua.api.service.deposit

import dev.yidafu.aqua.api.dto.UpsertDepositPriceRequest
import dev.yidafu.aqua.common.domain.model.DepositPriceModel

/**
 * 水桶押金价目变更服务(R-MON-03 押金按规格配置,实现位于 aqua-marketing;AGENTS.md 规则 3)
 */
interface DepositPriceMutationService {
  /**
   * 新增/编辑价目(id 空=新增;规格唯一冲突拦截;押金 0..9999900 分)
   */
  fun upsert(request: UpsertDepositPriceRequest): DepositPriceModel
}
