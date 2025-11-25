package dev.yidafu.aqua.delivery.domain.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "delivery_areas")
data class DeliveryArea(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "province", nullable = false)
    var province: String,

    @Column(name = "city", nullable = false)
    var city: String,

    @Column(name = "district", nullable = false)
    var district: String,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true
)
