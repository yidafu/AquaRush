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

import dev.yidafu.aqua.api.dto.CouponOption
import dev.yidafu.aqua.api.dto.GiftItem
import dev.yidafu.aqua.common.domain.model.enums.PriceSource

/**
 * 促销引擎计价中间状态(规则按 R-MKT-07 顺序累积)。
 */
data class PricingState(
  // R-MKT-02 单价解析
  var unitPriceCents: Long = 0,
  var priceSource: PriceSource = PriceSource.CATALOG_PRICE,
  var subtotalCents: Long = 0,
  // R-MKT-08 水票核销
  var ticketRedeemCount: Int = 0,
  var ticketCoveredQuantity: Int = 0,
  var cashSubtotalCents: Long = 0,
  var availableTicketCount: Int = 0,
  // R-MKT-07 叠加① 满减/折扣(与首单立减互斥取最大)
  var promotionDiscountCents: Long = 0,
  var promotionActivityId: Long? = null,
  var promotionActivityName: String? = null,
  // R-MKT-07 叠加① 首单立减(仅首单,与满减互斥取最大)
  var firstOrderDiscountCents: Long = 0,
  var firstOrderActivityId: Long? = null,
  var firstOrderActivityName: String? = null,
  // R-MKT-07 叠加③ 优惠券
  var couponDiscountCents: Long = 0,
  var selectedCouponId: Long? = null,
  var selectedCouponName: String? = null,
  var couponOptions: List<CouponOption> = emptyList(),
  // R-MKT-09 买赠/满赠赠品(附加 0 价行,不参与现金计价与叠加)
  var giftItems: List<GiftItem> = emptyList(),
) {
  val totalDiscountCents: Long
    get() = promotionDiscountCents + firstOrderDiscountCents + couponDiscountCents

  val payableProductCents: Long
    get() = maxOf(0, cashSubtotalCents - totalDiscountCents)
}
