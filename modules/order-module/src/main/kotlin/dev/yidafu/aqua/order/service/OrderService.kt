package dev.yidafu.aqua.order.service

import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.order.domain.model.Order
import dev.yidafu.aqua.order.domain.model.OrderStatus
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository
) {
    
    @Transactional
    fun createOrder(order: Order): Order {
        // TODO: 实现订单创建逻辑
        // 1. 验证产品库存
        // 2. 验证地址是否在配送范围
        // 3. 创建订单
        // 4. 发起支付
        return orderRepository.save(order)
    }
    
    fun getOrderById(orderId: UUID): Order {
        return orderRepository.findById(orderId).orElseThrow {
            NotFoundException("订单不存在: $orderId")
        }
    }
    
    fun getOrderByNumber(orderNumber: String): Order {
        return orderRepository.findByOrderNumber(orderNumber)
            ?: throw NotFoundException("订单不存在: $orderNumber")
    }
    
    fun getUserOrders(userId: UUID): List<Order> {
        return orderRepository.findByUserId(userId)
    }
    
    fun getUserOrdersByStatus(userId: UUID, status: OrderStatus): List<Order> {
        return orderRepository.findByUserIdAndStatus(userId, status)
    }
    
    @Transactional
    fun cancelOrder(orderId: UUID): Order {
        val order = getOrderById(orderId)
        // TODO: 实现取消订单逻辑
        // 1. 验证订单状态
        // 2. 恢复库存
        // 3. 更新订单状态
        order.status = OrderStatus.CANCELLED
        return orderRepository.save(order)
    }
    
    @Transactional
    fun updateOrderStatus(orderId: UUID, status: OrderStatus): Order {
        val order = getOrderById(orderId)
        order.status = status
        return orderRepository.save(order)
    }
    
    fun getOrdersByStatus(status: OrderStatus): List<Order> {
        return orderRepository.findByStatus(status)
    }
}
