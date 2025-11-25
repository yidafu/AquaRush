package dev.yidafu.aqua.order.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "order_number", unique = true, nullable = false)
    val orderNumber: String,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "product_id", nullable = false)
    val productId: UUID,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal,

    @Column(name = "address_id", nullable = false)
    val addressId: UUID,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    var paymentMethod: PaymentMethod? = null,

    @Column(name = "payment_transaction_id")
    var paymentTransactionId: String? = null,

    @Column(name = "payment_time")
    var paymentTime: LocalDateTime? = null,

    @Column(name = "delivery_worker_id")
    var deliveryWorkerId: UUID? = null,

    @Column(name = "delivery_photos", columnDefinition = "jsonb")
    var deliveryPhotos: String? = null,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

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

enum class OrderStatus {
    PENDING_PAYMENT,    // 待支付
    PENDING_DELIVERY,   // 待配送
    DELIVERING,         // 配送中
    COMPLETED,          // 已完成
    CANCELLED           // 已取消
}

enum class PaymentMethod {
    WECHAT_PAY         // 微信支付
}
