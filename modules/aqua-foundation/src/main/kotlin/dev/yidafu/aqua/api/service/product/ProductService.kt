package dev.yidafu.aqua.api.service.product

import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.graphql.generated.CreateProductInput
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductService {
  fun findById(id: Long): ProductModel?

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

  fun countByStatus(status: ProductStatus): Long

  fun findPopularProducts(
    pageable: Pageable,
    limit: Int,
  ): Page<ProductModel>

  fun productsPaginated(
    query: ProductSearchRequest,
    pageable: Pageable,
  ): Page<ProductModel>
}
