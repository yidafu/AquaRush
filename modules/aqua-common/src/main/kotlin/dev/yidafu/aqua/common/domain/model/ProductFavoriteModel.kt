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

package dev.yidafu.aqua.common.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "product_favorites")
data class ProductFavoriteModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "user_id", nullable = false)
  val userId: Long = -1,

  @Column(name = "product_id", nullable = false)
  val productId: Long = -1,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", insertable = false, updatable = false)
  val product: ProductModel? = null,

  @Column(name = "enable", nullable = false)
  val enable: Boolean = true,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null
) : SoftDeletable
