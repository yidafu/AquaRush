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
 * 满减/折扣活动档位(PRD 10-marketing.md §R-MKT-04):
 * 满减用 discountAmountCents,满折用 discountRatePermille(千分比,9000=9折);档位金额递增且互斥。
 */
data class PromotionTierItem(
  @field:NotNull(message = "档位门槛无效")
  @field:Min(value = 1, message = "档位门槛无效")
  @field:Max(value = 9999900, message = "档位门槛无效(0.01-99999元)")
  val thresholdCents: Long,
  @field:Min(value = 1, message = "满减金额无效")
  @field:Max(value = 9999900, message = "满减金额无效(0.01-99999元)")
  val discountAmountCents: Long? = null,
  @field:Min(value = 100, message = "折扣率超出范围(1-9.9折)")
  @field:Max(value = 9900, message = "折扣率超出范围(1-9.9折)")
  val discountRatePermille: Long? = null,
)
