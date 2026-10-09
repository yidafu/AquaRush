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
 * 已完成营收日事实(第 3 层历史趋势指标中间表,夜间快照任务写入)
 * 原子粒度:stat_date = 订单完成时间(completed_at)归属日(PRD 01-overview §日完成订单数)。
 * 与 daily_order_facts 分离:daily_order_facts 按 created_at(下单日)记录全状态订单数与金额,
 * 仅用于订单数/状态分布;营收与已完成订单数由本表按完成日提供,避免跨日完成订单的营收丢失
 * (下单日未完成不计入、完成日又因 created_at 不在窗口而漏计)。
 * - order_count:当日进入「已完成」状态的订单数
 * - amount_cents:当日已完成订单金额合计(仅 COMPLETED 状态,金额按订单只计一次 P1-3)
 */
data class DailyRevenueFactModel(
  var id: Long? = null,
  var statDate: LocalDate = LocalDate.now(),
  var orderCount: Long = 0,
  var amountCents: Long = 0,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
