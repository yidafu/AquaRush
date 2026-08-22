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

package dev.yidafu.aqua.analytics.statistics.dto

import java.time.LocalDate

/**
 * 营销统计单日 DTO(PRD 08-statistics.md §F-STA-007)
 * 金额单位:分(R-MON-01,展示层转元)。
 */
data class MarketingDailyStatisticDTO(
  val date: LocalDate,
  /** 当日营销订单数(订单含 券/满减/首单立减 任一快照优惠,按订单去重) */
  val marketingOrderCount: Long,
  /** 当日券优惠金额合计(coupon_discount_cents 快照) */
  val couponBenefitCents: Long,
  /** 当日满减/折扣优惠金额合计(promotion_discount_cents 快照) */
  val promotionBenefitCents: Long,
  /** 当日首单立减金额合计(first_order_discount_cents 快照) */
  val firstOrderBenefitCents: Long,
  /** 当日核销券张数(user_coupons USED 按 used_at) */
  val couponRedeemedCount: Long,
  /** 当日已支付购票张数(ticket_purchases PAID 按 paidAt,回退 createdAt) */
  val ticketSalesCount: Long,
  /** 当日购票销售金额(分) */
  val ticketSalesRevenueCents: Long,
  /** 当日水票核销张数(user_tickets USED 按 usedAt) */
  val ticketRedeemedCount: Long,
  /** 当日赠品件数(order_gifts 按 created_at) */
  val giftQuantity: Long,
) {
  companion object {
    fun zero(date: LocalDate) =
      MarketingDailyStatisticDTO(
        date = date,
        marketingOrderCount = 0L,
        couponBenefitCents = 0L,
        promotionBenefitCents = 0L,
        firstOrderBenefitCents = 0L,
        couponRedeemedCount = 0L,
        ticketSalesCount = 0L,
        ticketSalesRevenueCents = 0L,
        ticketRedeemedCount = 0L,
        giftQuantity = 0L,
      )
  }
}
