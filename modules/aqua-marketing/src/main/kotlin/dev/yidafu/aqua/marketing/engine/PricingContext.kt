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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.common.domain.model.ProductModel
import java.time.LocalDateTime

/**
 * 促销引擎计价上下文(PRD 10-marketing.md §R-MKT-02):
 * orderId 为空=纯预览(无副作用);非空=下单消费(同事务原子核销,失败整体回滚)。
 */
data class PricingContext(
  val userId: Long,
  val productId: Long,
  val quantity: Int,
  val isSelfCollect: Boolean,
  /** null=引擎自动选最优券,0=不用券 */
  val preferredCouponId: Long?,
  val ticketRedeemCount: Int,
  val now: LocalDateTime,
  val product: ProductModel,
  val orderId: Long? = null,
)
