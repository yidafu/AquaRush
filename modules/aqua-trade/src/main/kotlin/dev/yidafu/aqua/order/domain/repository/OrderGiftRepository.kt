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

import dev.yidafu.aqua.common.domain.model.OrderGiftModel

/**
 * 订单赠品行仓储(R-MKT-09:赠品随单落库,配送明细/订单详情联查)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法;
 * order_gifts 为裸表(无软删列,不作 is_deleted 过滤);saveAll 为单事务批量 upsert。
 */
interface OrderGiftRepository {
  fun findByOrderId(orderId: Long): List<OrderGiftModel>

  /** 单事务批量 upsert(全部成功或全部回滚),返回回填 id 后的实体列表。 */
  fun saveAll(entities: List<OrderGiftModel>): List<OrderGiftModel>
}
