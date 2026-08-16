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

package dev.yidafu.aqua.payment.service

import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/**
 * 支付超时扫描测试(R-ORD-04):
 * 过期 PENDING 支付记录 → 触发订单支付超时处理并标记 EXPIRED;未过期/已支付竞态不误标。
 */
class PaymentExpirySchedulerTest :
  FunSpec({
    var paymentRepository: PaymentRepository = mockk()
    var paymentService: PaymentService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var scheduler: PaymentExpiryScheduler = mockk()

    beforeTest {
      paymentRepository = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      scheduler = PaymentExpiryScheduler(paymentRepository, paymentService, orderQueryService)
      every { paymentRepository.save(any()) } answers { firstArg() }
    }
    test("expiresPendingPaymentAndDelegatesTimeout") {
      val payment =
        PaymentModel(
          orderId = 1L,
          userId = 100L,
          amount = 1200,
          status = PaymentStatus.PENDING,
          expiredAt = LocalDateTime.now().minusMinutes(1),
        )
      every { paymentRepository.findByStatus(PaymentStatus.PENDING) } returns listOf(payment)
      every { orderQueryService.getOrderById(1L) } returns
        OrderModel(
          id = 1L,
          orderNo = "ORD-1",
          userId = 100L,
          amountCents = 1000,
          status = OrderModelStatus.CANCELLED,
        )

      scheduler.expirePendingPayments()

      verify(exactly = 1) { paymentService.handlePaymentTimeout(1L) }
      verify { paymentRepository.save(match { it.status == PaymentStatus.EXPIRED }) }
    }
    test("skipsNonExpiredPayments") {
      val payment =
        PaymentModel(
          orderId = 1L,
          userId = 100L,
          amount = 1200,
          status = PaymentStatus.PENDING,
          expiredAt = LocalDateTime.now().plusMinutes(10),
        )
      every { paymentRepository.findByStatus(PaymentStatus.PENDING) } returns listOf(payment)

      scheduler.expirePendingPayments()

      verify(exactly = 0) { paymentService.handlePaymentTimeout(any()) }
    }
    test("syncsSuccessWhenOrderAlreadyPaid") {
      val payment =
        PaymentModel(
          orderId = 1L,
          userId = 100L,
          amount = 1200,
          status = PaymentStatus.PENDING,
          expiredAt = LocalDateTime.now().minusMinutes(1),
        )
      every { paymentRepository.findByStatus(PaymentStatus.PENDING) } returns listOf(payment)
      every { orderQueryService.getOrderById(1L) } returns
        OrderModel(
          id = 1L,
          orderNo = "ORD-1",
          userId = 100L,
          amountCents = 1000,
          deliveryFeeCents = 200,
          status = OrderModelStatus.PENDING_DELIVERY,
          paymentTransactionId = "WX-1",
          paymentTime = LocalDateTime.now(),
        )

      scheduler.expirePendingPayments()

      verify { paymentRepository.save(match { it.status == PaymentStatus.SUCCESS }) }
      payment.transactionId shouldBe "WX-1"
    }
  })
