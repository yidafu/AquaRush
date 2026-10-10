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

import java.time.LocalDateTime

/**
 * 购桶单明细(G06:规格/单价/数量快照——价目配置变更不影响已生成购桶单)。
 */
open class BarrelPurchaseItemModel(
  var id: Long? = null,
  var purchaseId: Long = 0,
  var barrelFeeId: Long = 0,
  var specification: String = "",
  var unitPriceCents: Long = 0,
  var quantity: Int = 0,
  var amountCents: Long = 0,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
