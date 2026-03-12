/*
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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.OrderOperationType
import dev.yidafu.aqua.common.domain.model.OperatorType

/**
 * 订单操作记录服务接口
 */
interface OrderOperationService {
  /**
   * 记录订单操作
   *
   * @param orderId 订单ID
   * @param operationType 操作类型
   * @param operatorId 操作人ID (可选)
   * @param operatorType 操作人类型
   * @param description 操作描述 (可选)
   * @param extraData 额外数据，JSON格式 (可选)
   * @return 创建的操作记录
   */
  fun recordOperation(
    orderId: Long,
    operationType: OrderOperationType,
    operatorType: OperatorType,
    operatorId: Long? = null,
    description: String? = null,
    extraData: String? = null,
  ): OrderOperationModel

  /**
   * 获取订单的操作历史
   *
   * @param orderId 订单ID
   * @return 操作记录列表，按时间倒序
   */
  fun getOrderOperations(orderId: Long): List<OrderOperationModel>

  /**
   * 统计订单的操作记录数量
   *
   * @param orderId 订单ID
   * @return 操作记录数量
   */
  fun countOrderOperations(orderId: Long): Long
}
