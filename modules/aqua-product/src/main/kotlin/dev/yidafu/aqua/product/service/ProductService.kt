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
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ProductService(
  val productRepository: ProductRepository,
) {
  fun findById(id: Long): Product? = productRepository.findById(id).orElse(null)

  fun findAll(): List<Product> = productRepository.findAll()

  fun findOnlineProducts(): List<Product> = productRepository.findByStatus(ProductStatus.Online)

  fun findOnlineProducts(pageable: Pageable): org.springframework.data.domain.Page<Product> {
    return findByStatus(ProductStatus.Online, pageable)
  }

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

  // Additional methods for queries
  fun findByNameContainingAndStatus(keyword: String, status: ProductStatus, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.name.contains(keyword, ignoreCase = true) && it.status == status
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByNameContaining(keyword: String, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.name.contains(keyword, ignoreCase = true)
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByStatus(status: ProductStatus, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findByStatus(status)
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findAll(pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll()
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findLowStockProducts(threshold: Int, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter { it.stock <= threshold }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByCategory(category: String, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.detailImages?.contains(category) == true
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByPriceBetween(minPrice: java.math.BigDecimal, maxPrice: java.math.BigDecimal, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.price >= minPrice && it.price <= maxPrice
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun count(): Long = productRepository.count()

  fun countByStatus(status: ProductStatus): Long = productRepository.findByStatus(status).size.toLong()

  fun countLowStockProducts(threshold: Int): Long = productRepository.findAll().count { it.stock <= threshold }.toLong()

  // Additional methods for client queries
  fun findByCategoryAndNameContainingAndStatus(category: String, keyword: String, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.detailImages?.contains(category) == true &&
        it.name.contains(keyword, ignoreCase = true) &&
        it.status == ProductStatus.Online
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByCategoryAndStatus(category: String, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.detailImages?.contains(category) == true && it.status == ProductStatus.Online
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByNameContainingAndStatus(keyword: String, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.name.contains(keyword, ignoreCase = true) && it.status == ProductStatus.Online
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByPriceBetweenAndStatus(minPrice: java.math.BigDecimal, maxPrice: java.math.BigDecimal, pageable: Pageable): org.springframework.data.domain.Page<Product> {
    val products = productRepository.findAll().filter {
        it.price >= minPrice && it.price <= maxPrice && it.status == ProductStatus.Online
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findPopularProducts(pageable: Pageable, limit: Int): org.springframework.data.domain.Page<Product> {
    // Simplified: just return online products ordered by name (would normally sort by popularity)
    val products = productRepository.findByStatus(ProductStatus.Online).sortedBy { it.name }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, minOf(products.size, limit))
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findNewProducts(pageable: Pageable, days: Int): org.springframework.data.domain.Page<Product> {
    // Simplified: return all online products (would normally filter by creation date)
    return findByStatus(ProductStatus.Online, pageable)
  }

  fun findRecommendedProducts(pageable: Pageable, limit: Int): org.springframework.data.domain.Page<Product> {
    // Simplified: return first few online products (would normally have recommendation logic)
    val products = productRepository.findByStatus(ProductStatus.Online).take(limit)
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return org.springframework.data.domain.PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findAllCategories(): List<String> {
    // Simplified: extract categories from detailImages (would normally have a proper category field)
    return productRepository.findAll()
        .mapNotNull { it.detailImages }
        .flatMap { images -> images.split(",").map { it.trim() } }
        .distinct()
  }

  fun getPriceRangeStatistics(): List<dev.yidafu.aqua.client.product.resolvers.ClientProductQueryResolver.Companion.PriceRange> {
    // Simplified: return basic price ranges
    val allProducts = productRepository.findByStatus(ProductStatus.Online)
    val prices = allProducts.map { it.price }

    if (prices.isEmpty()) return emptyList()

    val min = prices.minOrNull() ?: java.math.BigDecimal.ZERO
    val max = prices.maxOrNull() ?: java.math.BigDecimal.ZERO
    val step = (max - min).divide(java.math.BigDecimal(4)) // Divide into 4 ranges

    return (0..3).map { i ->
        val rangeMin = min + step * i.toBigDecimal()
        val rangeMax = if (i == 3) max else min + step * (i + 1).toBigDecimal()
        val count = allProducts.count { it.price >= rangeMin && it.price < rangeMax }.toLong()
        dev.yidafu.aqua.client.product.resolvers.ClientProductQueryResolver.Companion.PriceRange(
            min = rangeMin,
            max = rangeMax,
            count = count,
            label = "${rangeMin}-${rangeMax}"
        )
    }
  }
}
