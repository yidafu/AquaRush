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

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class PaymentTimeoutHandler(
  private val orderOperationService: OrderOperationService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(PaymentTimeoutHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.PAYMENT_TIMEOUT

  /**
   * 处理支付超时事件
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

      val orderId = (eventData["orderId"] as Number).toLong()
      val userId = (eventData["userId"] as Number).toLong()

      logger.info("Processing PAYMENT_TIMEOUT event for order: $orderId")

      // 记录订单操作
      orderOperationService.recordOperation(
        orderId = orderId,
        operationType = OrderOperationType.PAYMENT_TIMEOUT,
        operatorType = OperatorType.SYSTEM,
        description = "订单支付超时，自动取消",
      )

      // 可以在这里发送支付超时通知给用户
      // sendPaymentTimeoutNotification(userId, orderNumber)

      logger.info("Successfully processed PAYMENT_TIMEOUT event for order: $orderId")
    } catch (e: Exception) {
      logger.error("Failed to process PAYMENT_TIMEOUT event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }
}
