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

package dev.yidafu.aqua.notification.gateway

import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import dev.yidafu.aqua.common.messaging.outbox.OutboxStatus
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * 通知消息 outbox 处理器委托测试:payload 反序列化 → 网关;失败上抛(退避重试)。
 */
class NotificationMessageConsumerTest :
  FunSpec({
    val gateway: NotificationGatewayService = mockk()
    val consumer = NotificationMessageConsumer(gateway)
    val objectMapper = jacksonObjectMapper()

    fun recordOf(event: DomainEvent) =
      OutboxEventRecord(
        id = 7L,
        eventType = event.eventType,
        destination = EventQueueConst.NOTIFICATIONS_QUEUE_NAME,
        subscriber = null,
        payload = objectMapper.writeValueAsString(event),
        nextRunAt = LocalDateTime.now(),
        status = OutboxStatus.PROCESSING,
        retryCount = 0,
      )

    test("反序列化 DomainEvent 并委托网关") {
      val event = DomainEvent(id = 9L, eventType = "NOTIFICATION_SEND", payload = """{"scene":"NEW_ORDER"}""")
      every { gateway.handle(any()) } returns Unit

      consumer.handle(recordOf(event))

      verify {
        gateway.handle(
          match { it.eventType == "NOTIFICATION_SEND" && it.id == 9L },
        )
      }
    }

    test("网关失败异常上抛(outbox 退避重试)") {
      val event = DomainEvent(id = 9L, eventType = "NOTIFICATION_SEND", payload = "{}")
      every { gateway.handle(any()) } throws RuntimeException("db down")

      shouldThrow<RuntimeException> { consumer.handle(recordOf(event)) }
    }
  })
