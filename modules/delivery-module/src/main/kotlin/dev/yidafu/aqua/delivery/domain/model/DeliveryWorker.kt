package dev.yidafu.aqua.delivery.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "delivery_workers")
data class DeliveryWorker(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "wechat_openid", unique = true, nullable = false)
    val wechatOpenId: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "phone", unique = true, nullable = false)
    var phone: String,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: WorkerStatus = WorkerStatus.OFFLINE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class WorkerStatus {
    ONLINE,   // 上线
    OFFLINE   // 下线
}
