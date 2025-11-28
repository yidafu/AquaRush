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

package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  fun findByStatus(status: ProductStatus): List<Product>

  fun decreaseStock(
    productId: Long,
    quantity: Int,
  ): Int {
    val product = findById(productId).orElse(null)
    return if (product != null && product.stock >= quantity) {
      product.stock -= quantity
      save(product)
      1
    } else {
      0
    }
  }

  fun increaseStock(
    productId: Long,
    quantity: Int,
  ): Int {
    val product = findById(productId).orElse(null)
    return if (product != null) {
      product.stock += quantity
      save(product)
      1
    } else {
      0
    }
  }
}
