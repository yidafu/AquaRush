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

import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import java.time.LocalDateTime

/**
 * 订单操作记录实体类
 * 用于记录订单的操作历史（时间线），方便展示订单的相关操作记录
 */
class OrderOperationModel(
  var id: Long? = null,
  val orderId: Long = -1L,
  val operationType: OrderOperationType = OrderOperationType.ORDER_CREATED,
  val operatorId: Long? = null,
  val operatorType: OperatorType = OperatorType.SYSTEM,
  val description: String? = null,
  val extraData: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
