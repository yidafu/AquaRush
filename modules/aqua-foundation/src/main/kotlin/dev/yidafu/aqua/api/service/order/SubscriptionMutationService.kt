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

package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.api.dto.CreateSubscriptionRequest
import dev.yidafu.aqua.common.domain.model.SubscriptionModel

/**
 * 周期订购计划管理服务(R-ORD-13,F-ORD-026 后台入口):
 * 创建(绑定用户+地址)/暂停/恢复/终止;终止不可恢复(需新建)。
 * 到期自动生成订单由 SubscriptionScheduler 驱动,不在本接口内。
 */
interface SubscriptionMutationService {
  /** 创建计划:校验地址归属/商品在售/周期参数,计算首次执行日期后落库 */
  fun create(
    adminId: Long,
    request: CreateSubscriptionRequest,
  ): SubscriptionModel

  /** 暂停(ACTIVE → PAUSED,期间不生成订单) */
  fun pause(id: Long): SubscriptionModel

  /** 恢复(PAUSED → ACTIVE;暂停期间错过的期次不补,按下一生效日重算) */
  fun resume(id: Long): SubscriptionModel

  /** 终止(ACTIVE/PAUSED → TERMINATED,不可恢复;已生成订单不受影响) */
  fun terminate(id: Long): SubscriptionModel
}
