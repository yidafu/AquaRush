package dev.yidafu.aqua.product.service.impl

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.mapper.toDomain
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.node.ArrayNode

@Service
@Transactional(readOnly = true)
class ProductServiceImpl(
  val productRepository: ProductRepository,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
  private val bizLogger: BizLogger,
) : ProductService {
  override fun findById(id: Long): ProductModel? = productRepository.findById(id).orElse(null)

  @Transactional
  override fun createProduct(request: CreateProductInput): ProductModel {
    val product =
      ProductModel(
        name = request.name,
        subtitle = request.subtitle,
        price = request.priceCents,
        originalPrice = request.originalPriceCents,
        coverImageUrl = request.coverImageUrl,
        imageGallery = request.imageGallery,
        specification = request.specification,
        waterSource = request.waterSource,
        mineralContent = request.mineralContent,
        stock = request.stock,
        salesVolume = request.salesVolume,
        status = request.status.toDomain(),
        sortOrder = request.sortOrder,
        tags = request.tags,
        detailContent = request.detailContent,
        certificateImages = request.certificateImages,
      )
    val saved = productRepository.save(product)

    // 业务日志(F-LOG-003 PRODUCT 域):产品创建
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logProductOperation(
      productId = saved.id!!,
      operation = "PRODUCT_CREATED",
      result = "SUCCESS",
      userId = operatorId,
      username = operatorName,
      additionalData = mapOf("name" to saved.name, "priceCents" to saved.price),
    )
    return saved
  }

  // Legacy method for backward compatibility
  // Removed deprecated createProduct method to avoid conflicts

  @Transactional
  fun updateProduct(
    productId: Long,
    name: String?,
    priceCents: Long?,
    coverImageUrl: String?,
    description: String?,
    stock: Int?,
    subtitle: String? = null,
    originalPriceCents: Long? = null,
    imageGallery: ArrayNode? = null,
    specification: String? = null,
    waterSource: String? = null,
    mineralContent: String? = null,
    salesVolume: Int? = null,
    sortOrder: Int? = null,
    tags: ArrayNode? = null,
    detailContent: String? = null,
    certificateImages: ArrayNode? = null,
    status: ProductStatus? = null,
  ): ProductModel {
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { IllegalArgumentException("Product not found: $productId") }

    name?.let { product.name = it }
    subtitle?.let { product.subtitle = it }
    priceCents?.let { product.price = it }
    originalPriceCents?.let { product.originalPrice = it }
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
    status?.let { product.status = it.toDomain() }
    val saved = productRepository.save(product)

    // 业务日志(F-LOG-003 PRODUCT 域):产品更新
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logProductOperation(
      productId = saved.id!!,
      operation = "PRODUCT_UPDATED",
      result = "SUCCESS",
      userId = operatorId,
      username = operatorName,
      additionalData = mapOf("name" to saved.name),
    )
    return saved
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
    val before = product.status
    product.status = status.toDomain()
    val saved = productRepository.save(product)

    // 业务日志(F-LOG-003 PRODUCT 域):产品状态变更
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logProductOperation(
      productId = saved.id!!,
      operation = "PRODUCT_STATUS_CHANGED",
      result = "SUCCESS",
      userId = operatorId,
      username = operatorName,
      additionalData = mapOf("from" to before.name, "to" to saved.status.name),
    )
    return saved
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
