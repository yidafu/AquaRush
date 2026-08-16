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

import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus

/**
 * 订单状态分布项(PRD F-STA-005 G37)
 * 按所选时间范围各状态订单数聚合;各状态合计 = 订单总数(totalOrders)。
 */
data class OrderStatusDistributionItemDTO(
  val status: OrderModelStatus,
  val count: Long,
)

/**
 * 客单价分布区间(PRD F-STA-005 G37)
 * 按订单金额 amountCents 分桶聚合订单数;各区间合计 = 订单总数(totalOrders)。
 * 金额单位分;fromCents 含下限、toCents 不含上限,null 表示开区间。
 */
data class OrderValueBucketDTO(
  val fromCents: Long?,
  val toCents: Long?,
  val label: String,
  val count: Long = 0,
)

/**
 * 高峰时段分析项(PRD F-STA-005 G37)
 * 按订单创建时间(createdAt)小时 0-23 聚合;各小时合计 = 订单总数(totalOrders)。
 */
data class PeakHourDTO(
  val hour: Int,
  val count: Long,
)
