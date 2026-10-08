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
 * 营销方式使用明细行 DTO(PRD 08-statistics.md §F-STA-007 营销方式使用明细表)
 * 区间内按 (kind, ref_id) 聚合;金额单位:分(R-MON-01,展示层转元)。
 * 票种行的 order_count/benefit_cents/usage_count 恒为 0,销售/核销见 ticket_* 字段;
 * 非票种行的 ticket_* 字段恒为 0。
 */
data class MarketingUsageBreakdownRowDTO(
  val kind: MarketingBreakdownKind,
  val refId: Long,
  /** 活动/券/票种名称(查询侧批量解析,不在中间表落 name) */
  val name: String,
  /** 使用订单数(券=订单快照含券的订单;满减/首单=命中订单;赠品=赠品订单;票种=0) */
  val orderCount: Long,
  /** 优惠金额(券/满减/首单立减=快照列合计;赠品=0,估算在展示层按当前目录价;票种=0) */
  val benefitCents: Long,
  /** 使用数量(券=核销张数;赠品=赠品件数;满减/首单=使用订单数;票种=0) */
  val usageCount: Long,
  /** 票种专用:销售张数/销售金额(分)/核销张数(其余 kind 为 0) */
  val ticketSalesCount: Long,
  val ticketSalesRevenueCents: Long,
  val ticketRedeemedCount: Long,
)
