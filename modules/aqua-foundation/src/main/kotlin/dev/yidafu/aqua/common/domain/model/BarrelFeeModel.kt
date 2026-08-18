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
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 水桶费价目(G06:一次性塑料水桶费——独立价目表,按规格配置,与商品解耦):
 * 用户端「买水桶」按启用价目选购,独立支付购桶(与商品订单不捆绑);价目停用不影响已生成购桶单。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "barrel_fees",
  indexes = [
    Index(name = "idx_barrel_fees_status_sort", columnList = "status,sort_order"),
  ],
)
open class BarrelFeeModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "specification", nullable = false, unique = true, length = 100)
  var specification: String = "",
  @Column(name = "price_cents", nullable = false)
  var priceCents: Long = 0,
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: BarrelFeeStatus = BarrelFeeStatus.ENABLED,
  @Column(name = "sort_order", nullable = false)
  var sortOrder: Int = 999,
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
