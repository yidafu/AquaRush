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

import dev.yidafu.aqua.api.dto.UpdateOrderAddressRequest
import dev.yidafu.aqua.api.service.admin.AddressService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryTaskMutationService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.util.JsonHelper
import dev.yidafu.aqua.logging.util.BizLogger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryTaskMutationServiceImpl(
  private val adminService: AdminService,
  private val orderQueryService: OrderQueryService,
  private val orderMutationService: OrderMutationService,
  private val orderOperationService: OrderOperationService,
  private val eventPublishService: SimplifiedEventPublishService,
  private val jsonHelper: JsonHelper,
  private val bizLogger: BizLogger,
  private val addressService: AddressService,
) : DeliveryTaskMutationService {
  private val logger = LoggerFactory.getLogger(DeliveryTaskMutationService::class.java)

  /**
   * 校验账号具备配送能力且在线(送水员档案并入 admins 单表后,workerId 即账号 id)
   *
   * 停用账号(DISABLED)不可派单/接单(AC-USR-021-07,PRD/07-user.md §F-USR-021)。
   */
  private fun requireOnlineDeliveryWorker(workerId: Long): AdminModel {
    val worker =
      adminService.findById(workerId)
        ?: throw NotFoundException("配送员不存在: $workerId")
    if (worker.role != AdminRoleModel.DELIVERY_WORKER && worker.role != AdminRoleModel.ADMIN) {
      throw NotFoundException("配送员不存在: $workerId")
    }
    if (worker.accountStatus == AdminAccountStatus.DISABLED) {
      throw BadRequestException("配送员账号已停用，无法分配任务")
    }
    if (worker.onlineStatus != DeliverWorkerModelStatus.ONLINE) {
      throw BadRequestException("配送员不在线，无法分配任务")
    }
    return worker
  }

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

    requireOnlineDeliveryWorker(workerId)

    // 验证订单状态 - 仅待派单可派单(R-ORD-01 状态机「待派单→配送中」,G09;
    // 待配送=已被抢单/接单,二次派单由状态拦截,幂等成立)
    if (order.status != OrderModelStatus.PENDING_DISPATCH) {
      throw BadRequestException("订单状态不正确，仅待派单订单可分配配送员")
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

    // 业务日志(F-LOG-003 DELIVERY 域):派单
    bizLogger.logDeliveryOperation(
      deliveryId = savedOrder.id!!,
      operation = "DELIVERY_ASSIGNED",
      result = "SUCCESS",
      userId = adminId,
      additionalData = mapOf("workerId" to workerId, "isSelfCollect" to isSelfCollect),
    )
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
    requireOnlineDeliveryWorker(workerId)

    val assignedOrders = mutableListOf<OrderModel>()

    for (orderId in orderIds) {
      try {
        val order =
          orderQueryService.getOrderById(orderId)

        // 只处理待派单状态的订单(R-ORD-01 状态机「待派单→配送中」,G09)
        if (order.status == OrderModelStatus.PENDING_DISPATCH) {
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

          // 业务日志(F-LOG-003 DELIVERY 域):批量派单(逐单记录)
          bizLogger.logDeliveryOperation(
            deliveryId = savedOrder.id!!,
            operation = "DELIVERY_ASSIGNED",
            result = "SUCCESS",
            userId = adminId,
            additionalData = mapOf("workerId" to workerId, "batch" to true),
          )
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
    // 账号即送水员身份(单表);校验账号存在且具备配送能力
    val worker =
      adminService.findById(adminId)
        ?: throw UserNotFoundException("管理员 $adminId 不存在")
    if (worker.role != AdminRoleModel.DELIVERY_WORKER && worker.role != AdminRoleModel.ADMIN) {
      throw UserNotFoundException("管理员 $adminId 不存在")
    }
    // 停用账号不可接单(AC-USR-021-07)
    if (worker.accountStatus == AdminAccountStatus.DISABLED) {
      throw BadRequestException("账号已停用，无法接单")
    }
    val workerId = worker.id ?: throw UserNotFoundException("管理员 $adminId 不存在")
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
      deliveryWorkerId = workerId,
      userId = savedOrder.userId,
    )

    logger.info("Worker $adminId accepted delivery for order $orderId")

    // 业务日志(F-LOG-003 DELIVERY 域):配送员接单
    bizLogger.logDeliveryOperation(
      deliveryId = savedOrder.id!!,
      operation = "DELIVERY_ACCEPTED",
      result = "SUCCESS",
      userId = adminId,
      additionalData = mapOf("workerId" to workerId),
    )
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

    // 业务日志(F-LOG-003 DELIVERY 域):开始配送
    bizLogger.logDeliveryOperation(
      deliveryId = savedOrder.id!!,
      operation = "DELIVERY_STARTED",
      result = "SUCCESS",
      userId = savedOrder.deliveryWorkerId,
    )
    return savedOrder
  }

  /**
   * 完成配送任务
   * @param deliveryPhotos 配送照片列表
   * @param paymentType 收款方式（非自收订单需要记录）
   * @param remark 配送员备注
   *
   * 状态流转(R-ORD-11 确认收货与营收确认,PRD 02-business-flows.md §订单状态机):
   * - 非自提订单:配送中 → 待确认收货,仅记录送达时间(deliveryConfirmedAt,自动签收锚点),
   *   不置完成时间(completedAt)—— 营收在用户确认收货/系统自动确认后才计入(按确认时间归属);
   * - 自提订单:控制权在取水当场转移,配送中 → 已完成,即时计入营收(R-ORD-09)。
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

    // 服务端硬校验(R-ORD-07,PRD 04-order.md §2.1):配送照片必传 ≥1 张,防止绕过客户端
    if (deliveryPhotos.isEmpty()) {
      throw BadRequestException("请先拍摄配送照片")
    }

    // 更新订单状态
    order.status = if (order.isSelfCollect) OrderModelStatus.COMPLETED else OrderModelStatus.AWAITING_CONFIRMATION
    order.deliveryPhotos = jsonHelper.createArrayNode(deliveryPhotos).toString()
    // 送达即入核验队列(裁决缺陷 #10,G23/F-ORD-021):显式置 PENDING 待核验
    order.deliveryPhotoReviewStatus = DeliveryPhotoReviewStatus.PENDING
    // 电子票核销订单:未指定收款方式时自动记为「水票」(R-COL-04,PRD 10-marketing.md §F-MKT-007)
    val resolvedPaymentType = paymentType ?: if (order.ticketRedeemCount > 0) PaymentType.WATER_TICKET else null
    // 收款方式必选(R-ORD-07):非自提订单未选择收款方式且无水票核销 → 拦截;自提订单默认已收款(R-ORD-09)
    if (resolvedPaymentType == null && !order.isSelfCollect) {
      throw BadRequestException("请选择收款方式")
    }
    order.paymentType = resolvedPaymentType
    order.deliveryRemark = remark
    // 送达时间(自动签收锚点,R-ORD-11):非自提订单从此刻起 N 天未确认自动签收
    order.deliveryConfirmedAt = java.time.LocalDateTime.now()
    // 自提订单控制权当场转移:即时完成并计入营收;非自提订单待用户确认后由 confirmReceipt/autoConfirmReceipt 置完成时间
    if (order.isSelfCollect) {
      order.completedAt = java.time.LocalDateTime.now()
    }
    val savedOrder = orderMutationService.updateOrder(orderId, order)

    // 记录订单操作 - 配送完成(送达)
    orderOperationService.recordOperation(
      orderId = savedOrder.id!!,
      operationType = OrderOperationType.DELIVERY_COMPLETED,
      operatorType = OperatorType.DELIVERY_WORKER,
      operatorId = savedOrder.deliveryWorkerId,
      description = if (remark.isNullOrBlank()) "配送员完成配送" else "配送员完成配送: $remark",
      extraData = """{"paymentType": "${savedOrder.paymentType?.name}"}""",
    )

    // 自提订单:即时订单完成(操作 + 事件);非自提订单进入「待确认收货」,
    // 订单完成操作与 ORDER_COMPLETED 事件推迟到用户确认收货/系统自动确认(R-ORD-11)
    if (order.isSelfCollect) {
      // 记录订单操作 - 订单完成
      orderOperationService.recordOperation(
        orderId = savedOrder.id!!,
        operationType = OrderOperationType.ORDER_COMPLETED,
        operatorType = OperatorType.DELIVERY_WORKER,
        operatorId = savedOrder.deliveryWorkerId,
        description = "订单已完成",
      )

      // 发布订单完成事件
      eventPublishService.publishOrderCompleted(
        orderId = savedOrder.id!!,
        userId = savedOrder.userId,
        deliveryWorkerId = savedOrder.deliveryWorkerId!!,
      )
    }

    // 发布配送完成事件(送达确认通知:用户端「已送达,请确认收货」)
    eventPublishService.publishDeliveryCompleted(
      orderId = savedOrder.id!!,
      deliveryWorkerId = savedOrder.deliveryWorkerId!!,
    )

    logger.info("Successfully completed delivery for order $orderId, paymentType: $paymentType")

    // 业务日志(F-LOG-003 DELIVERY 域):配送完成
    bizLogger.logDeliveryOperation(
      deliveryId = savedOrder.id!!,
      operation = "DELIVERY_COMPLETED",
      result = "SUCCESS",
      userId = savedOrder.deliveryWorkerId,
      additionalData = mapOf("paymentType" to (savedOrder.paymentType?.name ?: "NONE")),
    )
    return savedOrder
  }

  /**
   * 配送任务中修正订单收货地址信息(F-ORD-024)
   *
   * 订单关联 addresses 表的地址记录(OrderModel.addressId → AddressModel),修改会同步
   * 反映到该用户的地址簿(后续订单复用修正后的地址);仅传入字段生效,空值不覆盖原值。
   * 终态订单(已完成/已取消/已退款)不可修改;订单状态不影响已生成订单的配送费快照(R-MON-02)。
   */
  @Transactional
  override fun updateOrderAddress(
    operatorId: Long,
    orderId: Long,
    request: UpdateOrderAddressRequest,
  ): OrderModel {
    val order = orderQueryService.getOrderById(orderId)

    // 终态订单不可再修正地址(R-ORD-01 状态机终态不可逆;待确认收货=已送达,地址亦不可再改 —— R-ORD-11)
    if (
      order.status == OrderModelStatus.AWAITING_CONFIRMATION ||
      order.status == OrderModelStatus.COMPLETED ||
      order.status == OrderModelStatus.CANCELLED ||
      order.status == OrderModelStatus.REFUNDED
    ) {
      throw BadRequestException("订单已结束，无法修改收货地址")
    }

    val address =
      addressService.findById(order.addressId)
        ?: throw BadRequestException("订单关联的收货地址不存在")

    // 仅非空字段生效(与 UpdateAddressInput 语义一致,空值不覆盖原值)
    request.receiverName?.let { address.receiverName = it }
    request.phone?.let { address.phone = it }
    request.detailAddress?.let { address.detailAddress = it }
    request.floor?.let { address.floor = it }
    request.longitude?.let { address.longitude = it }
    request.latitude?.let { address.latitude = it }
    addressService.save(address)

    // 订单操作留痕:配送地址修正(F-ORD-024),extraData 仅记录本次实际更新的字段
    val updatedFields = mutableMapOf<String, Any>()
    request.receiverName?.let { updatedFields["receiverName"] = it }
    request.phone?.let { updatedFields["phone"] = it }
    request.detailAddress?.let { updatedFields["detailAddress"] = it }
    request.floor?.let { updatedFields["floor"] = it }
    request.longitude?.let { updatedFields["longitude"] = it }
    request.latitude?.let { updatedFields["latitude"] = it }
    orderOperationService.recordOperation(
      orderId = order.id!!,
      operationType = OrderOperationType.ADDRESS_UPDATED,
      operatorType = OperatorType.DELIVERY_WORKER,
      operatorId = operatorId,
      description = "配送员修正收货地址信息(收货人/电话/详细地址/楼层/定位)",
      extraData = jsonHelper.createObjectNode(updatedFields).toString(),
    )

    logger.info("Worker $operatorId updated address $order.addressId for order $orderId")

    // 业务日志(F-LOG-003 DELIVERY 域):配送地址修正
    bizLogger.logDeliveryOperation(
      deliveryId = order.id!!,
      operation = "DELIVERY_ADDRESS_UPDATED",
      result = "SUCCESS",
      userId = operatorId,
      additionalData = mapOf("addressId" to order.addressId),
    )
    return order
  }
}
