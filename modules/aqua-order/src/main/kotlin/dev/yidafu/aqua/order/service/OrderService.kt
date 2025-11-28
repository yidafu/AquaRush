/*
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.order.service

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.Order
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.delivery.service.DeliveryService
import dev.yidafu.aqua.order.domain.model.DomainEvent
import dev.yidafu.aqua.order.domain.model.EventStatus
import dev.yidafu.aqua.order.domain.repository.DomainEventRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.product.service.ProductService
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import dev.yidafu.aqua.common.domain.service.OrderService as IOrderService

@Service
class OrderService(
  private val orderRepository: OrderRepository,
  private val productRepository: ProductRepository,
  private val addressRepository: AddressRepository,
  private val domainEventRepository: DomainEventRepository,
  private val productService: ProductService,
  private val deliveryService: DeliveryService,
) : IOrderService {
  private val objectMapper = jacksonObjectMapper()

  @Transactional
  fun createOrder(
    userId: Long,
    productId: Long,
    addressId: Long,
    quantity: Int,
  ): Order {
    // 1. 验证产品存在且有足够库存
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { NotFoundException("产品不存在: $productId") }

    if (product.stock < quantity) {
      throw BadRequestException("库存不足，当前库存: ${product.stock}，需求数量: $quantity")
    }

    // 2. 验证地址存在且属于当前用户
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { NotFoundException("收货地址不存在: $addressId") }

    if (address.userId != userId) {
      throw BadRequestException("无权使用此收货地址")
    }

    // 3. 验证地址是否在配送范围内
    deliveryService.validateDeliveryAddress(address.province, address.city, address.district)

    // 4. 计算订单金额
    val amount = product.price.multiply(BigDecimal(quantity))

    // 5. 生成唯一订单号
    val orderNumber = generateUniqueOrderNumber()

    // 6. 扣减库存（使用原子操作）
    val stockDecreased = productService.decreaseStock(productId, quantity)
    if (!stockDecreased) {
      throw BadRequestException("库存扣减失败，请重试")
    }

    // 7. 创建订单
    val order =
      Order(
        id = DefaultIdGenerator().generate(),
        orderNumber = orderNumber,
        userId = userId,
        productId = productId,
        quantity = quantity,
        amount = amount,
        addressId = addressId,
        deliveryAddressId = addressId, // 映射到同一字段
        status = OrderStatus.PENDING_PAYMENT,
        paymentMethod = null,
        paymentTransactionId = null,
        paymentTime = null,
        deliveryWorkerId = null,
        deliveryPhotos = null,
        completedAt = null,
        totalAmount = amount, // 映射到同一字段
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    val savedOrder = orderRepository.save(order)

    // 8. 发布订单创建事件
    publishDomainEvent(
      eventType = "ORDER_CREATED",
      aggregateId = savedOrder.id.toString(),
      eventData =
        mapOf(
          "orderId" to savedOrder.id.toString(),
          "orderNumber" to savedOrder.orderNumber,
          "userId" to savedOrder.userId.toString(),
          "productId" to savedOrder.productId.toString(),
          "quantity" to savedOrder.quantity,
          "amount" to savedOrder.amount.toString(),
          "addressId" to savedOrder.addressId.toString(),
        ),
    )

    return savedOrder
  }

  // 保持原有方法以兼容现有代码
  @Transactional
  fun createOrder(order: Order): Order = createOrder(order.userId, order.productId, order.addressId, order.quantity)

  override fun getOrderById(orderId: Long): Order =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }

  override fun getOrderByNumber(orderNumber: String): Order =
    orderRepository.findByOrderNumber(orderNumber)
      ?: throw NotFoundException("订单不存在: $orderNumber")

  fun getUserOrders(userId: Long): List<Order> = orderRepository.findByUserId(userId)

  fun getUserOrdersByStatus(
    userId: Long,
    status: OrderStatus,
  ): List<Order> = orderRepository.findByUserIdAndStatus(userId, status)

  @Transactional
  fun cancelOrder(orderId: Long): Order {
    val order = getOrderById(orderId)

    // 1. 验证订单状态是否可以取消
    if (order.status == OrderStatus.CANCELLED) {
      throw BadRequestException("订单已取消")
    }

    if (order.status == OrderStatus.COMPLETED) {
      throw BadRequestException("订单已完成，无法取消")
    }

    if (order.status == OrderStatus.DELIVERING) {
      throw BadRequestException("订单配送中，无法取消")
    }

    // 2. 如果订单已支付，需要退款（先处理库存恢复，退款在支付服务中处理）
    var shouldRefund = false
    if (order.status == OrderStatus.PENDING_DELIVERY && order.paymentTransactionId != null) {
      shouldRefund = true
    }

    // 3. 恢复库存（仅在未配送且已支付或待支付时恢复）
    if (order.status != OrderStatus.DELIVERING && order.status != OrderStatus.COMPLETED) {
      productService.increaseStock(order.productId, order.quantity)
    }

    // 4. 更新订单状态
    order.status = OrderStatus.CANCELLED
    val cancelledOrder = orderRepository.save(order)

    // 5. 发布订单取消事件
    publishDomainEvent(
      eventType = "ORDER_CANCELLED",
      aggregateId = cancelledOrder.id.toString(),
      eventData =
        mapOf(
          "orderId" to cancelledOrder.id.toString(),
          "orderNumber" to cancelledOrder.orderNumber,
          "userId" to cancelledOrder.userId.toString(),
          "productId" to cancelledOrder.productId.toString(),
          "quantity" to cancelledOrder.quantity,
          "amount" to cancelledOrder.amount.toString(),
          "shouldRefund" to shouldRefund,
          "paymentTransactionId" to (cancelledOrder.paymentTransactionId ?: ""),
        ),
    )

    return cancelledOrder
  }

  @Transactional
  fun updateOrderStatus(
    orderId: Long,
    status: OrderStatus,
  ): Order {
    val order = getOrderById(orderId)
    order.status = status
    return orderRepository.save(order)
  }

  fun getOrdersByStatus(status: OrderStatus): List<Order> = orderRepository.findByStatus(status)

  /**
   * 生成唯一订单号
   */
  private fun generateUniqueOrderNumber(): String {
    val timestamp = System.currentTimeMillis()
    val random = (1000..9999).random()
    return "ORD${timestamp}$random"
  }

  /**
   * 发布领域事件
   */
  private fun publishDomainEvent(
    eventType: String,
    aggregateId: String,
    eventData: Map<String, Any>,
    nextRunAt: LocalDateTime? = null,
  ) {
    val eventPayload = objectMapper.writeValueAsString(eventData)

    val domainEvent =
      DomainEvent(
        id = DefaultIdGenerator().generate(),
        eventType = eventType,
        payload = eventPayload,
        status = EventStatus.PENDING,
        retryCount = 0,
        nextRunAt = nextRunAt,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        errorMessage = null,
      )

    domainEventRepository.save(domainEvent)
  }

  /**
   * 处理支付成功
   */
  @Transactional
  override fun handlePaymentSuccess(
    orderId: Long,
    paymentTransactionId: String,
  ) {
    val order = getOrderById(orderId)

    if (order.status != OrderStatus.PENDING_PAYMENT) {
      throw BadRequestException("订单状态不正确，无法处理支付")
    }

    order.status = OrderStatus.PENDING_DELIVERY
    order.paymentTransactionId = paymentTransactionId
    order.paymentTime = LocalDateTime.now()
    order.paymentMethod = PaymentMethod.WECHAT_PAY

    val updatedOrder = orderRepository.save(order)

    // 发布支付成功事件
    publishDomainEvent(
      eventType = "ORDER_PAID",
      aggregateId = updatedOrder.id.toString(),
      eventData =
        mapOf(
          "orderId" to updatedOrder.id.toString(),
          "orderNumber" to updatedOrder.orderNumber,
          "userId" to updatedOrder.userId.toString(),
          "amount" to updatedOrder.amount.toString(),
          "paymentTransactionId" to paymentTransactionId,
        ),
    )
  }

  /**
   * 处理支付超时
   */
  @Transactional
  override fun handlePaymentTimeout(orderId: Long) {
    val order = getOrderById(orderId)

    if (order.status != OrderStatus.PENDING_PAYMENT) {
      return // 已处理过，跳过
    }

    // 恢复库存
    productService.increaseStock(order.productId, order.quantity)

    // 取消订单
    order.status = OrderStatus.CANCELLED
    orderRepository.save(order)

    // 发布支付超时事件
    publishDomainEvent(
      eventType = "PAYMENT_TIMEOUT",
      aggregateId = order.id.toString(),
      eventData =
        mapOf(
          "orderId" to order.id.toString(),
          "orderNumber" to order.orderNumber,
          "userId" to order.userId.toString(),
          "amount" to order.amount.toString(),
        ),
    )
  }
}
