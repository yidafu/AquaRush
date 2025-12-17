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

import dev.yidafu.aqua.client.product.resolvers.ClientProductQueryResolver
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.graphql.generated.ProductUpdateRequestInput
import dev.yidafu.aqua.common.utils.MoneyUtils
import dev.yidafu.aqua.product.domain.model.ProductModel
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ProductService(
  val productRepository: ProductRepository,
) {
  fun findById(id: Long): ProductModel? = productRepository.findById(id).orElse(null)

  fun findAll(): List<ProductModel> = productRepository.findAll()

  fun findOnlineProducts(): List<ProductModel> = productRepository.findByStatus(ProductStatus.ONLINE)

  fun findOnlineProducts(pageable: Pageable): Page<ProductModel> {
    return findByStatus(ProductStatus.ONLINE, pageable)
  }

  @Transactional
  fun createProduct(request: CreateProductInput): ProductModel {
    // Convert prices from yuan to cents for storage
    val priceCents = MoneyUtils.toCents(BigDecimal.valueOf(request.price).divide(BigDecimal(100)))
    val originalPriceCents = request.originalPrice?.let { MoneyUtils.toCents(BigDecimal.valueOf(it).divide(BigDecimal(100))) }
    val depositPriceCents = request.depositPrice?.let { MoneyUtils.toCents(BigDecimal.valueOf(it).divide(BigDecimal(100))) }

    val product =
      ProductModel(
        name = request.name,
        subtitle = request.subtitle,
        price = priceCents,
        originalPrice = originalPriceCents,
        depositPrice = depositPriceCents,
        coverImageUrl = request.coverImageUrl,
        imageGallery = request.imageGallery,
        specification = request.specification,
        waterSource = request.waterSource,
        mineralContent = request.mineralContent,
        stock = request.stock,
        salesVolume = request.salesVolume,
        status = request.status,
        sortOrder = request.sortOrder,
        tags = request.tags,
        detailContent = request.detailContent,
        certificateImages = request.certificateImages,
        deliverySettings = request.deliverySettings,
        isDeleted = false
      )
    return productRepository.save(product)
  }

  // Legacy method for backward compatibility
  // Removed deprecated createProduct method to avoid conflicts

  @Transactional
  fun updateProduct(
    productId: Long,
    name: String?,
    priceYuan: BigDecimal?,
    coverImageUrl: String?,
    description: String?,
    stock: Int?,
    subtitle: String? = null,
    originalPriceYuan: BigDecimal? = null,
    depositPriceYuan: BigDecimal? = null,
    imageGallery: ArrayNode? = null,
    specification: String? = null,
    waterSource: String? = null,
    mineralContent: String? = null,
    salesVolume: Int? = null,
    sortOrder: Int? = null,
    tags: ArrayNode? = null,
    detailContent: String? = null,
    certificateImages: ArrayNode? = null,
    deliverySettings: ObjectNode? = null,
    status: ProductStatus? = null
  ): ProductModel {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }

    name?.let { product.name = it }
    subtitle?.let { product.subtitle = it }
    priceYuan?.let { product.price = MoneyUtils.toCents(priceYuan) }
    originalPriceYuan?.let { product.originalPrice = MoneyUtils.toCents(originalPriceYuan) }
    depositPriceYuan?.let { product.depositPrice = MoneyUtils.toCents(depositPriceYuan) }
    coverImageUrl?.let { product.coverImageUrl = it }
    imageGallery?.let { product.imageGallery = it }
    specification?.let { product.specification = it }
    waterSource?.let { product.waterSource = it }
    mineralContent?.let { product.mineralContent = it }
    imageGallery?.let { product.imageGallery = it }
    stock?.let { product.stock = it }
    salesVolume?.let { product.salesVolume = it }
    sortOrder?.let { product.sortOrder = it }
    tags?.let { product.tags = it }
    detailContent?.let { product.detailContent = it }
    certificateImages?.let { product.certificateImages = it }
    deliverySettings?.let { product.deliverySettings = it }
    status?.let { product.status = it }

    return productRepository.save(product)
  }

  @Transactional
  fun updateProductStatus(
    productId: Long,
    status: ProductStatus,
  ): ProductModel {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.status = status
    return productRepository.save(product)
  }

  // Service methods for stock management
  @Transactional
  fun increaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean {
    val product = productRepository.findById(productId).orElse(null)
    return if (product != null) {
      product.stock += quantity
      if (product.stock > 0 && product.status == ProductStatus.OUT_OF_STOCK) {
        product.status = ProductStatus.ONLINE
      }
      productRepository.save(product)
      true
    } else {
      false
    }
  }

  @Transactional
  fun decreaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean {
    val product = productRepository.findById(productId).orElse(null)
    return if (product != null && product.stock >= quantity) {
      product.stock -= quantity
      if (product.stock == 0) {
        product.status = ProductStatus.OUT_OF_STOCK
      }
      productRepository.save(product)
      true
    } else {
      false
    }
  }

  // Additional methods for queries
  fun findByNameContainingAndStatus(keyword: String, status: ProductStatus, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter {
        it.name.contains(keyword, ignoreCase = true) && it.status == status
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByNameContaining(keyword: String, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter {
        it.name.contains(keyword, ignoreCase = true)
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByStatus(status: ProductStatus, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findByStatus(status)
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findAll(pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll()
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findLowStockProducts(threshold: Int, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter { it.stock <= threshold }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByCategory(category: String, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter {
        it.getImageGalleryAsList().any { url -> url.contains(category, ignoreCase = true) }
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByPriceBetween(minPriceYuan: BigDecimal, maxPriceYuan: BigDecimal, pageable: Pageable):  Page<ProductModel> {
    // Convert price ranges from yuan to cents for comparison
    val minPriceCents = MoneyUtils.toCents(minPriceYuan)
    val maxPriceCents = MoneyUtils.toCents(maxPriceYuan)

    val products = productRepository.findAll().filter {
        it.price >= minPriceCents && it.price <= maxPriceCents
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun count(): Long = productRepository.count()

  fun countByStatus(status: ProductStatus): Long = productRepository.findByStatus(status).size.toLong()

  fun countLowStockProducts(threshold: Int): Long = productRepository.findAll().count { it.stock <= threshold }.toLong()

  // Additional methods for client queries
  fun findByCategoryAndNameContainingAndStatus(category: String, keyword: String, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter {
        it.getImageGalleryAsList().any { url -> url.contains(category, ignoreCase = true) } &&
        it.name.contains(keyword, ignoreCase = true) &&
        it.status == ProductStatus.ONLINE
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByCategoryAndStatus(category: String, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter {
        it.getImageGalleryAsList().any { url -> url.contains(category, ignoreCase = true) } && it.status == ProductStatus.ONLINE
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByNameContainingAndStatus(keyword: String, pageable: Pageable):  Page<ProductModel> {
    val products = productRepository.findAll().filter {
        it.name.contains(keyword, ignoreCase = true) && it.status == ProductStatus.ONLINE
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findByPriceBetweenAndStatus(minPriceYuan: BigDecimal, maxPriceYuan: BigDecimal, pageable: Pageable):  Page<ProductModel> {
    // Convert price ranges from yuan to cents for comparison
    val minPriceCents = MoneyUtils.toCents(minPriceYuan)
    val maxPriceCents = MoneyUtils.toCents(maxPriceYuan)

    val products = productRepository.findAll().filter {
        it.price in minPriceCents..maxPriceCents && it.status == ProductStatus.ONLINE
    }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findPopularProducts(pageable: Pageable, limit: Int):  Page<ProductModel> {
    // Simplified: just return online products ordered by name (would normally sort by popularity)
    val products = productRepository.findByStatus(ProductStatus.ONLINE).sortedBy { it.name }
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, minOf(products.size, limit))
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findNewProducts(pageable: Pageable, days: Int):  Page<ProductModel> {
    // Simplified: return all online products (would normally filter by creation date)
    return findByStatus(ProductStatus.ONLINE, pageable)
  }

  fun findRecommendedProducts(pageable: Pageable, limit: Int):  Page<ProductModel> {
    // Simplified: return first few online products (would normally have recommendation logic)
    val products = productRepository.findByStatus(ProductStatus.ONLINE).take(limit)
    val start = pageable.pageNumber * pageable.pageSize
    val end = minOf(start + pageable.pageSize, products.size)
    val pageContent = if (start < products.size) products.subList(start, end) else emptyList()
    return  PageImpl(pageContent, pageable, products.size.toLong())
  }

  fun findAllCategories(): List<String> {
    // Simplified: extract categories from imageGallery (would normally have a proper category field)
    return productRepository.findAll()
        .flatMap { product -> product.getImageGalleryAsList() }
        .map { url -> url.trim() }
        .distinct()
  }

  // New methods for admin functionality
  @Transactional
  fun batchUpdateProducts(updates: List<ProductUpdateRequestInput>): List<ProductModel> {
    return updates.map { update ->
      updateProduct(
        productId = update.id,
        name = update.name,
        priceYuan = update.price?.let { MoneyUtils.fromCents(it) },
        coverImageUrl = update.coverImageUrl,
        // detailImages字段在ProductUpdateRequestInput中不存在
        // description字段在ProductUpdateRequestInput中不存在
        stock = update.stock,
        subtitle = update.subtitle,
        originalPriceYuan = update.originalPrice?.let { MoneyUtils.fromCents(it) },
        depositPriceYuan = update.depositPrice?.let { MoneyUtils.fromCents(it) },
        imageGallery = update.imageGallery,
        specification = update.specification,
        waterSource = update.waterSource,
        mineralContent = update.mineralContent,
        salesVolume = update.salesVolume,
        sortOrder = update.sortOrder,
        tags = update.tags,
        detailContent = update.detailContent,
        certificateImages = update.certificateImages,
        deliverySettings = update.deliverySettings,
        description = TODO()  // isDeleted字段在ProductUpdateRequestInput中不存在
      )
    }
  }

  fun getProductsByStatus(status: ProductStatus): List<ProductModel> {
    return productRepository.findByStatus(status)
  }

  fun getLowStockProducts(threshold: Int): List<ProductModel> {
    return productRepository.findAll().filter { it.stock <= threshold }
  }

  fun getProductStatistics(): ProductStatistics {
    val allProducts = productRepository.findAll()
    val onlineProducts = allProducts.filter { it.status == ProductStatus.ONLINE }
    val offlineProducts = allProducts.filter { it.status == ProductStatus.OFFLINE }
    val lowStockThreshold = 10 // Default threshold
    val lowStockProducts = allProducts.filter { it.stock <= lowStockThreshold }

    val totalValue = allProducts.sumOf { it.price }
    val averagePrice = if (allProducts.isNotEmpty()) totalValue / allProducts.size else 0L

    return ProductStatistics(
      totalProducts = allProducts.size,
      onlineProducts = onlineProducts.size,
      offlineProducts = offlineProducts.size,
      lowStockProducts = lowStockProducts.size,
      totalValue = totalValue,
      averagePrice = averagePrice
    )
  }

  fun getPriceRangeStatistics(): List<ClientProductQueryResolver.Companion.PriceRange> {
    // Simplified: return basic price ranges
    val allProducts = productRepository.findByStatus(ProductStatus.ONLINE)

    if (allProducts.isEmpty()) return emptyList()

    // Convert prices from cents to yuan for statistics
    val pricesYuan = allProducts.map { MoneyUtils.fromCents(it.price) }

    val min = pricesYuan.minOrNull() ?: BigDecimal.ZERO
    val max = pricesYuan.maxOrNull() ?: BigDecimal.ZERO
    val step = (max - min).divide(BigDecimal(4)) // Divide into 4 ranges

    return (0..3).map { i ->
        val rangeMin = min + step * i.toBigDecimal()
        val rangeMax = if (i == 3) max else min + step * (i + 1).toBigDecimal()

        // Convert ranges back to cents for comparison
        val rangeMinCents = MoneyUtils.toCents(rangeMin)
        val rangeMaxCents = MoneyUtils.toCents(rangeMax)

        val count = allProducts.count {
          val priceCents = it.price
          priceCents >= rangeMinCents && (i == 3 || priceCents < rangeMaxCents)
        }.toLong()

        ClientProductQueryResolver.Companion.PriceRange(
            min = rangeMin,
            max = rangeMax,
            count = count,
            label = "${rangeMin}-${rangeMax}"
        )
    }
  }

  // Enhanced methods for new product functionality

  // Sales volume tracking
  @Transactional
  fun incrementSalesVolume(productId: Long, quantity: Int) {
    val product = productRepository.findById(productId)
      .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.salesVolume += quantity
    productRepository.save(product)
  }

  fun getTopSalesProducts(limit: Int = 10): List<ProductModel> {
    return productRepository.findAllByOrderBySalesVolumeDesc()
      .filter { !it.isDeleted && it.status == ProductStatus.ONLINE }
      .take(limit)
  }

  @Transactional
  fun updateSalesVolume(productId: Long, volume: Int) {
    val product = productRepository.findById(productId)
      .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.salesVolume = volume
    productRepository.save(product)
  }

  // Advanced filtering
  fun findByWaterSource(waterSource: String): List<ProductModel> {
    return productRepository.findByWaterSourceContaining(waterSource)
      .filter { !it.isDeleted }
  }



  fun findBySalesVolumeGreaterThan(minVolume: Int): List<ProductModel> {
    return productRepository.findBySalesVolumeGreaterThan(minVolume)
      .filter { !it.isDeleted }
  }

  fun findByTagsContaining(tag: String): List<ProductModel> {
    return productRepository.findByTagsContaining(tag)
      .filter { !it.isDeleted }
  }

  // Soft delete support
  fun findActiveProducts(): List<ProductModel> {
    return productRepository.findByIsDeletedFalse()
  }

  fun findActiveProductsByStatus(status: ProductStatus): List<ProductModel> {
    return productRepository.findByIsDeletedFalseAndStatus(status)
  }

  // Sorting and ordering
  fun findAllByOrderBySalesVolumeDesc(): List<ProductModel> {
    return productRepository.findAllByOrderBySalesVolumeDesc()
      .filter { !it.isDeleted }
  }

  fun findAllByOrderBySortOrderAsc(): List<ProductModel> {
    return productRepository.findAllByOrderBySortOrderAsc()
      .filter { !it.isDeleted }
  }

  // Enhanced statistics
  fun getWaterSourceStatistics(): Map<String, Long> {
    return findActiveProducts()
      .filter { !it.waterSource.isNullOrBlank() }
      .groupBy { it.waterSource!! }
      .mapValues { it.value.size.toLong() }
  }

  fun getSpecificationStatistics(): Map<String, Long> {
    return findActiveProducts()
      .groupBy { it.specification }
      .mapValues { it.value.size.toLong() }
  }

  // Batch operations for stock management
  // Removed duplicate methods to avoid conflicts

  fun ArrayNode.contains(value: String): Boolean {
    this.forEach { node ->
        if (node.isString) {
          if (value == node.stringValue()) {
            return true
          }
        }
    }
    return false
  }
}
