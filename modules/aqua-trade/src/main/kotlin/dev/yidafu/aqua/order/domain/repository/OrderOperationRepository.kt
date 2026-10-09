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

/**
 * 订单操作流水仓库(订单时间线)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法
 * (findByOrderIdAndOperationType 无人调用已删除);
 * order_operations 为裸表(无软删列,不作 is_deleted 过滤)。
 */
interface OrderOperationRepository {
  /**
   * 根据订单ID查询操作记录,按创建时间倒序排列(id 破平)。
   */
  fun findByOrderIdOrderByCreatedAtDesc(orderId: Long): List<OrderOperationModel>

  /**
   * 统计订单的操作记录数量。
   */
  fun countByOrderId(orderId: Long): Long

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: OrderOperationModel): OrderOperationModel
}
