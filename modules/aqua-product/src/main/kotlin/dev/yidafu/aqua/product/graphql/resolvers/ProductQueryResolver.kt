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

import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.service.ProductService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProductQueryResolver(
  private val productService: ProductService,
) {
  @QueryMapping
  fun product(
    @Argument id: Long,
  ): Product? = productService.findById(id)

  @QueryMapping
  fun products(): List<Product> = productService.findAll()

  @QueryMapping
  fun activeProducts(): List<Product> = productService.findOnlineProducts()
}
