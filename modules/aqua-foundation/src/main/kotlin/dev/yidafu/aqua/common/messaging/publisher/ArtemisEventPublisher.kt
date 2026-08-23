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

package dev.yidafu.aqua.common.messaging.publisher

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import org.apache.activemq.artemis.api.core.Message
import org.slf4j.LoggerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessagePostProcessor
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * ActiveMQ Artemis事件发布器
 * 基于ActiveMQ Artemis的消息队列实现，提供可靠且高性能的事件发布机制
 */
@Component
class ArtemisEventPublisher(
  val jmsTemplate: JmsTemplate,
  /** 单次消息发送的最大等待时间;超时即放弃(返回 false),避免业务请求被消息队列无限挂起。 */
  private val sendTimeoutMillis: Long = DEFAULT_SEND_TIMEOUT_MILLIS,
) : EventPublisher {
  private val logger = LoggerFactory.getLogger(ArtemisEventPublisher::class.java)
  private val objectMapper = jacksonObjectMapper()

  /**
   * 事件发送专用线程池(daemon)。
   *
   * Artemis 在 broker 磁盘满/地址满(流控,AMQ222210/AMQ222212)时会让生产者无限期阻塞等待
   * credits(ClientProducerCreditsImpl.actualAcquire 以 10s 为周期无限重试,没有整体超时)。
   * 若直接在请求线程同步发送,订单接单/派单/完成配送等业务请求会被永久挂起、事务不提交,
   * 客户端表现为请求超时("network request fail")。因此发送统一挪到独立线程做有界等待,
   * 超时即视为发送失败(与既有 catch→false 语义一致),业务主流程不再被消息队列拖垮。
   */
  private val sendExecutor: ExecutorService =
    Executors.newFixedThreadPool(SEND_THREAD_COUNT) { runnable ->
      Thread(runnable, "aqua-jms-event-sender").apply { isDaemon = true }
    }

  override suspend fun publish(event: DomainEvent): Boolean = sendBounded(getDestinationForEventType(event.eventType), event)

  override fun publishSync(event: DomainEvent): Boolean = sendBounded(getDestinationForEventType(event.eventType), event)

  /**
   * 延迟投递(R-ORD-04 支付超时优化):
   * 通过 Artemis 核心消息头 HDR_SCHEDULED_DELIVERY_TIME(_AMQ_SCHED_DELIVERY)由 broker
   * 在 deliveryTimeMillis 时刻投递(随 journal 持久化,broker 重启不丢;由兜底扫描覆盖异常场景)。
   */
  override fun publishDelayed(
    event: DomainEvent,
    deliveryTimeMillis: Long,
  ): Boolean =
    sendBounded(getDestinationForEventType(event.eventType), event) { message ->
      message.setLongProperty(Message.HDR_SCHEDULED_DELIVERY_TIME.toString(), deliveryTimeMillis)
      message
    }

  /**
   * 有界发送:在独立线程执行 JMS 发送并等待至多 [sendTimeoutMillis];
   * 超时后中断阻塞中的发送并返回 false(发送失败,调用方已有 catch→false 语义,不抛异常)。
   */
  private fun sendBounded(
    destination: String,
    event: DomainEvent,
    postProcessor: MessagePostProcessor = MessagePostProcessor { it },
  ): Boolean {
    val sendTask = Runnable { jmsTemplate.convertAndSend(destination, event, postProcessor) }
    return try {
      val future = sendExecutor.submit(sendTask)
      try {
        future.get(sendTimeoutMillis, TimeUnit.MILLISECONDS)
        logger.debug("消息已发送到Artemis队列: {}", event.eventType)
        true
      } catch (e: TimeoutException) {
        future.cancel(true)
        logger.error(
          "消息发送超时(>{}ms)已放弃: {},broker 可能处于磁盘满/地址满流控状态",
          sendTimeoutMillis,
          event.eventType,
        )
        false
      }
    } catch (e: Exception) {
      logger.error("消息发送失败: {}", event.eventType, e)
      false
    }
  }

  override fun getName(): String = "ArtemisEventPublisher"

  override fun isAvailable(): Boolean = true

  override fun getType(): EventTypePublisherType = EventTypePublisherType.ARTEMIS

  override fun getPriority(): Int = 1 // 最高优先级

  override suspend fun publishBatch(events: List<DomainEvent>): List<Boolean> =
    events.map { event -> sendBounded(getDestinationForEventType(event.eventType), event) }

  override fun publishBatchSync(events: List<DomainEvent>): List<Boolean> =
    events.map { event ->
      try {
        jmsTemplate.sendAndReceive(getDestinationForEventType(event.eventType)) { session ->
          session.createTextMessage(objectMapper.writeValueAsString(event))
        }
        logger.debug("批量同步消息发送成功: {}", event.eventType)
        true
      } catch (e: Exception) {
        logger.error("批量同步消息发送失败: {}", event.eventType, e)
        false
      }
    }

  companion object {
    private const val SEND_THREAD_COUNT = 4

    /** 默认单次发送超时:5s。broker 健康时发送为毫秒级;超时说明 broker 异常,放弃发送以保业务。 */
    private const val DEFAULT_SEND_TIMEOUT_MILLIS = 5000L
  }

  /**
   * 根据事件类型确定目标队列
   */
  private fun getDestinationForEventType(eventType: String): String {
    val domainEventType = DomainEventType.fromValue(eventType)
    return when (domainEventType) {
      DomainEventType.ORDER_CREATED,
      DomainEventType.ORDER_PAID,
      DomainEventType.ORDER_CANCELLED,
      DomainEventType.ORDER_COMPLETED,
      DomainEventType.ORDER_DELIVERED,
      DomainEventType.ORDER_URGING,
      -> EventQueueName.ORDER_QUEUE.toString()

      DomainEventType.PAYMENT_TIMEOUT,
      DomainEventType.PAYMENT_REFUNDED,
      -> EventQueueName.PAYMENT_QUEUE.toString()

      // 支付到期检查触发:独立队列,仅 admin 服务消费(R-ORD-04 支付超时优化)
      DomainEventType.PAYMENT_EXPIRY_CHECK,
      -> EventQueueName.PAYMENT_EXPIRY_QUEUE.toString()

      DomainEventType.DELIVERY_ASSIGNED,
      DomainEventType.DELIVERY_STARTED,
      DomainEventType.DELIVERY_COMPLETED,
      DomainEventType.DELIVERY_TIMEOUT,
      -> EventQueueName.DELIVERY_QUEUE.toString()

      DomainEventType.USER_REGISTERED,
      DomainEventType.USER_UPDATED,
      DomainEventType.COUPON_EXPIRING,
      -> EventQueueName.USER_QUEUE.toString()

      DomainEventType.NOTIFICATION_SEND,
      -> EventQueueName.NOTIFICATION_QUEUE.toString()

      else -> EventQueueName.DELIVERY_QUEUE.toString()
    }
  }
}
