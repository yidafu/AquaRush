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

package dev.yidafu.aqua.order.domain.model

import dev.yidafu.aqua.common.utils.MoneyUtils
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "order_items")
data class OrderItemModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "order_id", nullable = false)
  val orderId: Long,

  @Column(name = "product_id", nullable = false)
  val productId: Long,

  @Column(name = "quantity", nullable = false)
  val quantity: Int,

  @Column(name = "unit_price_cents", nullable = false)
  val unitPriceCents: Long,


  @Column(name = "total_price_cents", nullable = false)
  val totalPriceCents: Long,


  @Column(name = "product_snapshot", nullable = false, columnDefinition = "jsonb")
  val productSnapshot: String, // JSON string of product snapshot

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now()
) {

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
      productSnapshot: String
    ): OrderItemModel {
      val totalPriceCents = unitPriceCents * quantity
      return OrderItemModel(
        orderId = orderId,
        productId = productId,
        quantity = quantity,
        unitPriceCents = unitPriceCents,
        totalPriceCents = totalPriceCents,
        productSnapshot = productSnapshot
      )
    }
  }
}
