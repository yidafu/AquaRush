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

package dev.yidafu.aqua.product.service

import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ProductService(
  private val productRepository: ProductRepository,
) {
  fun findById(id: Long): Product? = productRepository.findById(id).orElse(null)

  fun findAll(): List<Product> = productRepository.findAll()

  fun findOnlineProducts(): List<Product> = productRepository.findByStatus(ProductStatus.Online)

  @Transactional
  fun createProduct(
    name: String,
    price: BigDecimal,
    coverImageUrl: String,
    detailImages: String?,
    description: String?,
    stock: Int,
  ): Product {
    val product =
      Product(
        name = name,
        price = price,
        coverImageUrl = coverImageUrl,
        detailImages = detailImages,
        description = description,
        stock = stock,
        status = ProductStatus.Offline,
      )
    return productRepository.save(product)
  }

  @Transactional
  fun updateProduct(
    productId: Long,
    name: String?,
    price: BigDecimal?,
    coverImageUrl: String?,
    detailImages: String?,
    description: String?,
    stock: Int?,
  ): Product {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }

    name?.let { product.name = it }
    price?.let { product.price = it }
    coverImageUrl?.let { product.coverImageUrl = it }
    detailImages?.let { product.detailImages = it }
    description?.let { product.description = it }
    stock?.let { product.stock = it }

    return productRepository.save(product)
  }

  @Transactional
  fun updateProductStatus(
    productId: Long,
    status: ProductStatus,
  ): Product {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.status = status
    return productRepository.save(product)
  }

  @Transactional
  fun decreaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean {
    val updated = productRepository.decreaseStock(productId, quantity)
    return updated > 0
  }

  @Transactional
  fun increaseStock(
    productId: Long,
    quantity: Int,
  ) {
    productRepository.increaseStock(productId, quantity)
  }
}
