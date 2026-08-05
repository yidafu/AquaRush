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

package dev.yidafu.aqua.order.event

import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class OrderCancelledHandler(
  private val orderRepository: OrderRepository,
  private val paymentService: PaymentService,
  private val orderOperationService: OrderOperationService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(OrderCancelledHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_CANCELLED

  /**
   * 处理订单取消事件
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
      val order =
        orderRepository
          .findById(orderId)
          .orElseThrow { IllegalStateException("Order not found: $orderId") }

      val shouldRefund = eventData["shouldRefund"] as? Boolean ?: false
      val paymentTransactionId = eventData["paymentTransactionId"] as? String

      logger.info("Processing ORDER_CANCELLED event for order: ${order.orderNo}, shouldRefund: $shouldRefund")

      // 记录订单操作
      val cancelDescription = if (shouldRefund) "用户取消订单（需退款）" else "用户取消订单"
      orderOperationService.recordOperation(
        orderId = order.id!!,
        operationType = OrderOperationType.ORDER_CANCELLED,
        operatorType = OperatorType.USER,
        operatorId = order.userId,
        description = cancelDescription,
      )

      // 如果需要退款且存在支付交易号
      if (shouldRefund && !paymentTransactionId.isNullOrEmpty()) {
        processRefund(order, paymentTransactionId)
      }

      logger.info("Successfully processed ORDER_CANCELLED event for order: ${order.orderNo}")
    } catch (e: Exception) {
      logger.error("Failed to process ORDER_CANCELLED event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }

  /**
   * 处理退款
   */
  private fun processRefund(
    order: OrderModel,
    paymentTransactionId: String,
  ) {
    try {
      // 计算退款金额（分为单位）
      val refundAmount = (order.amountCents)
      val totalAmount = refundAmount

      logger.info("Processing refund for order ${order.orderNo}, amount: ${order.amountCents}")

      // 调用退款接口
      val refundResult =
        paymentService.refund(
          transactionId = paymentTransactionId,
          refundAmountCents = refundAmount,
          totalAmountCents = totalAmount,
          reason = "订单取消退款 - 订单号: ${order.orderNo}",
//          refundAmountCents = order.amount,
//          totalAmountCents = order.totalAmount,
        )

      logger.info("Refund processed successfully for order ${order.orderNo}, refundId: ${refundResult["refundId"]}")

      // 可以在这里发送退款成功通知给用户
      // sendRefundNotification(order.userId, refundResult)
    } catch (e: Exception) {
      logger.error("Failed to process refund for order ${order.orderNo}", e)

      // 退款失败不抛出异常，但需要记录日志以便人工处理
      logger.error("Refund failed for order ${order.orderNo}, requires manual intervention")
      // 可以发送告警通知给管理员
      // sendRefundFailureAlert(order, e)
    }
  }
}
