package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.delivery.domain.model.DeliveryWorker
import dev.yidafu.aqua.delivery.domain.model.WorkerOnlineStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeliveryWorkerRepository : JpaRepository<DeliveryWorker, UUID> {
    
    fun findByWechatOpenId(wechatOpenId: String): DeliveryWorker?
    
    fun findByOnlineStatus(status: WorkerOnlineStatus): List<DeliveryWorker>
}
