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

package dev.yidafu.aqua.deposit.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 购桶单明细仓储(G06:规格/单价/数量快照)
 */
@Repository
interface BarrelPurchaseItemRepository : JpaRepository<BarrelPurchaseItemModel, Long> {
  fun findAllByPurchaseIdOrderByIdAsc(purchaseId: Long): List<BarrelPurchaseItemModel>
}
