package dev.yidafu.aqua.user.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "addresses")
data class Address(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "receiver_name", nullable = false)
    var receiverName: String,

    @Column(name = "phone", nullable = false)
    var phone: String,

    @Column(name = "province", nullable = false)
    var province: String,

    @Column(name = "city", nullable = false)
    var city: String,

    @Column(name = "district", nullable = false)
    var district: String,

    @Column(name = "detail_address", nullable = false, length = 500)
    var detailAddress: String,

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,

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
