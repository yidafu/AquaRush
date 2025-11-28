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

package dev.yidafu.aqua.product.graphql.resolvers

import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.UpdateProductInput
import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.service.ProductService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class ProductMutationResolver(
  private val productService: ProductService,
) {
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun createProduct(
    @Argument @Valid input: CreateProductInput,
  ): Product =
    productService.createProduct(
      name = input.name,
      price = input.price,
      coverImageUrl = input.coverImageUrl,
      detailImages = null,
      description = input.description,
      stock = input.stock,
    )

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun updateProduct(
    @Argument id: Long,
    @Argument @Valid input: UpdateProductInput,
  ): Product =
    productService.updateProduct(
      productId = id,
      name = input.name,
      price = input.price,
      coverImageUrl = input.coverImageUrl,
      detailImages = null,
      description = input.description,
      stock = input.stock,
    )

  // Delete method not implemented in ProductService yet, let's skip it for now
  // @MutationMapping
  // @PreAuthorize("hasRole('ADMIN')")
  // fun deleteProduct(@Argument id: Long): Boolean

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun increaseStock(
    @Argument productId: Long,
    @Argument quantity: Int,
  ): String {
    productService.increaseStock(productId, quantity)
    return "Stock increased successfully"
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun decreaseStock(
    @Argument productId: Long,
    @Argument quantity: Int,
  ): Boolean = productService.decreaseStock(productId, quantity)
}
