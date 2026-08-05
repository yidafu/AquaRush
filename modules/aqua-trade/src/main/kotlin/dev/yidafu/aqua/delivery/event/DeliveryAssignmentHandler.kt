package dev.yidafu.aqua.delivery.event

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class DeliveryAssignmentHandler(
  private val orderService: OrderQueryService,
  private val orderOperationService: OrderOperationService,
  private val adminService: AdminService,
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(DeliveryAssignmentHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.DELIVERY_ASSIGNED

  /**
   * 处理配送分配事件
   */
  @Transactional
  override fun handle(event: DomainEvent) {
    try {
      // 解析payload获取事件数据
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )

      val orderId = eventData["orderId"].toString().toLong()
      val adminId = eventData["adminId"].toString().toLong()
      val workerId = eventData["deliveryWorkerId"].toString().toLong()
      val order =
        orderService
          .getOrderById(orderId)

      logger.info("Processing DELIVERY_ASSIGNMENT event for order: ${order.orderNo}")
      val admin = adminService.findById(adminId)
      val deliveryWorker =
        deliveryWorkerRepository
          .findById(workerId)
          .orElseThrow { IllegalStateException("Delivery worker not found: $adminId") }
      val msg = "${admin?.realName ?: "管理员"}($adminId)将${order.user?.nickname ?: "用户"}的订单[${
        order.orderNo}]派给${deliveryWorker.name}"

      orderOperationService.recordOperation(
        orderId,
        OrderOperationType.DELIVERY_ASSIGNED,
        OperatorType.ADMIN,
        operatorId = adminId,
        msg,
      )
      logger.info("记录派单信息 => $msg")
    } catch (e: Exception) {
      logger.error("Failed to process DELIVERY_ASSIGNMENT event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }
}
