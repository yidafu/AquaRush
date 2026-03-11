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

import dev.yidafu.aqua.api.service.*
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

@Service
class OrderServiceImpl(
  private val orderRepository: OrderRepository,
  private val productRepository: ProductRepository,
  private val addressRepository: AddressRepository,
  private val eventPublishService: SimplifiedEventPublishService,
  private val productService: ProductService,
  private val deliveryService: DeliveryService,
  private val orderIdGenerator: OrderIdGeneratorService,
  private val adminService: AdminService,
) : OrderService {
  private val logger = LoggerFactory.getLogger(OrderServiceImpl::class.java)
  private val objectMapper = jacksonObjectMapper()

  @Transactional
  override fun createOrder(
    userId: Long,
    productId: Long,
    addressId: Long,
    quantity: Int,
  ): OrderModel {
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
    // 这里需要区分管理员下单和不同用户下单
    if (address.userId != userId) {
      throw BadRequestException("无权使用此收货地址")
    }

    // 3. 验证地址是否在配送范围内
    deliveryService.validateDeliveryAddress(address.province, address.city, address.district)

    // 4. 计算订单金额 (product.price is already in cents)
    val amount = product.price

    // 5. 生成唯一订单号
    val orderNumber = orderIdGenerator.generateOrderId(userId)

    // 6. 扣减库存（使用原子操作）
    val stockDecreased = productService.decreaseStock(productId, quantity)
    if (!stockDecreased) {
      throw BadRequestException("库存扣减失败，请重试")
    }

    // 7. 创建订单
    val order =
      OrderModel(
        id = DefaultIdGenerator().generate(),
        orderNumber = orderNumber,
        userId = userId,
        productId = productId,
        quantity = quantity,
        amountCents = amount,
        addressId = addressId,
        status = OrderStatus.PENDING_PAYMENT,
        paymentMethod = null,
        paymentTransactionId = null,
        paymentTime = null,
        deliveryWorkerId = null,
        deliveryPhotos = null,
        completedAt = null,
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
          "amount" to savedOrder.amountCents.toString(),
          "amountCents" to savedOrder.amountCents,
          "addressId" to savedOrder.addressId.toString(),
        ),
    )

    return savedOrder
  }

  override fun getOrderById(orderId: Long): OrderModel =
    orderRepository.findById(orderId).orElseThrow {
      NotFoundException("订单不存在: $orderId")
    }

  override fun getOrderByNumber(orderNumber: String): OrderModel =
    orderRepository.findByOrderNumber(orderNumber)
      ?: throw NotFoundException("订单不存在: $orderNumber")

  override fun getUserOrders(userId: Long): List<OrderModel> = orderRepository.findByUserId(userId)

  override fun getUserOrdersByStatus(
    userId: Long,
    status: OrderStatus,
  ): List<OrderModel> = orderRepository.findByUserIdAndStatus(userId, status)

  @Transactional
  override fun cancelOrder(orderId: Long): OrderModel {
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
          "amount" to cancelledOrder.amountCents.toString(),
          "amountCents" to cancelledOrder.amountCents,
          "shouldRefund" to shouldRefund,
          "paymentTransactionId" to (cancelledOrder.paymentTransactionId ?: ""),
        ),
    )

    return cancelledOrder
  }

  @Transactional
  override fun updateOrderStatus(
    orderId: Long,
    status: OrderStatus,
  ): OrderModel {
    val order = getOrderById(orderId)
    order.status = status
    return orderRepository.save(order)
  }

  override fun getOrdersByStatus(status: OrderStatus): List<OrderModel> = orderRepository.findByStatus(status)

  /**
   * 发布领域事件
   */
  private fun publishDomainEvent(
    eventType: String,
    aggregateId: String,
    eventData: Map<String, Any>,
    nextRunAt: LocalDateTime? = null,
  ) {
    // 通过 Artemis 发布事件
    eventPublishService.publishDomainEvent(
      eventType = eventType,
      aggregateId = aggregateId,
      eventData = eventData,
    )
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
          "amount" to updatedOrder.amountCents.toString(),
          "amountCents" to updatedOrder.amountCents,
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
          "amount" to order.amountCents.toString(),
          "amountCents" to order.amountCents,
        ),
    )
  }

  // Additional methods for GraphQL resolvers
  @Transactional
  override fun createOrder(
    input: Any,
    userId: Long,
  ): OrderModel {
    // Parse input based on type (assuming CreateOrderInput)
    // This is a simplified implementation - you may need to adapt based on your input type
    val inputMap = objectMapper.convertValue(input, Map::class.java)
    val productId = (inputMap["productId"] as Number).toLong()
    val addressId = (inputMap["addressId"] as Number).toLong()
    val quantity = (inputMap["quantity"] as Number).toInt()

    return createOrder(userId, productId, addressId, quantity)
  }

  /**
   * 配送员创建订单 - 通过地址ID获取用户ID
   * 配送员不需要用户认证，直接通过地址ID获取用户信息
   */
  @Transactional
  override fun createDeliveryOrder(
    adminId: Long,
    productId: Long,
    addressId: Long,
    quantity: Int,
    isSelfCollect: Boolean,
    remark: String?,
  ): OrderModel {
    // 1. 验证地址存在并获取用户ID
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { NotFoundException("收货地址不存在: $addressId") }

    // 从地址中获取用户ID
    val user = adminService.getUserById(adminId)
    val userId = user.id!!
    // 2. 验证产品存在且有足够库存
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { NotFoundException("产品不存在: $productId") }

    if (product.stock < quantity) {
      throw BadRequestException("库存不足，当前库存: ${product.stock}，需求数量: $quantity")
    }

    // 3. 验证地址是否在配送范围内
    deliveryService.validateDeliveryAddress(address.province, address.city, address.district)

    // 4. 计算订单金额 (product.price is already in cents)
    val amount = product.price
    // 5. 生成唯一订单号
    val orderNumber = orderIdGenerator.generateOrderId(userId)
    // 6. 扣减库存（使用原子操作）
    val stockDecreased = productService.decreaseStock(productId, quantity)
    if (!stockDecreased) {
      throw BadRequestException("库存扣减失败，请重试")
    }

    // 7. 创建订单
    val order =
      OrderModel(
//        id = DefaultIdGenerator().generate(),
        orderNumber = orderNumber,
        userId = userId,
        productId = productId,
        quantity = quantity,
        amountCents = amount,
        addressId = addressId,
        status = OrderStatus.PENDING_DELIVERY,
        paymentMethod = PaymentMethod.CASH,
        paymentTransactionId = null,
        paymentTime = if (isSelfCollect) LocalDateTime.now() else null,
        deliveryWorkerId = null,
        deliveryPhotos = null,
        completedAt = null,
        remark = remark,
        isSelfCollect = isSelfCollect,
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
          "amount" to savedOrder.amountCents.toString(),
          "amountCents" to savedOrder.amountCents,
          "addressId" to savedOrder.addressId.toString(),
        ),
    )

    return orderRepository.findById(order.id).orElseThrow()
  }

  @Transactional
  override fun cancelOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findById(orderId).orElse(null) ?: return null

    // Verify order belongs to user
    if (order.userId != userId) {
      return null
    }

    return cancelOrder(orderId)
  }

  @Transactional
  override fun cancelOrderForAdmin(orderId: Long): OrderModel? =
    try {
      cancelOrder(orderId)
    } catch (e: Exception) {
      null
    }

  @Transactional
  override fun updateOrderStatus(
    orderId: Long,
    status: String,
  ): OrderModel? =
    try {
      val orderStatus = OrderStatus.valueOf(status.uppercase())
      updateOrderStatus(orderId, orderStatus)
    } catch (e: Exception) {
      null
    }

  override fun findAllOrders(): List<OrderModel> = orderRepository.findAll()

  override fun findOrderByIdAndUserId(
    orderId: Long,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findById(orderId).orElse(null) ?: return null
    return if (order.userId == userId) order else null
  }

  override fun findOrderByNumberAndUserId(
    orderNumber: String,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findByOrderNumber(orderNumber) ?: return null
    return if (order.userId == userId) order else null
  }

  override fun findOrdersByUserId(userId: Long): List<OrderModel> = orderRepository.findByUserId(userId)

  override fun findOrdersByStatus(status: String): List<OrderModel> =
    try {
      val orderStatus = OrderStatus.valueOf(status.uppercase())
      getOrdersByStatus(orderStatus)
    } catch (e: Exception) {
      emptyList()
    }

  override fun findOrdersByUserIdAndStatus(
    userId: Long,
    status: String,
  ): List<OrderModel> =
    try {
      val orderStatus = OrderStatus.valueOf(status.uppercase())
      getUserOrdersByStatus(userId, orderStatus)
    } catch (e: Exception) {
      emptyList()
    }

  // Method for current user (requires authentication context)
  fun findOrdersByCurrentUser(): List<OrderModel> {
    // This would typically be implemented with authentication context
    // For now, return empty list - you may need to adapt based on your auth setup
    return emptyList()
  }

  override fun searchOrders(
    keyword: String?,
    status: String?,
    userId: Long?,
    dateFrom: String?,
    dateTo: String?,
    minAmount: Long?,
    maxAmount: Long?,
    deliveryWorkerId: Long?,
    page: Int,
    size: Int,
    sort: String,
  ): Page<OrderModel> {
    // Parse sort string (e.g., "createdAt,desc")
    val sortParts = sort.split(",")
    val sortField = sortParts.getOrElse(0) { "createdAt" }
    val sortDirection = sortParts.getOrElse(1) { "desc" }

    // Convert date strings to LocalDateTime
    val startDate =
      dateFrom?.let {
        try {
          LocalDateTime.parse(it)
        } catch (e: Exception) {
          null
        }
      }
    val endDate =
      dateTo?.let {
        try {
          LocalDateTime.parse(it)
        } catch (e: Exception) {
          null
        }
      }

    // Convert status string to OrderStatus enum
    val orderStatus =
      status?.let {
        try {
          OrderStatus.valueOf(it.uppercase())
        } catch (e: Exception) {
          null
        }
      }

    return orderRepository.findOrdersPaginated(
      keyword = keyword,
      status = orderStatus,
      userId = userId,
      deliveryWorkerId = deliveryWorkerId,
      startDate = startDate,
      endDate = endDate,
      minAmount = minAmount,
      maxAmount = maxAmount,
      page = page,
      size = size,
      sortField = sortField,
      sortDirection = sortDirection,
    )
  }
}
