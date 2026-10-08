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

package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 保存配送费规则请求(单行配置,整体覆盖保存)
 * 与 PRD 12-operations-config.md §F-OPS-005 / 04-order.md §R-MON-02 保持一致(楼层口径):
 * 免费楼层 ≥ 0、加价 0-999 元(≤2 位小数,以分存储);未配置时按默认规则(免费4楼/加价2元)。
 */
data class SaveDeliveryFeeRuleRequest(
  @field:Min(value = 0, message = "免费楼层不能小于0")
  @field:NotNull(message = "免费楼层不能为空")
  val freeFloorLimit: Int,
  @field:Min(value = 0, message = "运费金额无效")
  @field:Max(value = 99_900, message = "运费金额无效")
  @field:NotNull(message = "运费金额无效")
  val surchargeCents: Long,
)
