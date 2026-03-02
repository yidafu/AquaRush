/*
 * AquaRush Admin Order Mutation Resolver
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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.DeliveryService
import dev.yidafu.aqua.api.service.OrderService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.PaymentType
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.order.mapper.OrderMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@AdminService
@Controller("adminOrderMutationResolver")
class AdminOrderMutationResolver(
  private val orderService: OrderService,
  private val deliveryService: DeliveryService,
) {
  private val logger = LoggerFactory.getLogger(AdminOrderMutationResolver::class.java)

  /**
   * 创建订单 - 管理员权限（管理员可以为任何用户创建订单）
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun createOrder(
    @Argument @Valid input: CreateOrderInput,
    @Argument userId: Long,
  ): Order = OrderMapper.map(orderService.createOrder(input, userId))

  /**
   * 取消订单 - 管理员权限
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun cancelOrder(
    @Argument orderId: Long,
  ): Order =
    orderService.cancelOrderForAdmin(orderId)?.let { OrderMapper.map(it) }

      ?: throw IllegalArgumentException("Order not found")

  /**
   * 更新订单状态 - 管理员权限
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun updateOrderStatus(
    @Argument orderId: Long,
    @Argument status: OrderStatus,
  ): Order =
    orderService
      .updateOrderStatus(orderId, status.name)
      ?.let { OrderMapper.map(it) }
      ?: throw IllegalArgumentException("Order not found")

  // ==================== 派单相关 mutations ====================

  /**
   * 分配配送员给订单（管理员功能）
   * @param orderId 订单ID
   * @param workerId 配送员ID
   * @param isSelfCollect 是否自收（水钱已收/水票已扣）
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun assignDeliveryWorker(
    @Argument orderId: Long,
    @Argument workerId: Long,
    @Argument isSelfCollect: Boolean,
  ): Order {
    logger.info("Admin assigning worker $workerId to order $orderId, isSelfCollect: $isSelfCollect")
    return OrderMapper.map(deliveryService.assignDeliveryWorker(orderId, workerId, isSelfCollect))
  }

  /**
   * 批量分配订单给配送员（管理员功能）
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun batchAssignOrders(
    @Argument orderIds: List<Long>,
    @Argument workerId: Long,
  ): List<Order> {
    logger.info("Admin batch assigning orders $orderIds to worker $workerId")
    return deliveryService.batchAssignOrders(orderIds, workerId).map { OrderMapper.map(it) }
  }

  /**
   * 配送员接单（配送员或管理员可调用）
   */
  @MutationMapping
  fun acceptDelivery(
    @Argument orderId: Long,
    @Argument workerId: Long,
  ): Order {
    logger.info("Worker $workerId accepting delivery for order $orderId")
    return OrderMapper.map(deliveryService.acceptDelivery(orderId, workerId))
  }

  /**
   * 开始配送（配送员点击开始配送按钮）
   */
  @MutationMapping
  fun startDelivery(
    @Argument orderId: Long,
  ): Order {
    logger.info("Starting delivery for order $orderId")
    return OrderMapper.map(deliveryService.startDelivery(orderId))
  }

  /**
   * 完成配送（拍照确认）
   * @param photos 配送照片列表
   * @param paymentType 收款方式（非自收订单需要记录）
   */
  @MutationMapping
  fun completeDelivery(
    @Argument orderId: Long,
    @Argument photos: List<String>,
    @Argument paymentType: PaymentType?,
  ): Order {
    logger.info("Completing delivery for order $orderId, paymentType: $paymentType")
    return OrderMapper.map(deliveryService.completeDelivery(orderId, photos, paymentType))
  }
}
