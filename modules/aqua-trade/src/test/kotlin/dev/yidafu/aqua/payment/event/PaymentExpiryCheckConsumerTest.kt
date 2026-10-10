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

package dev.yidafu.aqua.payment.event

import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRecord
import dev.yidafu.aqua.common.messaging.outbox.OutboxStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

/**
 * 支付到期检查 outbox 处理器测试(R-ORD-04 支付超时优化):
 * 解析 PAYMENT_EXPIRY_CHECK payload 的 orderId → 触发 handlePaymentTimeout;
 * 非法 payload 异常上抛(outbox 退避重试,消费端幂等由 CAS 认领保证)。
 */
class PaymentExpiryCheckConsumerTest :
  FunSpec({
    var paymentService: PaymentService = mockk()
    var consumer: PaymentExpiryCheckConsumer = mockk()
    val objectMapper = jacksonObjectMapper()

    fun recordOf(event: DomainEvent) =
      OutboxEventRecord(
        id = 1L,
        eventType = event.eventType,
        destination = "payment-expiry-events",
        subscriber = null,
        payload = objectMapper.writeValueAsString(event),
        nextRunAt = LocalDateTime.now(),
        status = OutboxStatus.PROCESSING,
        retryCount = 0,
      )

    beforeTest {
      paymentService = mockk(relaxed = true)
      consumer = PaymentExpiryCheckConsumer(paymentService)
    }
    test("parsesOrderIdAndDelegatesTimeout") {
      val event =
        DomainEvent(
          eventType = "PAYMENT_EXPIRY_CHECK",
          payload = """{"orderId":123,"userId":100,"expireAt":"2026-01-01T00:00:00","timestamp":1}""",
        )

      consumer.handle(recordOf(event))

      verify(exactly = 1) { paymentService.handlePaymentTimeout(123L) }
    }
    test("malformedPayloadRethrowsForRetry") {
      val event = DomainEvent(eventType = "PAYMENT_EXPIRY_CHECK", payload = "not-json")

      shouldThrow<Exception> { consumer.handle(recordOf(event)) }
      verify(exactly = 0) { paymentService.handlePaymentTimeout(any()) }
    }
    test("missingOrderIdRethrowsForRetry") {
      val event = DomainEvent(eventType = "PAYMENT_EXPIRY_CHECK", payload = """{"userId":100}""")

      shouldThrow<Exception> { consumer.handle(recordOf(event)) }
      verify(exactly = 0) { paymentService.handlePaymentTimeout(any()) }
    }
  })
