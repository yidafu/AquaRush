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

import dev.yidafu.aqua.common.messaging.config.MessagingProperties
import dev.yidafu.aqua.common.domain.model.enums.EventStatusModel
import dev.yidafu.aqua.common.domain.model.DomainEventModel
import dev.yidafu.aqua.order.domain.repository.DomainEventRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

interface EventProcessor

/**
 * 混合模式事件处理器
 * 支持传统的Outbox模式和新的内存队列模式
 */
@Component
@ConditionalOnProperty(name = ["aqua.messaging.strategy"], havingValue = "outbox-only", matchIfMissing = false)
class OutboxEventProcessor(
  private val domainEventRepository: DomainEventRepository,
  private val orderPaidHandler: OrderPaidHandler,
  private val orderCancelledHandler: OrderCancelledHandler,
  private val paymentTimeoutHandler: PaymentTimeoutHandler,
  private val deliveryAssignmentHandler: DeliveryAssignmentHandler,
  private val deliveryTimeoutHandler: DeliveryTimeoutHandler,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(OutboxEventProcessor::class.java)

  companion object {
    private const val MAX_RETRY_COUNT = 5
    private val RETRY_DELAYS =
      longArrayOf(
        TimeUnit.MINUTES.toMillis(1), // 1 minute
        TimeUnit.MINUTES.toMillis(5), // 5 minutes
        TimeUnit.MINUTES.toMillis(15), // 15 minutes
        TimeUnit.HOURS.toMillis(1), // 1 hour
        TimeUnit.HOURS.toMillis(6), // 6 hours
      )
  }

  /**
   * 定时处理待处理的事件（仅Outbox模式）
   * 每分钟执行一次
   */
  @Scheduled(fixedRate = 60000) // 60 seconds
  @Transactional
  fun processPendingEvents() {
    try {
      val events =
        domainEventRepository.findPendingEvents(
          EventStatusModel.PENDING,
          LocalDateTime.now(),
        )

      logger.info("Found ${events.size} pending events to process (Outbox mode)")

      events.forEach { event ->
        try {
          processEvent(event)
        } catch (e: Exception) {
          logger.error("Error processing event ${event.id}", e)
          handleEventFailure(event, e)
        }
      }
    } catch (e: Exception) {
      logger.error("Error in Outbox event processing scheduler", e)
    }
  }

  /**
   * 处理单个事件
   */
  private fun processEvent(event: DomainEventModel) {
    // 标记事件为处理中
    event.status = EventStatusModel.PROCESSING
    domainEventRepository.save(event)

    try {
      when (event.eventType) {
        "ORDER_PAID" -> orderPaidHandler.handle(event)
        "ORDER_CANCELLED" -> orderCancelledHandler.handle(event)
        "ORDER_DELIVERED" -> {
          // TODO: 实现订单送达处理器
          logger.info("Processing ORDER_DELIVERED event: ${event.id}")
        }

        "ORDER_ASSIGNED" -> {
          // TODO: 实现订单分配处理器
          logger.info("Processing ORDER_ASSIGNED event: ${event.id}")
        }

        "PAYMENT_TIMEOUT" -> paymentTimeoutHandler.handle(event)
        "DELIVERY_TIMEOUT" -> deliveryTimeoutHandler.handle(event)
        else -> {
          logger.warn("Unknown event type: ${event.eventType}")
          event.status = EventStatusModel.COMPLETED
          domainEventRepository.save(event)
        }
      }

      // 如果处理成功，标记为已完成
      event.status = EventStatusModel.COMPLETED
      event.errorMessage = null
      domainEventRepository.save(event)

      logger.info("Successfully processed event: ${event.eventType} (${event.id})")
    } catch (e: Exception) {
      throw e // 让外层catch处理失败逻辑
    }
  }

  /**
   * 处理事件失败
   */
  private fun handleEventFailure(
    event: DomainEventModel,
    exception: Exception,
  ) {
    event.retryCount++

    if (event.retryCount >= MAX_RETRY_COUNT) {
      // 超过最大重试次数，标记为失败
      event.status = EventStatusModel.FAILED
      event.errorMessage = exception.message ?: "Unknown error"
      logger.error("Event processing failed after $MAX_RETRY_COUNT retries: ${event.id}", exception)
    } else {
      // 计算下次重试时间
      val delayIndex = minOf(event.retryCount - 1, RETRY_DELAYS.size - 1)
      val nextRunAt = LocalDateTime.now().plusSeconds(RETRY_DELAYS[delayIndex] / 1000)

      event.status = EventStatusModel.PENDING
      event.nextRunAt = nextRunAt
      event.errorMessage = exception.message ?: "Unknown error"

      logger.warn("Event processing failed, will retry in ${RETRY_DELAYS[delayIndex]}ms: ${event.id}", exception)
    }

    domainEventRepository.save(event)
  }

  /**
   * 清理已完成的事件
   * 每小时执行一次，清理30天前的已完成事件
   */
  @Scheduled(fixedRate = 3600000) // 1 hour
  @Transactional
  fun cleanupCompletedEvents() {
    try {
      val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
      domainEventRepository.deleteByStatusAndCreatedAtBefore(EventStatusModel.COMPLETED, thirtyDaysAgo)
      logger.info("Cleaned up completed events older than 30 days")
    } catch (e: Exception) {
      logger.error("Error cleaning up completed events", e)
    }
  }
}

