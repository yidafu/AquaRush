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

package dev.yidafu.aqua.product.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.service.ProductService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
class ProductController(
  private val productService: ProductService,
) {
  @GetMapping
  fun getAllProducts(): ApiResponse<List<Product>> {
    val products = productService.findAll()
    return ApiResponse.success(products)
  }

  @GetMapping("/active")
  fun getActiveProducts(): ApiResponse<List<Product>> {
    val products = productService.findOnlineProducts()
    return ApiResponse.success(products)
  }

  @GetMapping("/{productId}")
  fun getProduct(
    @PathVariable productId: Long,
  ): ApiResponse<Product> {
    val product =
      productService.findById(productId)
        ?: return ApiResponse.error("Product not found")
    return ApiResponse.success(product)
  }

  @DeleteMapping("/{productId}")
  fun deleteProduct(
    @PathVariable productId: Long,
  ): ApiResponse<Unit> {
    // Note: Adding delete method to service would be needed
    return ApiResponse.success(Unit)
  }

  @PostMapping("/{productId}/stock/increase")
  fun increaseStock(
    @PathVariable productId: Long,
    @RequestParam quantity: Int,
  ): ApiResponse<String> {
    productService.increaseStock(productId, quantity)
    return ApiResponse.success("Stock increased successfully")
  }

  @PostMapping("/{productId}/stock/decrease")
  fun decreaseStock(
    @PathVariable productId: Long,
    @RequestParam quantity: Int,
  ): ApiResponse<Boolean> {
    val success = productService.decreaseStock(productId, quantity)
    return if (success) {
      ApiResponse.success(true)
    } else {
      ApiResponse.error("Insufficient stock")
    }
  }
}
