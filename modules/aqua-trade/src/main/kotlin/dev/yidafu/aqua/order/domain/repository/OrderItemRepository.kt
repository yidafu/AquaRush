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

import dev.yidafu.aqua.common.domain.model.OrderItemModel

/**
 * 订单商品明细仓库(F-ORD-004 多商品下单):
 * order_items 为订单商品权威明细(orders 表仅存聚合金额/营销快照);
 * 查询按软删过滤(与既有仓库一致,见 OrderRepository 等)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法;saveAll 为单事务批量 upsert。
 */
interface OrderItemRepository {
  fun findByOrderId(orderId: Long): List<OrderItemModel>

  fun findByProductId(productId: Long): List<OrderItemModel>

  /** 单条 upsert:无 id 或 id 不存在时插入(雪花 id 回填),否则按 id 更新业务列。 */
  fun save(entity: OrderItemModel): OrderItemModel

  /** 单事务批量 upsert(全部成功或全部回滚),返回回填 id 后的实体列表。 */
  fun saveAll(entities: List<OrderItemModel>): List<OrderItemModel>
}
