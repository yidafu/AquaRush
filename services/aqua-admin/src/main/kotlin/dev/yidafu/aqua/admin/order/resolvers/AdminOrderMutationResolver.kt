/**
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

import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryTaskMutationService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.CreateDeliveryOrderInput
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.order.mapper.CreateDeliveryOrderInputMapper
import dev.yidafu.aqua.order.mapper.OrderMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

@Controller("adminOrderMutationResolver")
class AdminOrderMutationResolver(
  private val orderMutationService: OrderMutationService,
  private val orderQueryService: OrderQueryService,
  private val deliveryTaskMutationService: DeliveryTaskMutationService,
  private val adminService: AdminService,
) {
  private val logger = LoggerFactory.getLogger(AdminOrderMutationResolver::class.java)

  /**
   * 取消订单 - 管理员权限
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun cancelOrder(
    @Argument orderId: Long,
  ): Order =
    orderMutationService.cancelOrderForAdmin(orderId)?.let { OrderMapper.map(it) }

      ?: throw IllegalArgumentException("Order not found")

  /**
   * 更新订单状态 - 管理员权限
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun updateOrderStatus(
    @Argument orderId: Long,
    @Argument status: OrderModelStatus,
  ): Order =
    orderMutationService
      .updateOrderStatus(orderId, status)
      .let { OrderMapper.map(it) }

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
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Order {
    val adminId = principal.id
    val username = principal.username
    logger.info("Admin $username($adminId) assigning worker $workerId to order $orderId, isSelfCollect: $isSelfCollect")
    return OrderMapper.map(deliveryTaskMutationService.assignDeliveryWorker(adminId, orderId, workerId, isSelfCollect))
  }

  /**
   * 批量分配订单给配送员（管理员功能）
   */
  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun batchAssignOrders(
    @Argument orderIds: List<Long>,
    @Argument workerId: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<Order> {
    val adminId = principal.id
    val username = principal.username
    logger.info("Admin $username($adminId)  batch assigning orders $orderIds to worker $workerId")
    return deliveryTaskMutationService.batchAssignOrders(adminId, orderIds, workerId).map { OrderMapper.map(it) }
  }

  /**
   * 配送员创建订单（配送员为用户创建订单）
   * 配送员不需要用户认证，直接通过地址ID获取用户信息
   */
  @MutationMapping
  fun createDeliveryOrder(
    @Argument @Valid input: CreateDeliveryOrderInput,
    @AuthenticationPrincipal userDetail: UserDetails,
  ): Order {
    logger.info(
      "Delivery creating order: productId=${input.productId}, addressId=${input.addressId}, quantity=${input.quantity}, isSelfCollect=${input.isSelfCollect}",
    )
    val adminId = getAdminId(userDetail.username)
    val request = CreateDeliveryOrderInputMapper.map(input)
    val order =
      orderMutationService.createDeliveryOrder(
        adminId,
        request,
      )
    return OrderMapper.map(
      orderQueryService.getOrderById(order.id!!),
    )
  }

  /**
   * 配送员接单（配送员或管理员可调用）
   */
  @MutationMapping
  fun acceptDelivery(
    @Argument orderId: Long,
    @AuthenticationPrincipal userDetail: UserDetails,
  ): Order {
    val username = userDetail.username
    logger.info("Worker $username accepting delivery for order $orderId")
    return OrderMapper.map(deliveryTaskMutationService.acceptDelivery(orderId, getAdminId(username)))
  }

  /**
   * 开始配送（配送员点击开始配送按钮）
   */
  @MutationMapping
  fun startDelivery(
    @Argument orderId: Long,
  ): Order {
    logger.info("Starting delivery for order $orderId")
    return OrderMapper.map(deliveryTaskMutationService.startDelivery(orderId))
  }

  /**
   * 完成配送（拍照确认）
   * @param photos 配送照片列表
   * @param paymentType 收款方式（非自收订单需要记录）
   * @param remark 配送员备注
   */
  @MutationMapping
  fun completeDelivery(
    @Argument orderId: Long,
    @Argument photos: List<String>,
    @Argument paymentType: PaymentType?,
    @Argument remark: String?,
  ): Order {
    logger.info("Completing delivery for order $orderId, paymentType: $paymentType, remark: $remark")
    return OrderMapper.map(deliveryTaskMutationService.completeDelivery(orderId, photos, paymentType, remark))
  }

  private fun getAdminId(username: String): Long =
    adminService.findByUsername(username)?.id ?: throw UserNotFoundException("管理员 $username 不存在")
}
