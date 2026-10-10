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

import dev.yidafu.aqua.common.util.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDateTime

open class OrderItemModel(
  var id: Long? = null,
  val orderId: Long,
  val productId: Long,
  val quantity: Int,
  val unitPriceCents: Long,
  val totalPriceCents: Long,
  val productSnapshot: String, // JSON string of product snapshot
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
  // Backward compatibility property
  val unitPrice: BigDecimal
    get() = MoneyUtils.fromCents(unitPriceCents)

  // Backward compatibility property
  val totalPrice: BigDecimal
    get() = MoneyUtils.fromCents(totalPriceCents)

  // Companion object for factory methods
  companion object {
    fun create(
      orderId: Long,
      productId: Long,
      quantity: Int,
      unitPriceCents: Long,
      productSnapshot: String,
    ): OrderItemModel {
      val totalPriceCents = unitPriceCents * quantity
      return OrderItemModel(
        orderId = orderId,
        productId = productId,
        quantity = quantity,
        unitPriceCents = unitPriceCents,
        totalPriceCents = totalPriceCents,
        productSnapshot = productSnapshot,
      )
    }
  }
}
