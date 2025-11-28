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

package dev.yidafu.aqua.product.mapper

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.model.Product
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import java.math.BigDecimal
import java.time.LocalDateTime

// Product DTO
data class ProductDTO(
  val id: Long?,
  val name: String,
  val price: BigDecimal,
  val coverImageUrl: String,
  val detailImages: String?,
  val description: String?,
  val stock: Int,
  val status: ProductStatus,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)

@Component
object ProductMapper : ObjectMappie<Product, ProductDTO>()
