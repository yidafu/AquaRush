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

package dev.yidafu.aqua.common.messaging.processor

import dev.yidafu.aqua.common.domain.model.DomainEvent
import dev.yidafu.aqua.common.messaging.config.SimplifiedMessagingProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 可重试消息处理器
 * 实现指数退避重试策略
 */
@Component
class RetryableMessageProcessor(
  private val messagingProperties: SimplifiedMessagingProperties,
) {
  private val logger = LoggerFactory.getLogger(RetryableMessageProcessor::class.java)

  /**
   * 带重试机制的消息处理
   */
  fun processWithRetry(
    event: DomainEvent,
    processor: (DomainEvent) -> Unit,
  ): Boolean {
    var attempt = 0
    val maxAttempts = messagingProperties.artemis.retry.maxAttempts
    var delay = messagingProperties.artemis.retry.initialInterval

    while (attempt < maxAttempts) {
      try {
        processor(event)
        return true
      } catch (e: Exception) {
        attempt++
        if (attempt >= maxAttempts) {
          logger.error("消息处理达到最大重试次数: {}, 事件类型: {}", maxAttempts, event.eventType, e)
          return false
        }

        logger.warn("消息处理失败，{}ms后进行第{}次重试，事件类型: {}", delay, attempt, event.eventType, e)
        try {
          Thread.sleep(delay)
        } catch (ie: InterruptedException) {
          Thread.currentThread().interrupt()
          logger.warn("重试等待被中断")
          return false
        }
        delay = (delay * messagingProperties.artemis.retry.multiplier).toLong()
        delay = minOf(delay, messagingProperties.artemis.retry.maxInterval)
      }
    }

    return false
  }

  /**
   * 批量消息处理带重试机制
   */
  fun processBatchWithRetry(
    events: List<DomainEvent>,
    processor: (DomainEvent) -> Unit,
  ): List<Boolean> =
    events.map { event ->
      processWithRetry(event, processor)
    }
}
