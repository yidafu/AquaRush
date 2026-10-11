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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * 周期订购计划表(`subscriptions`,G77/F-ORD-026,PRD 04-order.md §F-ORD-026):
 * 后台为用户创建的定期配送计划,到期由调度自动生成代客下单订单并提醒送水工(R-ORD-13)。
 * 计划终止是状态(TERMINATED)而非删除,故为普通表(非软删),历史计划长期可查。
 * 列定义与 changelog V347AddSubscriptions 逐列对齐。
 */
object SubscriptionsTable : Table("aqua_subscriptions") {
  val id = long("id")
  val userId = long("user_id")
  val addressId = long("address_id")

  // 绑定送水工(可空):设置后到期定向提醒该送水工,未设置则广播提醒
  val workerId = long("worker_id").nullable()

  // WEEKLY / BIWEEKLY / MONTHLY
  val cycleType = varchar("cycle_type", 16)

  // WEEKLY/BIWEEKLY 生效星期(ISO 1=周一..7=周日,逗号分隔,如 "1,3,5");MONTHLY 为 null
  val weekdays = varchar("weekdays", 64).nullable()

  // MONTHLY 每月几号(1~31);WEEKLY/BIWEEKLY 为 null
  val monthDay = integer("month_day").nullable()

  // 配送时段(DeliveryTimeSlot:IMMEDIATE/MORNING/AFTERNOON/EVENING)
  val timeSlot = varchar("time_slot", 20)
  val startDate = date("start_date")
  val endDate = date("end_date").nullable()
  val maxPeriods = integer("max_periods").nullable()
  val executedPeriods = integer("executed_periods").default(0)

  // 调度扫描键(idx_subscription_next(status, next_run_date))
  val nextRunDate = date("next_run_date")

  // ACTIVE / PAUSED / TERMINATED(终止不可恢复)
  val status = varchar("status", 16)
  val remark = varchar("remark", 500).nullable()

  // 最近一次到期执行失败原因(不静默:后台列表可见,次日重试,R-ORD-13)
  val lastError = varchar("last_error", 500).nullable()
  val lastRunAt = datetime("last_run_at").nullable()

  // 创建计划的管理员账号 id(到期生成订单的操作者审计)
  val createdBy = long("created_by").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}

/** 周期订购计划商品明细表(`subscription_items`):生成订单时按当期营销链路重算价格。 */
object SubscriptionItemsTable : Table("aqua_subscription_items") {
  val id = long("id")
  val subscriptionId = long("subscription_id")
  val productId = long("product_id")
  val quantity = integer("quantity")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
