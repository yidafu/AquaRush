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
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import dev.yidafu.aqua.common.messaging.outbox.OutboxStatus
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * 业务领域事件 outbox 处理器测试:四队列订阅、按 DomainEventType 分发到全部处理器、
 * 失败上抛、未知事件类型不视为失败。
 */
class DomainEventOutboxHandlerTest :
  FunSpec({
    val objectMapper = jacksonObjectMapper()

    fun recordOf(
      event: DomainEvent,
      destination: String = EventQueueConst.ORDER_QUEUE_NAME,
    ) = OutboxEventRecord(
      id = event.id ?: 1L,
      eventType = event.eventType,
      destination = destination,
      subscriber = null,
      payload = objectMapper.writeValueAsString(event),
      nextRunAt = LocalDateTime.now(),
      status = OutboxStatus.PROCESSING,
      retryCount = 0,
    )

    test("订阅四个业务队列") {
      val handler = DomainEventOutboxHandler(emptyList())
      handler.destinations shouldContainExactly
        setOf(
          EventQueueConst.ORDER_QUEUE_NAME,
          EventQueueConst.PAYMENT_QUEUE_NAME,
          EventQueueConst.DELIVERY_QUEUE_NAME,
          EventQueueConst.USER_QUEUE_NAME,
        )
    }

    test("按事件类型分发到全部匹配处理器") {
      val first: EventProcessor = mockk()
      val second: EventProcessor = mockk()
      every { first.getSupportedEventType() } returns DomainEventType.ORDER_CREATED
      every { second.getSupportedEventType() } returns DomainEventType.ORDER_CREATED
      every { first.handle(any()) } returns Unit
      every { second.handle(any()) } returns Unit
      val handler = DomainEventOutboxHandler(listOf(first, second))

      val event = DomainEvent(id = 5L, eventType = DomainEventType.ORDER_CREATED.value, payload = "{}")
      handler.handle(recordOf(event))

      verify(exactly = 1) { first.handle(match { it.id == 5L }) }
      verify(exactly = 1) { second.handle(match { it.id == 5L }) }
    }

    test("处理器失败异常上抛(outbox 退避重试)") {
      val failing: EventProcessor = mockk()
      every { failing.getSupportedEventType() } returns DomainEventType.ORDER_PAID
      every { failing.handle(any()) } throws RuntimeException("boom")
      val handler = DomainEventOutboxHandler(listOf(failing))

      val event = DomainEvent(id = 6L, eventType = DomainEventType.ORDER_PAID.value, payload = "{}")
      io.kotest.assertions.throwables
        .shouldThrow<RuntimeException> { handler.handle(recordOf(event)) }
    }

    test("未知事件类型仅告警,不视为失败") {
      val handler = DomainEventOutboxHandler(emptyList())
      val event = DomainEvent(id = 7L, eventType = "NOT_A_KNOWN_TYPE", payload = "{}")

      handler.handle(recordOf(event))
    }
  })
