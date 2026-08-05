/**
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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.delivery.DeliveryTaskMutationService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.utils.JsonHelper
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryTaskMutationServiceImpl(
  private val workerRepository: DeliveryWorkerRepository,
  private val orderQueryService: OrderQueryService,
  private val orderMutationService: OrderMutationService,
  private val orderOperationService: OrderOperationService,
  private val eventPublishService: SimplifiedEventPublishService,
  private val jsonHelper: JsonHelper,
) : DeliveryTaskMutationService {
  private val logger = LoggerFactory.getLogger(DeliveryTaskMutationService::class.java)

  /**
   * 分配送水员给订单
   */
  @Transactional
  override fun assignDeliveryWorker(
    adminId: Long,
    orderId: Long,
    workerId: Long,
    isSelfCollect: Boolean,
  ): OrderModel {
    val order =
      orderQueryService.getOrderById(orderId)

    val worker =
      workerRepository.findById(workerId).orElseThrow {
        NotFoundException("配送员不存在: $workerId")
      }

    // 验证订单状态 - 待配送状态才能派单
    if (order.status != OrderModelStatus.PENDING_DELIVERY) {
      throw BadRequestException("订单状态不正确，无法分配配送员")
    }

    // 验证配送员状态
    if (worker.onlineStatus != DeliverWorkerModelStatus.ONLINE) {
      throw BadRequestException("配送员不在线，无法分配任务")
    }

    // 分配配送员，状态变为已接单（待开始配送）
    order.deliveryWorkerId = workerId
    order.isSelfCollect = isSelfCollect
    order.status = OrderModelStatus.DELIVERING

    val savedOrder = orderMutationService.updateOrder(orderId, order)

    // 发布配送分配事件
    eventPublishService.publishDeliveryAssigned(
      orderId = savedOrder.id!!,
      deliveryWorkerId = workerId,
      userId = savedOrder.userId,
      adminId = adminId,
    )

    logger.info("Successfully assigned worker $workerId to order $orderId, isSelfCollect: $isSelfCollect")
    return savedOrder
  }

  /**
   * 批量分配订单给配送员
   */
  @Transactional
  override fun batchAssignOrders(
    adminId: Long,
    orderIds: List<Long>,
    workerId: Long,
  ): List<OrderModel> {
    val worker =
      workerRepository.findById(workerId).orElseThrow {
        NotFoundException("配送员不存在: $workerId")
      }

    // 验证配送员状态
    if (worker.onlineStatus != DeliverWorkerModelStatus.ONLINE) {
      throw BadRequestException("配送员不在线，无法分配任务")
    }

    val assignedOrders = mutableListOf<OrderModel>()

    for (orderId in orderIds) {
      try {
        val order =
          orderQueryService.getOrderById(orderId)

        // 只处理待配送状态的订单
        if (order.status == OrderModelStatus.PENDING_DELIVERY) {
          order.deliveryWorkerId = workerId
          order.status = OrderModelStatus.DELIVERING
          val savedOrder = orderMutationService.updateOrder(orderId, order)

          // 发布配送分配事件
          eventPublishService.publishDeliveryAssigned(
            adminId = adminId,
            orderId = savedOrder.id!!,
            deliveryWorkerId = workerId,
            userId = savedOrder.userId,
          )

          assignedOrders.add(savedOrder)
        }
      } catch (e: Exception) {
        logger.warn("Failed to assign order $orderId to worker $workerId: ${e.message}")
      }
    }

    logger.info("Batch assigned ${assignedOrders.size} orders to worker $workerId")
    return assignedOrders
  }

  /**
   * 配送员接单
   */
  @Transactional
  override fun acceptDelivery(
    orderId: Long,
    adminId: Long,
  ): OrderModel {
    val order =
      orderQueryService.getOrderById(orderId)

    // 验证订单状态
    if (order.status != OrderModelStatus.PENDING_DISPATCH) {
      throw BadRequestException("订单状态不正确，无法接单")
    }
    val worker = workerRepository.findByAdminId(adminId) ?: throw UserNotFoundException("管理员 $adminId 不存在")
    val workerId = worker.id
    // 分配配送员
    order.deliveryWorkerId = workerId
    order.status = OrderModelStatus.PENDING_DELIVERY

    val savedOrder = orderMutationService.updateOrder(orderId, order)

    // 记录订单操作 - 配送员接单
    orderOperationService.recordOperation(
      orderId = savedOrder.id!!,
      operationType = OrderOperationType.DELIVERY_ASSIGNED,
      operatorType = OperatorType.DELIVERY_WORKER,
      operatorId = adminId,
      description = "配送员接单",
    )

    // 发布配送分配事件
    eventPublishService.publishDeliveryAssigned(
      adminId = adminId,
      orderId = savedOrder.id!!,
      deliveryWorkerId = workerId ?: 0L,
      userId = savedOrder.userId,
    )

    logger.info("Worker $adminId accepted delivery for order $orderId")
    return savedOrder
  }

  /**
   * 开始配送（配送员点击开始配送按钮）
   */
  @Transactional
  override fun startDelivery(orderId: Long): OrderModel {
    val order =
      orderQueryService.getOrderById(orderId)

    // 验证订单状态
    if (order.status != OrderModelStatus.PENDING_DELIVERY) {
      throw BadRequestException("订单状态不正确，无法开始配送")
    }

    // 验证配送员已分配
    if (order.deliveryWorkerId == null) {
      throw BadRequestException("订单未分配配送员，无法开始配送")
    }

    // 设置开始配送时间
    order.deliveryStartedAt = java.time.LocalDateTime.now()
    order.status = OrderModelStatus.DELIVERING
    val savedOrder = orderMutationService.updateOrder(orderId, order)
    // 记录订单操作 - 开始配送
    orderOperationService.recordOperation(
      orderId = savedOrder.id!!,
      operationType = OrderOperationType.DELIVERY_STARTED,
      operatorType = OperatorType.DELIVERY_WORKER,
      operatorId = savedOrder.deliveryWorkerId,
      description = "配送员开始配送",
    )

    // 发布配送开始事件
    eventPublishService.publishDeliveryStarted(
      orderId = savedOrder.id!!,
      deliveryWorkerId = savedOrder.deliveryWorkerId!!,
    )

    logger.info("Started delivery for order $orderId")
    return savedOrder
  }

  /**
   * 完成配送任务
   * @param deliveryPhotos 配送照片列表
   * @param paymentType 收款方式（非自收订单需要记录）
   * @param remark 配送员备注
   */
  @Transactional
  override fun completeDelivery(
    orderId: Long,
    deliveryPhotos: List<String>,
    paymentType: PaymentType?,
    remark: String?,
  ): OrderModel {
    val order =
      orderQueryService.getOrderById(orderId)

    if (order.status != OrderModelStatus.DELIVERING) {
      throw BadRequestException("订单状态不正确，无法完成配送")
    }

    // 更新订单状态
    order.status = OrderModelStatus.COMPLETED
    order.deliveryPhotos = jsonHelper.createArrayNode(deliveryPhotos).toString()
    order.paymentType = paymentType
    order.deliveryRemark = remark
    order.deliveryConfirmedAt = java.time.LocalDateTime.now()
    order.completedAt = java.time.LocalDateTime.now()
    val savedOrder = orderMutationService.updateOrder(orderId, order)

    // 记录订单操作 - 配送完成
    orderOperationService.recordOperation(
      orderId = savedOrder.id!!,
      operationType = OrderOperationType.DELIVERY_COMPLETED,
      operatorType = OperatorType.DELIVERY_WORKER,
      operatorId = savedOrder.deliveryWorkerId,
      description = if (remark.isNullOrBlank()) "配送员完成配送" else "配送员完成配送: $remark",
      extraData = """{"paymentType": "${paymentType?.name}"}""",
    )

    // 记录订单操作 - 订单完成
    orderOperationService.recordOperation(
      orderId = savedOrder.id!!,
      operationType = OrderOperationType.ORDER_COMPLETED,
      operatorType = OperatorType.DELIVERY_WORKER,
      operatorId = savedOrder.deliveryWorkerId,
      description = "订单已完成",
    )

    // 发布配送完成事件
    eventPublishService.publishDeliveryCompleted(
      orderId = savedOrder.id!!,
      deliveryWorkerId = savedOrder.deliveryWorkerId!!,
    )

    // 发布订单完成事件
    eventPublishService.publishOrderCompleted(
      orderId = savedOrder.id!!,
      userId = savedOrder.userId,
      deliveryWorkerId = savedOrder.deliveryWorkerId!!,
    )

    logger.info("Successfully completed delivery for order $orderId, paymentType: $paymentType")
    return savedOrder
  }
}
