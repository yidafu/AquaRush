package dev.yidafu.aqua.product.service

import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.domain.model.ProductStatus
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository
) {
    fun findById(id: UUID): Product? {
        return productRepository.findById(id).orElse(null)
    }

    fun findAll(): List<Product> {
        return productRepository.findAll()
    }

    fun findOnlineProducts(): List<Product> {
        return productRepository.findByStatus(ProductStatus.ONLINE)
    }

    @Transactional
    fun createProduct(
        name: String,
        price: BigDecimal,
        coverImageUrl: String,
        detailImages: String?,
        description: String?,
        stock: Int
    ): Product {
        val product = Product(
            name = name,
            price = price,
            coverImageUrl = coverImageUrl,
            detailImages = detailImages,
            description = description,
            stock = stock,
            status = ProductStatus.OFFLINE
        )
        return productRepository.save(product)
    }

    @Transactional
    fun updateProduct(
        productId: UUID,
        name: String?,
        price: BigDecimal?,
        coverImageUrl: String?,
        detailImages: String?,
        description: String?,
        stock: Int?
    ): Product {
        val product = productRepository.findById(productId)
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
    fun updateProductStatus(productId: UUID, status: ProductStatus): Product {
        val product = productRepository.findById(productId)
            .orElseThrow { IllegalArgumentException("Product not found: $productId") }
        product.status = status
        return productRepository.save(product)
    }

    @Transactional
    fun decreaseStock(productId: UUID, quantity: Int): Boolean {
        val updated = productRepository.decreaseStock(productId, quantity)
        return updated > 0
    }

    @Transactional
    fun increaseStock(productId: UUID, quantity: Int) {
        productRepository.increaseStock(productId, quantity)
    }
}
