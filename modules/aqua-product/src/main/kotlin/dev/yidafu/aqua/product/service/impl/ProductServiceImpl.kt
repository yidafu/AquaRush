package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatistics
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.common.utils.MoneyUtils
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.mapper.ProductStatusMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ProductServiceImpl(
  val productRepository: ProductRepository,
) : ProductService {
  override fun findById(id: Long): ProductModel? = productRepository.findById(id).orElse(null)

  @Transactional
  override fun createProduct(request: CreateProductInput): ProductModel {
    // Convert prices from yuan to cents for storage
    val priceCents = MoneyUtils.toCents(BigDecimal.valueOf(request.price).divide(BigDecimal(100)))
    val originalPriceCents =
      request.originalPrice?.let {
        MoneyUtils.toCents(
          BigDecimal.valueOf(it).divide(
            BigDecimal(
              100,
            ),
          ),
        )
      }
    val depositPriceCents =
      request.depositPrice?.let { MoneyUtils.toCents(BigDecimal.valueOf(it).divide(BigDecimal(100))) }

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
        status = ProductStatusMapper.map(request.status),
        sortOrder = request.sortOrder,
        tags = request.tags,
        detailContent = request.detailContent,
        certificateImages = request.certificateImages,
        deliverySettings = request.deliverySettings,
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
    status: ProductStatus? = null,
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
    stock?.let { product.stock = it }
    salesVolume?.let { product.salesVolume = it }
    sortOrder?.let { product.sortOrder = it }
    tags?.let { product.tags = it }
    detailContent?.let { product.detailContent = it }
    certificateImages?.let { product.certificateImages = it }
    deliverySettings?.let { product.deliverySettings = it }
    status?.let { product.status = ProductStatusMapper.map(it) }

    return productRepository.save(product)
  }

  @Transactional
  override fun updateProductStatus(
    productId: Long,
    status: ProductStatus,
  ): ProductModel {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.status = ProductStatusMapper.map(status)
    return productRepository.save(product)
  }

  // Service methods for stock management
  @Transactional
  override fun increaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean {
    val product = productRepository.findById(productId).orElse(null)
    return if (product != null) {
      product.stock += quantity
      if (product.stock > 0 && product.status == ProductModelStatus.OUT_OF_STOCK) {
        product.status = ProductModelStatus.ONLINE
      }
      productRepository.save(product)
      true
    } else {
      false
    }
  }

  @Transactional
  override fun decreaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean {
    val product = productRepository.findById(productId).orElse(null)
    return if (product != null && product.stock >= quantity) {
      product.stock -= quantity
      if (product.stock == 0) {
        product.status = ProductModelStatus.OUT_OF_STOCK
      }
      productRepository.save(product)
      true
    } else {
      false
    }
  }

  override fun productsPaginated(
    query: ProductSearchRequest,
    pageable: Pageable,
  ): Page<ProductModel> = productRepository.searchProducts(query, pageable)

  fun getProductStatistics(): ProductStatistics {
    val allProducts = productRepository.findAll()
    val onlineProducts = allProducts.filter { it.status == ProductModelStatus.ONLINE }
    val offlineProducts = allProducts.filter { it.status == ProductModelStatus.OFFLINE }
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
      averagePrice = averagePrice,
      productRanking = emptyList(),
      favoriteStatistics = emptyList(),
      totalSales = 0,
      totalFavorites = 0,
    )
  }

  // Sales volume tracking
  @Transactional
  fun incrementSalesVolume(
    productId: Long,
    quantity: Int,
  ) {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.salesVolume += quantity
    productRepository.save(product)
  }

  @Transactional
  fun updateSalesVolume(
    productId: Long,
    volume: Int,
  ) {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }
    product.salesVolume = volume
    productRepository.save(product)
  }
}