/**
 * 混合模式事件处理器协调器
 * 同时使用Outbox和内存队列模式，提供最佳性能和可靠性
 */
@Component
@ConditionalOnProperty(name = ["aqua.messaging.strategy"], havingValue = "hybrid", matchIfMissing = true)
class HybridEventProcessorCoordinator(
//  private val localMessageEventProcessor: LocalMessageEventProcessor,
  private val domainEventRepository: DomainEventRepository,
  private val messagingProperties: MessagingProperties,
) {
  private val logger = LoggerFactory.getLogger(HybridEventProcessorCoordinator::class.java)

  companion object {
    private const val MAX_RETRY_COUNT = 5
    private val RETRY_DELAYS =
      longArrayOf(
        TimeUnit.MINUTES.toMillis(1), // 1 minute
        TimeUnit.MINUTES.toMillis(5), // 5 minutes
        TimeUnit.MINUTES.toMillis(15), // 15 minutes
        TimeUnit.HOURS.toMillis(1), // 1 hour
        TimeUnit.HOURS.toMillis(6), // 6 hours
      )
  }

  /**
   * 定时处理Outbox中的待处理事件（混合模式下的Outbox清理）
   * 每分钟执行一次，主要用于处理回退事件和清理
   */
  @Scheduled(fixedRate = 60000) // 60 seconds
  @Transactional
  fun processOutboxFallbackEvents() {
    try {
      val events =
        domainEventRepository.findPendingEvents(
          EventStatusModel.PENDING,
          LocalDateTime.now(),
        )

      if (events.isNotEmpty()) {
        logger.info("Found ${events.size} Outbox fallback events to process (Hybrid mode)")

        events.forEach { event ->
          try {
            // 只处理那些无法通过内存队列处理的事件
            // 或者作为死信队列的处理
            processOutboxEvent(event)
          } catch (e: Exception) {
            logger.error("Error processing Outbox event ${event.id}", e)
            handleOutboxEventFailure(event, e)
          }
        }
      }
    } catch (e: Exception) {
      logger.error("Error in Outbox fallback event processing scheduler", e)
    }
  }

  /**
   * 处理Outbox中的事件
   */
  private fun processOutboxEvent(event: DomainEventModel) {
    logger.info("Processing Outbox fallback event: ${event.eventType} (${event.id})")

    // 这里可以根据事件类型进行特殊处理
    // 或者记录到监控系统
    when (event.eventType) {
      "ORDER_PAID" -> logger.debug("Outbox processing ORDER_PAID: ${event.id}")
      "ORDER_CANCELLED" -> logger.debug("Outbox processing ORDER_CANCELLED: ${event.id}")
      "PAYMENT_TIMEOUT" -> logger.debug("Outbox processing PAYMENT_TIMEOUT: ${event.id}")
      "DELIVERY_TIMEOUT" -> logger.debug("Outbox processing DELIVERY_TIMEOUT: ${event.id}")
      else -> logger.warn("Outbox processing unknown event type: ${event.eventType}")
    }

    // 标记为已完成（假设已经通过其他方式处理）
    event.status = EventStatusModel.COMPLETED
    event.errorMessage = null
    domainEventRepository.save(event)
  }

  /**
   * 处理Outbox事件失败
   */
  private fun handleOutboxEventFailure(
    event: DomainEventModel,
    exception: Exception,
  ) {
    event.retryCount++

    if (event.retryCount >= MAX_RETRY_COUNT) {
      // 超过最大重试次数，标记为失败
      event.status = EventStatusModel.FAILED
      event.errorMessage = exception.message ?: "Unknown error"
      logger.error("Outbox event processing failed after $MAX_RETRY_COUNT retries: ${event.id}", exception)
    } else {
      // 计算下次重试时间
      val delayIndex = minOf(event.retryCount - 1, RETRY_DELAYS.size - 1)
      val nextRunAt = LocalDateTime.now().plusSeconds(RETRY_DELAYS[delayIndex] / 1000)

      event.status = EventStatusModel.PENDING
      event.nextRunAt = nextRunAt
      event.errorMessage = exception.message ?: "Unknown error"

      logger.warn(
        "Outbox event processing failed, will retry in ${RETRY_DELAYS[delayIndex]}ms: ${event.id}",
        exception,
      )
    }

    domainEventRepository.save(event)
  }

  /**
   * 清理已完成的事件
   * 每小时执行一次，清理30天前的已完成事件
   */
  @Scheduled(fixedRate = 3600000) // 1 hour
  @Transactional
  fun cleanupCompletedEvents() {
    try {
      val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
      domainEventRepository.deleteByStatusAndCreatedAtBefore(EventStatusModel.COMPLETED, thirtyDaysAgo)
      logger.info("Cleaned up completed events older than 30 days")
    } catch (e: Exception) {
      logger.error("Error cleaning up completed events", e)
    }
  }

  /**
   * 获取混合处理器状态
   */
  fun getHybridStatus(): Map<String, Any> =
    mapOf(
      "hybridMode" to true,
//      "memoryQueueStatus" to localMessageEventProcessor.getHealthStatus(),
      "outboxFallbackEnabled" to true,
      "messagingStrategy" to (messagingProperties.strategy ?: "hybrid"),
    )
}

