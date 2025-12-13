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

package dev.yidafu.aqua.product.domain.model

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "products")
data class ProductModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0L,

  @Column(name = "name", nullable = false)
  var name: String,

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  var price: BigDecimal,

  @Column(name = "cover_image_url", nullable = false)
  var coverImageUrl: String,

  @Column(name = "detail_images", columnDefinition = "jsonb")
  var detailImages: String? = null,

  @Column(name = "description", columnDefinition = "TEXT")
  var description: String? = null,

  @Column(name = "stock", nullable = false)
  var stock: Int = 0,

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: ProductStatus = ProductStatus.OFFLINE,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now()
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
