package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.order.domain.model.Order
import dev.yidafu.aqua.order.domain.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    
    fun findByOrderNumber(orderNumber: String): Order?
    
    fun findByUserId(userId: UUID): List<Order>
    
    fun findByUserIdAndStatus(userId: UUID, status: OrderStatus): List<Order>
    
    fun findByStatus(status: OrderStatus): List<Order>
    
    fun findByDeliveryWorkerId(deliveryWorkerId: UUID): List<Order>
}