class Xxx(
  val domainEventRepository: DomainEventRepository,
  val orderPaidHandler: OrderPaidHandler,
  val orderCancelledHandler: OrderCancelledHandler,
  val paymentTimeoutHandler: PaymentTimeoutHandler,
  val deliveryTimeoutHandler: DeliveryTimeoutHandler,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(EventProcessor::class.java)

  companion object {
    private const val MAX_RETRY_COUNT = 5
    private val RETRY_DELAYS =
      longArrayOf(
        TimeUnit.MINUTES.toMillis(1), // 1 minute
        TimeUnit.MINUTES.toMillis(5), // 5 minutes
        TimeUnit.MINUTES.toMillis(15), // 15 minutes
        TimeUnit.HOURS.toMillis(1), // 1 hour
        TimeUnit.HOURS.toMillis(6), // 6 hours
      )
  }

  /**
   * 定时处理待处理的事件
   * 每分钟执行一次
   */
  @Scheduled(fixedRate = 60000) // 60 seconds
  @Transactional
  fun processPendingEvents() {
    try {
      val events =
        domainEventRepository.findPendingEvents(
          EventStatusModel.PENDING,
          LocalDateTime.now(),
        )

      logger.info("Found ${events.size} pending events to process")

      events.forEach { event ->
        try {
          processEvent(event)
        } catch (e: Exception) {
          logger.error("Error processing event ${event.id}", e)
          handleEventFailure(event, e)
        }
      }
    } catch (e: Exception) {
      logger.error("Error in event processing scheduler", e)
    }
  }

  /**
   * 处理单个事件
   */
  private fun processEvent(event: DomainEventModel) {
    // 标记事件为处理中
    event.status = EventStatusModel.PROCESSING
    domainEventRepository.save(event)

    try {
      when (event.eventType) {
        "ORDER_PAID" -> orderPaidHandler.handle(event)
        "ORDER_CANCELLED" -> orderCancelledHandler.handle(event)
        "ORDER_DELIVERED" -> {
          // TODO: 实现订单送达处理器
          logger.info("Processing ORDER_DELIVERED event: ${event.id}")
        }

        "ORDER_ASSIGNED" -> {
          // TODO: 实现订单分配处理器
          logger.info("Processing ORDER_ASSIGNED event: ${event.id}")
        }

        "PAYMENT_TIMEOUT" -> paymentTimeoutHandler.handle(event)
        "DELIVERY_TIMEOUT" -> deliveryTimeoutHandler.handle(event)
        else -> {
          logger.warn("Unknown event type: ${event.eventType}")
          event.status = EventStatusModel.COMPLETED
          domainEventRepository.save(event)
        }
      }

      // 如果处理成功，标记为已完成
      event.status = EventStatusModel.COMPLETED
      event.errorMessage = null
      domainEventRepository.save(event)

      logger.info("Successfully processed event: ${event.eventType} (${event.id})")
    } catch (e: Exception) {
      throw e // 让外层catch处理失败逻辑
    }
  }

  /**
   * 处理事件失败
   */
  private fun handleEventFailure(
    event: DomainEventModel,
    exception: Exception,
  ) {
    event.retryCount++

    if (event.retryCount >= MAX_RETRY_COUNT) {
      // 超过最大重试次数，标记为失败
      event.status = EventStatusModel.FAILED
      event.errorMessage = exception.message ?: "Unknown error"
      logger.error("Event processing failed after $MAX_RETRY_COUNT retries: ${event.id}", exception)
    } else {
      // 计算下次重试时间
      val delayIndex = minOf(event.retryCount - 1, RETRY_DELAYS.size - 1)
      val nextRunAt = LocalDateTime.now().plusSeconds(RETRY_DELAYS[delayIndex] / 1000)

      event.status = EventStatusModel.PENDING
      event.nextRunAt = nextRunAt
      event.errorMessage = exception.message ?: "Unknown error"

      logger.warn("Event processing failed, will retry in ${RETRY_DELAYS[delayIndex]}ms: ${event.id}", exception)
    }

    domainEventRepository.save(event)
  }

  /**
   * 清理已完成的事件
   * 每小时执行一次，清理30天前的已完成事件
   */
  @Scheduled(fixedRate = 3600000) // 1 hour
  @Transactional
  fun cleanupCompletedEvents() {
    try {
      val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
      domainEventRepository.deleteByStatusAndCreatedAtBefore(EventStatusModel.COMPLETED, thirtyDaysAgo)
      logger.info("Cleaned up completed events older than 30 days")
    } catch (e: Exception) {
      logger.error("Error cleaning up completed events", e)
    }
  }
}
