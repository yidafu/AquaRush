package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

interface ProductService {
  fun findById(id: Long): ProductModel?

  fun findAll(): List<ProductModel>

  fun findOnlineProducts(pageable: Pageable): Page<ProductModel>

  fun createProduct(request: CreateProductInput): ProductModel

  fun updateProductStatus(
    productId: Long,
    status: ProductStatus,
  ): ProductModel

  fun increaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean

  fun decreaseStock(
    productId: Long,
    quantity: Int,
  ): Boolean

  fun findByStatus(
    status: ProductStatus,
    pageable: Pageable,
  ): Page<ProductModel>

  fun findAll(pageable: Pageable): Page<ProductModel>

  fun findByCategory(
    category: String,
    pageable: Pageable,
  ): Page<ProductModel>

  fun findByPriceBetween(
    minPriceYuan: BigDecimal,
    maxPriceYuan: BigDecimal,
    pageable: Pageable,
  ): Page<ProductModel>

  fun count(): Long

  fun countByStatus(status: ProductStatus): Long

  fun findPopularProducts(
    pageable: Pageable,
    limit: Int,
  ): Page<ProductModel>

  fun findAllCategories(): List<String>
}
