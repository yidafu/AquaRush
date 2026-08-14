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
 * 促销引擎计价请求(PRD 10-marketing.md §R-MKT-02/07/08):
 * 成交单价按 R-MKT-02 解析(分组价→目录价),订单层按 R-MKT-07 叠加(满减→优惠券),水票核销先于现金计价;
 * 金额一律服务端计算(R-MON-01),前端不传价格。
 */
data class OrderPricingRequest(
  @field:NotNull(message = "用户不能为空")
  val userId: Long,
  @field:NotNull(message = "商品不能为空")
  val productId: Long,
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  @field:Max(value = 999, message = "数量不能超过999")
  val quantity: Int,
  val isSelfCollect: Boolean = false,
  // 优惠券偏好(null=引擎自动选最优,0=不用券;一单一券 R-MKT-03)
  val preferredCouponId: Long? = null,
  // 水票核销张数(≤下单桶数 ∩ 可用张数;被核销桶按单价 0 计入 R-MKT-08)
  @field:Min(value = 0, message = "水票核销张数无效")
  @field:Max(value = 999, message = "水票核销张数不能超过999")
  val ticketRedeemCount: Int = 0,
)
