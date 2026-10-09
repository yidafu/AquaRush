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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel

/**
 * 购桶单明细仓储(G06:规格/单价/数量快照)。
 * JPA→Exposed 迁移 P4:仅保留实际被调用的方法;查询过滤 is_deleted。
 */
interface BarrelPurchaseItemRepository {
  fun findAllByPurchaseIdOrderByIdAsc(purchaseId: Long): List<BarrelPurchaseItemModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: BarrelPurchaseItemModel): BarrelPurchaseItemModel

  /** 批量 upsert(单事务原子)。 */
  fun saveAll(entities: List<BarrelPurchaseItemModel>): List<BarrelPurchaseItemModel>
}
