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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify

/**
 * 支付到期检查延迟消息消费者测试(R-ORD-04 支付超时优化):
 * 解析 PAYMENT_EXPIRY_CHECK payload 的 orderId → 触发 handlePaymentTimeout;
 * 非法 payload 异常上抛(触发 Artemis 重投,消费端幂等由 CAS 认领保证)。
 */
class PaymentExpiryCheckConsumerTest :
  FunSpec({
    var paymentService: PaymentService = mockk()
    var consumer: PaymentExpiryCheckConsumer = mockk()

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

      consumer.handlePaymentExpiryCheck(event)

      verify(exactly = 1) { paymentService.handlePaymentTimeout(123L) }
    }
    test("malformedPayloadRethrowsForRedelivery") {
      val event = DomainEvent(eventType = "PAYMENT_EXPIRY_CHECK", payload = "not-json")

      shouldThrow<Exception> { consumer.handlePaymentExpiryCheck(event) }
      verify(exactly = 0) { paymentService.handlePaymentTimeout(any()) }
    }
    test("missingOrderIdRethrowsForRedelivery") {
      val event = DomainEvent(eventType = "PAYMENT_EXPIRY_CHECK", payload = """{"userId":100}""")

      shouldThrow<Exception> { consumer.handlePaymentExpiryCheck(event) }
      verify(exactly = 0) { paymentService.handlePaymentTimeout(any()) }
    }
  })
