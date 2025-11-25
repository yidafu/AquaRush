package dev.yidafu.aqua.order.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.order.domain.model.Order
import dev.yidafu.aqua.order.domain.model.OrderStatus
import dev.yidafu.aqua.order.service.OrderService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {
    
    @PostMapping
    fun createOrder(@RequestBody order: Order): ApiResponse<Order> {
        val createdOrder = orderService.createOrder(order)
        return ApiResponse.success(createdOrder)
    }
    
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): ApiResponse<Order> {
        val order = orderService.getOrderById(orderId)
        return ApiResponse.success(order)
    }
    
    @GetMapping("/number/{orderNumber}")
    fun getOrderByNumber(@PathVariable orderNumber: String): ApiResponse<Order> {
        val order = orderService.getOrderByNumber(orderNumber)
        return ApiResponse.success(order)
    }
    
    @GetMapping("/user/{userId}")
    fun getUserOrders(@PathVariable userId: UUID): ApiResponse<List<Order>> {
        val orders = orderService.getUserOrders(userId)
        return ApiResponse.success(orders)
    }
    
    @GetMapping("/user/{userId}/status/{status}")
    fun getUserOrdersByStatus(
        @PathVariable userId: UUID,
        @PathVariable status: OrderStatus
    ): ApiResponse<List<Order>> {
        val orders = orderService.getUserOrdersByStatus(userId, status)
        return ApiResponse.success(orders)
    }
    
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(@PathVariable orderId: UUID): ApiResponse<Order> {
        val order = orderService.cancelOrder(orderId)
        return ApiResponse.success(order)
    }
    
    @GetMapping("/status/{status}")
    fun getOrdersByStatus(@PathVariable status: OrderStatus): ApiResponse<List<Order>> {
        val orders = orderService.getOrdersByStatus(status)
        return ApiResponse.success(orders)
    }
}
