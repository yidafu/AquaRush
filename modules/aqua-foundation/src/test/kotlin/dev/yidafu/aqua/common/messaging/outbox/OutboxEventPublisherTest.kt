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

package dev.yidafu.aqua.common.messaging.outbox

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.publisher.EventTypePublisherType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * OutboxEventPublisher 有界单测:路由、延迟 next_run_at、广播扇出、异常传播。
 */
class OutboxEventPublisherTest :
  FunSpec({
    var repository: OutboxEventRepository = mockk()
    var publisher: OutboxEventPublisher = mockk()

    val insertId = slot<Long>()
    val insertDestination = slot<String>()
    val insertPayload = slot<String>()
    val insertNextRunAt = slot<LocalDateTime>()

    beforeTest {
      repository = mockk(relaxed = true)
      publisher = OutboxEventPublisher(repository)
      every {
        repository.insert(
          id = capture(insertId),
          eventType = any(),
          destination = capture(insertDestination),
          subscriber = any(),
          payload = capture(insertPayload),
          nextRunAt = capture(insertNextRunAt),
        )
      } returns Unit
    }

    test("publishSync 按既有路由写入目标队列,subscriber 为空(竞争消费)") {
      val event =
        DomainEvent(
          id = 1001L,
          aggregateId = "42",
          eventType = DomainEventType.ORDER_CREATED.value,
          payload = """{"orderId":42}""",
        )

      publisher.publishSync(event) shouldBe true

      insertId.captured shouldBe 1001L
      insertDestination.captured shouldBe "order-events"
      insertPayload.captured shouldContain DomainEventType.ORDER_CREATED.value
      insertPayload.captured shouldContain "42"
      verify {
        repository.insert(
          id = 1001L,
          eventType = any(),
          destination = any(),
          subscriber = null,
          payload = any(),
          nextRunAt = any(),
        )
      }
    }

    test("PAYMENT_EXPIRY_CHECK 路由到独立 payment-expiry-events 队列") {
      val event =
        DomainEvent(
          id = 1002L,
          eventType = DomainEventType.PAYMENT_EXPIRY_CHECK.value,
          payload = "{}",
        )

      publisher.publishSync(event)

      insertDestination.captured shouldBe "payment-expiry-events"
    }

    test("publishDelayed 写未来 next_run_at(取代 Artemis 延迟投递)") {
      val event = DomainEvent(id = 1003L, eventType = DomainEventType.ORDER_PAID.value, payload = "{}")
      val deliveryMillis = System.currentTimeMillis() + 60_000

      publisher.publishDelayed(event, deliveryMillis) shouldBe true

      val expected =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(deliveryMillis), ZoneId.systemDefault())
      insertNextRunAt.captured shouldBe expected
    }

    test("publishToSubscribers 为每个订阅服务各写一行(广播扇出)") {
      publisher.publishToSubscribers(
        eventType = OutboxEventTypes.PUSH_EVENT,
        payload = """{"broadcast":true}""",
        subscribers = listOf("admin", "client"),
      ) shouldBe true

      verify(exactly = 2) {
        repository.insert(
          id = any(),
          eventType = any(),
          destination = any(),
          subscriber = any(),
          payload = any(),
          nextRunAt = any(),
        )
      }
      verify {
        repository.insert(
          id = any(),
          eventType = OutboxEventTypes.PUSH_EVENT,
          destination = "broadcast-events",
          subscriber = "admin",
          payload = any(),
          nextRunAt = any(),
        )
      }
      verify {
        repository.insert(
          id = any(),
          eventType = OutboxEventTypes.PUSH_EVENT,
          destination = "broadcast-events",
          subscriber = "client",
          payload = any(),
          nextRunAt = any(),
        )
      }
    }

    test("repository 异常向上传播(outbox 原子性,不吞异常)") {
      every { repository.insert(any(), any(), any(), any(), any(), any()) } throws RuntimeException("db down")
      val event = DomainEvent(id = 1004L, eventType = DomainEventType.ORDER_CREATED.value, payload = "{}")

      shouldThrow<RuntimeException> { publisher.publishSync(event) }
    }

    test("getType/getName 标识 outbox 实现") {
      publisher.getType() shouldBe EventTypePublisherType.OUTBOX
      publisher.getName() shouldBe "OutboxEventPublisher"
      publisher.isAvailable() shouldBe true
    }
  })
