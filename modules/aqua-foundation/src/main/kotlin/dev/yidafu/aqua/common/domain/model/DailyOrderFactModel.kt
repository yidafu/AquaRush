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
 * 订单日事实(第 3 层历史趋势指标中间表,夜间快照任务写入)
 * 原子粒度:stat_date × status × delivery_worker_id
 * - order_count:订单数(orders 行数)
 * - amount_cents:订单金额合计,按订单只计一次(不随 order_items 明细重复求和,P1-3)
 * - item_quantity:商品件数合计(order_items.quantity 汇总)
 * 高阶口径(已完成营收、周/月聚合、状态分布)在查询时组合,不落最终 DTO。
 */
data class DailyOrderFactModel(
  var id: Long? = null,
  var statDate: LocalDate = LocalDate.now(),
  var status: String = "",
  var deliveryWorkerId: Long? = null,
  var orderCount: Long = 0,
  var amountCents: Long = 0,
  var itemQuantity: Long = 0,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
