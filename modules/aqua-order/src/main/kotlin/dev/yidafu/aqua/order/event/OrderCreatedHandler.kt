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

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OperatorType
import dev.yidafu.aqua.common.domain.model.OrderOperationType
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class OrderCreatedHandler(
  private val orderOperationService: OrderOperationService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(OrderCreatedHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_CREATED

  /**
   * 处理订单创建事件
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
      val adminId = eventData["adminId"] as? Long
      if (adminId == null) {
        logger.info("Recording ORDER_CREATED operation for order: $orderId")

        // 记录订单操作
        orderOperationService.recordOperation(
          orderId = orderId,
          operationType = OrderOperationType.ORDER_CREATED,
          operatorType = OperatorType.USER,
          operatorId = userId,
          description = "用户创建订单",
        )

        logger.info("Successfully recorded ORDER_CREATED operation for order: $orderId")
      } else {
        logger.info("Recording ORDER_CREATED operation (admin) for order: $orderId")

        orderOperationService.recordOperation(
          orderId = orderId,
          operationType = OrderOperationType.ORDER_CREATED,
          operatorType = OperatorType.ADMIN,
          operatorId = adminId,
          description = "管理员/配送员创建订单",
        )

        logger.info("Successfully recorded ORDER_CREATED operation (admin) for order: $orderId")
      }
    } catch (e: Exception) {
      logger.error("Failed to record ORDER_CREATED operation: ${event.id}", e)
      throw e
    }
  }
}
