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
import dev.yidafu.aqua.order.domain.model.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class PaymentTimeoutHandler {
  private val logger = LoggerFactory.getLogger(PaymentTimeoutHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 处理支付超时事件
   */
  @Transactional
  fun handle(event: DomainEvent) {
    try {
      // 解析payload获取事件数据
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )

      val orderId = UUID.fromString(eventData["orderId"] as String)
      val orderNumber = eventData["orderNumber"] as String

      logger.info("Processing PAYMENT_TIMEOUT event for order: $orderNumber")

      // 支付超时事件通常由OrderService.handlePaymentTimeout处理
      // 这里主要负责记录日志和发送通知

      // 可以在这里发送支付超时通知给用户
      // sendPaymentTimeoutNotification(userId, orderNumber)

      logger.info("Successfully processed PAYMENT_TIMEOUT event for order: $orderNumber")
    } catch (e: Exception) {
      logger.error("Failed to process PAYMENT_TIMEOUT event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }
}
