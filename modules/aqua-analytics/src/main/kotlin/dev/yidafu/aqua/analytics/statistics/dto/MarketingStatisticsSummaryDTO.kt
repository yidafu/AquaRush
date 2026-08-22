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

/**
 * 营销统计汇总 DTO(PRD 08-statistics.md §F-STA-007,与所选时间范围一致)
 * 金额单位:分(R-MON-01,展示层转元)。
 */
data class MarketingStatisticsSummaryDTO(
  /** 营销订单数(区间内订单含任一快照优惠,按订单去重) */
  val marketingOrderCount: Long,
  /** 让利总额 = 券 + 满减 + 首单立减(订单快照列,精确口径;赠品估算/水票抵扣不计入) */
  val totalBenefitCents: Long,
  val couponBenefitCents: Long,
  val promotionBenefitCents: Long,
  val firstOrderBenefitCents: Long,
  /** 用券数(核销张数) */
  val couponRedeemedCount: Long,
  /** 水票销售张数/销售金额(分) */
  val ticketSalesCount: Long,
  val ticketSalesRevenueCents: Long,
  /** 水票核销张数 */
  val ticketRedeemedCount: Long,
  /** 赠品件数(order_gifts) */
  val giftQuantity: Long,
)
