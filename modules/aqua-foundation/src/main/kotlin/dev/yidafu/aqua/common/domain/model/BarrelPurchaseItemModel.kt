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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 购桶单明细(G06:规格/单价/数量快照——价目配置变更不影响已生成购桶单)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "barrel_purchase_items",
  indexes = [
    Index(name = "idx_barrel_purchase_items_purchase", columnList = "purchase_id"),
  ],
)
open class BarrelPurchaseItemModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "purchase_id", nullable = false)
  var purchaseId: Long = 0,
  @Column(name = "barrel_fee_id", nullable = false)
  var barrelFeeId: Long = 0,
  @Column(name = "specification", nullable = false, length = 100)
  var specification: String = "",
  @Column(name = "unit_price_cents", nullable = false)
  var unitPriceCents: Long = 0,
  @Column(name = "quantity", nullable = false)
  var quantity: Int = 0,
  @Column(name = "amount_cents", nullable = false)
  var amountCents: Long = 0,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
