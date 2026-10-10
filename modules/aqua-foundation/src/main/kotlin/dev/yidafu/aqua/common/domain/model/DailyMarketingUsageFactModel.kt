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

package dev.yidafu.aqua.common.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 营销使用日事实(第 3 层历史趋势指标中间表,夜间快照任务写入,F-STA-007)
 * 原子粒度:stat_date × kind × ref_id(营销方式使用明细数据源)
 * - kind/ref_id:营销方式与活动引用(COUPON_TEMPLATE/PROMOTION_ACTIVITY/FIRST_ORDER_ACTIVITY/GIFT_ACTIVITY/TICKET_TYPE)
 * - order_count:当日使用订单数(按订单去重)
 * - benefit_cents:当日优惠/让利金额(券/满减/首单立减=订单快照列合计;赠品=0,让利估算在查询侧按当前目录价计算;水票=0)
 * - usage_count:券=当日核销张数;赠品=当日赠品件数;满减/首单=使用订单数;水票=0
 * - ticket_sales_count/ticket_sales_revenue_cents/ticket_redeemed_count:票种专用(其余 kind 为 0)
 * 活动名易变,不落 name 列(查询时经 api 服务批量取名,与 daily_product_facts「商品名易变,查询时 join」一致)。
 */
data class DailyMarketingUsageFactModel(
  var id: Long? = null,
  var statDate: LocalDate = LocalDate.now(),
  var kind: String = "",
  var refId: Long = 0,
  var orderCount: Long = 0,
  var benefitCents: Long = 0,
  var usageCount: Long = 0,
  var ticketSalesCount: Long = 0,
  var ticketSalesRevenueCents: Long = 0,
  var ticketRedeemedCount: Long = 0,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
