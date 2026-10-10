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

package dev.yidafu.aqua.notification.channel.ws

import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import dev.yidafu.aqua.common.messaging.outbox.OutboxStatus
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * WebSocket 推送 outbox 处理器委托测试:定向/广播/best-effort(失败不重试)。
 */
class NotificationWsPushConsumerTest :
  FunSpec({
    val hub: NotificationWsHub = mockk(relaxed = true)
    val consumer = NotificationWsPushConsumer(hub)
    val objectMapper = jacksonObjectMapper()

    fun recordOf(payload: String) =
      OutboxEventRecord(
        id = 1L,
        eventType = "PUSH_EVENT",
        destination = PushEvent.TOPIC,
        subscriber = "client",
        payload = payload,
        nextRunAt = LocalDateTime.now(),
        status = OutboxStatus.PROCESSING,
        retryCount = 0,
      )

    test("定向推送按接收人分发到 hub") {
      val payload =
        objectMapper.writeValueAsString(
          PushEvent(receiverType = "USER", receiverId = 42L, payload = mapOf("k" to "v")),
        )
      every { hub.push(NotificationReceiverType.USER, 42L, any()) } returns 1

      consumer.handle(recordOf(payload))

      verify(exactly = 1) { hub.push(NotificationReceiverType.USER, 42L, any()) }
      verify(exactly = 0) { hub.broadcastToWorkers(any()) }
    }

    test("广播推送走 broadcastToWorkers") {
      val payload =
        objectMapper.writeValueAsString(
          PushEvent(receiverType = "WORKER", broadcast = true, payload = mapOf("k" to "v")),
        )

      consumer.handle(recordOf(payload))

      verify(exactly = 1) { hub.broadcastToWorkers(any()) }
    }

    test("hub 失败仅记日志不外抛(best-effort,由轮询器置 DONE)") {
      val payload =
        objectMapper.writeValueAsString(
          PushEvent(receiverType = "USER", receiverId = 1L, payload = emptyMap()),
        )
      every { hub.push(any(), any(), any()) } throws RuntimeException("session closed")

      io.kotest.assertions.throwables
        .shouldNotThrowAny { consumer.handle(recordOf(payload)) }
    }
  })
