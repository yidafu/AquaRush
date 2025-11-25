package dev.yidafu.aqua.product.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(name = "cover_image_url", nullable = false)
    var coverImageUrl: String,

    @Column(name = "detail_images", columnDefinition = "jsonb")
    var detailImages: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "stock", nullable = false)
    var stock: Int = 0,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ProductStatus = ProductStatus.OFFLINE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

enum class ProductStatus {
    ONLINE,   // 上架
    OFFLINE   // 下架
}
