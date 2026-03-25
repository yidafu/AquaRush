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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.query.CreateOrderRequest
import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryAreaQueryService
import dev.yidafu.aqua.api.service.order.OrderIdGeneratorService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 订单变更服务实现
 */
@Service
class OrderMutationServiceImpl(
  private val orderRepository: OrderRepository,
  private val productRepository: ProductRepository,
  private val addressRepository: AddressRepository,
  private val eventPublishService: SimplifiedEventPublishService,
  private val productService: ProductService,
  private val deliveryAreaQueryService: DeliveryAreaQueryService,
  private val orderIdGenerator: OrderIdGeneratorService,
  private val adminService: AdminService,
  private val orderQueryService: OrderQueryService,
) : OrderMutationService {
  private val logger = LoggerFactory.getLogger(OrderMutationServiceImpl::class.java)

  /**
   * 订单验证结果，包含验证通过后所需的全部数据
   */
  private data class OrderValidationResult(
    val product: ProductModel,
    val address: AddressModel,
    val userId: Long,
    val amountCents: Long,
    val orderNo: String,
  )

  /**
   * 统一的订单验证逻辑，提取 createOrder 和 createDeliveryOrder 的公共验证步骤
   */
  private fun validateAndPrepareOrder(
    userId: Long,
    productId: Long,
    addressId: Long,
    quantity: Int,
  ): OrderValidationResult {
    // 1. 验证产品存在且有足够库存
    val product =
      productRepository
        .findById(productId)
        .orElseThrow { NotFoundException("产品不存在: $productId") }

    if (product.stock < quantity) {
      throw BadRequestException("库存不足，当前库存: ${product.stock}，需求数量: $quantity")
    }

    // 2. 验证地址存在
    val address =
      addressRepository
        .findById(addressId)
        .orElseThrow { NotFoundException("收货地址不存在: $addressId") }

    // 3. 验证地址是否在配送范围内
    deliveryAreaQueryService.validateDeliveryAddress(address.province, address.city, address.district)

    // 4. 计算订单金额 (product.price is already in cents)
    val amountCents = product.price * quantity

    // 5. 生成唯一订单号
    val orderNo = orderIdGenerator.generateOrderId()

    // 6. 扣减库存（使用原子操作）
    val stockDecreased = productService.decreaseStock(productId, quantity)
    if (!stockDecreased) {
      throw BadRequestException("库存扣减失败，请重试")
    }

    return OrderValidationResult(
      product = product,
      address = address,
      userId = userId,
      amountCents = amountCents,
      orderNo = orderNo,
    )
  }

  /**
   * 保存订单的公共逻辑
   */
  private fun saveOrder(
    userId: Long,
    product: ProductModel,
    quantity: Int,
    addressId: Long,
    orderNo: String,
    amountCents: Long,
    paymentMethod: PaymentMethod?,
    remark: String?,
    isSelfCollect: Boolean,
    paymentTime: LocalDateTime?,
  ): OrderModel {
    val order =
      OrderModel(
//        id = DefaultIdGenerator().generate(),
        orderNo = orderNo,
        userId = userId,
        productId = product.id!!,
        quantity = quantity,
        amountCents = amountCents,
        addressId = addressId,
        status = OrderModelStatus.PENDING_DISPATCH,
        paymentMethod = paymentMethod,
        paymentTransactionId = null,
        paymentTime = paymentTime,
        deliveryWorkerId = null,
        deliveryPhotos = null,
        completedAt = null,
        remark = remark,
        isSelfCollect = isSelfCollect,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    val savedOrder = orderRepository.save(order)

    // 发布订单创建事件
    eventPublishService.publishOrderCreated(
      orderId = savedOrder.id!!,
      userId = savedOrder.userId,
      productId = savedOrder.productId,
      quantity = savedOrder.quantity,
      amountCents = savedOrder.amountCents,
    )

    return savedOrder
  }

  @Transactional
  override fun createOrder(input: CreateOrderRequest): OrderModel {
    val validation =
      validateAndPrepareOrder(
        userId = input.userId,
        productId = input.productId,
        addressId = input.addressId,
        quantity = input.quantity,
      )

    // 验证地址属于当前用户
    if (validation.address.userId != validation.userId) {
      throw BadRequestException("无权使用此收货地址")
    }

    return saveOrder(
      userId = validation.userId,
      product = validation.product,
      quantity = input.quantity,
      addressId = validation.address.id!!,
      orderNo = validation.orderNo,
      amountCents = validation.amountCents,
      paymentMethod = null,
      remark = null,
      isSelfCollect = false,
      paymentTime = null,
    )
  }

  @Transactional
  override fun createDeliveryOrder(
    adminId: Long,
    input: CreateOrderRequest,
  ): OrderModel {
    // 获取下单用户信息（adminId 是管理员/配送员自己的ID）
    val user = adminService.getUserById(adminId)
    val userId = user.id!!

    val validation =
      validateAndPrepareOrder(
        userId = userId,
        productId = input.productId,
        addressId = input.addressId,
        quantity = input.quantity,
      )
    val isSelfCollect = input.isSelfCollect ?: false

    return saveOrder(
      userId = validation.userId,
      product = validation.product,
      quantity = input.quantity,
      addressId = validation.address.id!!,
      orderNo = validation.orderNo,
      amountCents = validation.amountCents,
      paymentMethod = PaymentMethod.CASH,
      remark = input.remark,
      isSelfCollect = isSelfCollect,
      paymentTime = if (isSelfCollect) LocalDateTime.now() else null,
    )
  }

  @Transactional
  override fun cancelOrder(orderId: Long): OrderModel {
    val order = orderQueryService.getOrderById(orderId)

    // 1. 验证订单状态是否可以取消
    if (order.status == OrderModelStatus.CANCELLED) {
      throw BadRequestException("订单已取消")
    }

    if (order.status == OrderModelStatus.COMPLETED) {
      throw BadRequestException("订单已完成，无法取消")
    }

    if (order.status == OrderModelStatus.DELIVERING) {
      throw BadRequestException("订单配送中，无法取消")
    }

    // 2. 如果订单已支付，需要退款（先处理库存恢复，退款在支付服务中处理）
    var shouldRefund = false
    if (order.status == OrderModelStatus.PENDING_DELIVERY && order.paymentTransactionId != null) {
      shouldRefund = true
    }

    // 3. 恢复库存（仅在未配送且已支付或待支付时恢复）
    if (order.status != OrderModelStatus.DELIVERING && order.status != OrderModelStatus.COMPLETED) {
      productService.increaseStock(order.productId, order.quantity)
    }

    // 4. 更新订单状态
    order.status = OrderModelStatus.CANCELLED
    val cancelledOrder = orderRepository.save(order)

    // 5. 发布订单取消事件
    val cancelDescription = if (shouldRefund) "用户取消订单（需退款）" else "用户取消订单"
    eventPublishService.publishOrderCancelled(
      orderId = cancelledOrder.id!!,
      userId = cancelledOrder.userId,
      reason = cancelDescription,
    )

    return cancelledOrder
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

  override fun updateOrder(
    orderId: Long,
    orderDTO: OrderModel,
  ): OrderModel {
    orderDTO.id = orderId
    return orderRepository.save(orderDTO)
  }

  @Transactional
  override fun updateOrderStatus(
    orderId: Long,
    status: OrderModelStatus,
  ): OrderModel {
    val order = orderQueryService.getOrderById(orderId)
    order.status = status
    return orderRepository.save(order)
  }

  @Transactional
  override fun handlePaymentSuccess(
    orderId: Long,
    paymentTransactionId: String,
  ) {
    val order = orderQueryService.getOrderById(orderId)

    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      throw BadRequestException("订单状态不正确，无法处理支付")
    }

    order.status = OrderModelStatus.PENDING_DELIVERY
    order.paymentTransactionId = paymentTransactionId
    order.paymentTime = LocalDateTime.now()
    order.paymentMethod = PaymentMethod.WECHAT_PAY

    val updatedOrder = orderRepository.save(order)

    // 发布支付成功事件
    eventPublishService.publishOrderPaid(
      orderId = updatedOrder.id!!,
      userId = updatedOrder.userId,
      productId = updatedOrder.productId,
      amountCents = updatedOrder.amountCents,
    )
  }

  @Transactional
  override fun handlePaymentTimeout(orderId: Long) {
    val order = orderQueryService.getOrderById(orderId)

    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      return // 已处理过，跳过
    }

    // 恢复库存
    productService.increaseStock(order.productId, order.quantity)

    // 取消订单
    order.status = OrderModelStatus.CANCELLED
    orderRepository.save(order)

    // 发布支付超时事件
    eventPublishService.publishPaymentTimeout(
      orderId = order.id!!,
      userId = order.userId,
    )
  }
}
