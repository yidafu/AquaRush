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
class DeliveryTimeoutHandler {
  private val logger = LoggerFactory.getLogger(DeliveryTimeoutHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 处理配送超时事件
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

      logger.info("Processing DELIVERY_TIMEOUT event for order: $orderNumber")

      // 配送超时处理逻辑：
      // 1. 记录超时事件
      // 2. 发送通知给管理员
      // 3. 可以考虑重新分配配送员
      // 4. 发送通知给用户说明配送延迟

      // 记录配送超时日志
      logger.warn("Delivery timeout detected for order: $orderNumber")

      // 可以在这里实现具体的超时处理逻辑
      // 例如：sendDeliveryTimeoutNotification(orderId, orderNumber)
      // 例如：reassignDeliveryWorker(orderId)

      logger.info("Successfully processed DELIVERY_TIMEOUT event for order: $orderNumber")
    } catch (e: Exception) {
      logger.error("Failed to process DELIVERY_TIMEOUT event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }
}
