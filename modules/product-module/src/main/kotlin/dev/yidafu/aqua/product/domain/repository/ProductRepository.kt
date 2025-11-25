package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.product.domain.model.Product
import dev.yidafu.aqua.product.domain.model.ProductStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {
    fun findByStatus(status: ProductStatus): List<Product>
    
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
    fun decreaseStock(productId: UUID, quantity: Int): Int
    
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :productId")
    fun increaseStock(productId: UUID, quantity: Int): Int
}
