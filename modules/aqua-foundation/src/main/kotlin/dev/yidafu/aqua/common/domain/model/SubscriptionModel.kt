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

import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 周期订购计划(R-ORD-13,F-ORD-026):
 * 后台为用户创建的定期配送计划,到期由 [dev.yidafu.aqua.subscription.service.SubscriptionScheduler]
 * 按 `client_request_id = sub-{id}-{期次}` 幂等生成代客下单订单(现场收款 CASH),
 * 并经 SUBSCRIPTION_ORDER_CREATED 事件提醒送水工按时配送(G77 新方向:入口仅后台,不开发用户端)。
 */
data class SubscriptionModel(
  var id: Long? = null,
  var userId: Long,
  var addressId: Long,
  // 绑定送水工(可空):设置后到期定向提醒(站内消息+推送),未设置则广播提醒
  var workerId: Long? = null,
  var cycleType: SubscriptionCycleType,
  // ISO 1=周一..7=周日,逗号分隔(如 "1,3,5");MONTHLY 为 null
  var weekdays: String? = null,
  // MONTHLY 每月几号(1~31);WEEKLY/BIWEEKLY 为 null
  var monthDay: Int? = null,
  var timeSlot: DeliveryTimeSlot,
  var startDate: LocalDate,
  var endDate: LocalDate? = null,
  var maxPeriods: Int? = null,
  var executedPeriods: Int = 0,
  var nextRunDate: LocalDate,
  var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  var remark: String? = null,
  // 最近一次到期执行失败原因(不静默,后台列表可见,次日重试)
  var lastError: String? = null,
  var lastRunAt: LocalDateTime? = null,
  // 创建计划的管理员账号 id(到期生成订单的操作者审计)
  var createdBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
