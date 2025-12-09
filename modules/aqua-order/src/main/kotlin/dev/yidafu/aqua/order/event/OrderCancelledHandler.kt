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

package dev.yidafu.aqua.order.event

import tools.jackson.module.kotlin.jacksonObjectMapper
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.repository.OrderRepository
import dev.yidafu.aqua.order.domain.model.DomainEventModel
import dev.yidafu.aqua.payment.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Component
class OrderCancelledHandler(
  private val orderRepository: OrderRepository,
  private val paymentService: PaymentService,
) {
  private val logger = LoggerFactory.getLogger(OrderCancelledHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 处理订单取消事件
   */
  @Transactional
  fun handle(event: DomainEventModel) {
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

      logger.info("Processing ORDER_CANCELLED event for order: ${order.orderNumber}, shouldRefund: $shouldRefund")

      // 如果需要退款且存在支付交易号
      if (shouldRefund && !paymentTransactionId.isNullOrEmpty()) {
        processRefund(order, paymentTransactionId)
      }

      logger.info("Successfully processed ORDER_CANCELLED event for order: ${order.orderNumber}")
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
      val refundAmount = order.amount.multiply(BigDecimal("100")).toInt()
      val totalAmount = refundAmount

      logger.info("Processing refund for order ${order.orderNumber}, amount: ${order.amount}")

      // 调用退款接口
      val refundResult =
        paymentService.refund(
          transactionId = paymentTransactionId,
          refundAmount = refundAmount,
          totalAmount = totalAmount,
          reason = "订单取消退款 - 订单号: ${order.orderNumber}",
        )

      logger.info("Refund processed successfully for order ${order.orderNumber}, refundId: ${refundResult["refundId"]}")

      // 可以在这里发送退款成功通知给用户
      // sendRefundNotification(order.userId, refundResult)
    } catch (e: Exception) {
      logger.error("Failed to process refund for order ${order.orderNumber}", e)

      // 退款失败不抛出异常，但需要记录日志以便人工处理
      logger.error("Refund failed for order ${order.orderNumber}, requires manual intervention")
      // 可以发送告警通知给管理员
      // sendRefundFailureAlert(order, e)
    }
  }
}
