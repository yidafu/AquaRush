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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderOperationRepository : JpaRepository<OrderOperationModel, Long> {
  /**
   * 根据订单ID查询操作记录，按创建时间倒序排列
   */
  fun findByOrderIdOrderByCreatedAtDesc(orderId: Long): List<OrderOperationModel>

  /**
   * 根据订单ID查询特定类型的操作记录
   */
  fun findByOrderIdAndOperationType(
    orderId: Long,
    operationType: OrderOperationType,
  ): List<OrderOperationModel>

  /**
   * 统计订单的操作记录数量
   */
  fun countByOrderId(orderId: Long): Long
}
