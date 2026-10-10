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

package dev.yidafu.aqua.common.messaging.consumer

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.outbox.OutboxDestinationHandler
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 业务领域事件 outbox 处理器(替代原 MessageConsumer 的 JMS 监听分发)。
 * 监听四个业务队列(order/payment/delivery/user-events),按 [DomainEventType]
 * 分发到全部 [EventProcessor];失败异常上抛 → outbox 退避重试(处理端幂等)。
 */
@Component
class DomainEventOutboxHandler(
  private val eventProcessors: List<EventProcessor>,
) : OutboxDestinationHandler {
  private val logger = LoggerFactory.getLogger(DomainEventOutboxHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  // 构建事件类型到处理器的映射
  private val eventProcessorMap: Map<DomainEventType, List<EventProcessor>> by lazy {
    eventProcessors.groupBy { it.getSupportedEventType() }
  }

  override val destinations: Set<String> =
    setOf(
      EventQueueConst.ORDER_QUEUE_NAME,
      EventQueueConst.PAYMENT_QUEUE_NAME,
      EventQueueConst.DELIVERY_QUEUE_NAME,
      EventQueueConst.USER_QUEUE_NAME,
    )

  override fun handle(record: OutboxEventRecord) {
    val event = objectMapper.readValue(record.payload, DomainEvent::class.java)
    logger.info("处理事件: {} ({})", event.eventType, record.destination)
    val eventType = DomainEventType.fromValue(event.eventType)
    val processors = eventProcessorMap[eventType]
    if (processors == null) {
      logger.warn("未知的事件类型: {}", event.eventType)
      return
    }
    processors.forEach { processor -> processor.handle(event) }
    logger.debug("事件处理成功: {}", event.eventType)
  }
}
