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

import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.payment.config.PaymentExpiryProperties
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 支付超时兜底扫描测试(R-ORD-04 支付超时优化):
 * 分页游标分批扫描 PENDING 且已到期的支付记录 → 逐条触发 handlePaymentTimeout;
 * 空页终止、单条失败不影响后续、多页推进;互斥与幂等由服务内 CAS 认领保证(见 PaymentServiceImplTest)。
 */
class PaymentExpirySchedulerTest :
  FunSpec({
    var paymentRepository: PaymentRepository = mockk()
    var paymentService: PaymentService = mockk()
    var properties: PaymentExpiryProperties = PaymentExpiryProperties()
    var scheduler: PaymentExpiryScheduler = mockk()

    fun payment(
      orderId: Long,
      expiredAt: LocalDateTime,
    ) = PaymentModel(
      orderId = orderId,
      userId = 100L,
      amount = 1200,
      status = PaymentStatus.PENDING,
      expiredAt = expiredAt,
    )

    beforeTest {
      paymentRepository = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      properties = PaymentExpiryProperties(scanBatchSize = 2)
      scheduler = PaymentExpiryScheduler(paymentRepository, paymentService, properties)
    }
    test("scansExpiredPaymentsAndDelegatesTimeout") {
      val now = LocalDateTime.now()
      every { paymentRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, any(), any()) } returns
        PageImpl(
          listOf(
            payment(1L, now.minusMinutes(1)),
            payment(2L, now.minusMinutes(2)),
          ),
        )

      scheduler.expirePendingPayments()

      verify(exactly = 1) { paymentService.handlePaymentTimeout(1L) }
      verify(exactly = 1) { paymentService.handlePaymentTimeout(2L) }
    }
    test("paginatesThroughAllExpiredPayments") {
      val now = LocalDateTime.now()
      // total=5, size=2 → 3 页(0/1/2);PageImpl.hasNext 由 pageable+total 计算
      val page0 = PageImpl(listOf(payment(1L, now.minusMinutes(1))), PageRequest.of(0, 2), 5)
      val page1 = PageImpl(listOf(payment(2L, now.minusMinutes(2))), PageRequest.of(1, 2), 5)
      val page2 = PageImpl(listOf(payment(3L, now.minusMinutes(3))), PageRequest.of(2, 2), 5)
      every { paymentRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, any(), any()) } returnsMany
        listOf(page0, page1, page2)

      scheduler.expirePendingPayments()

      verify(exactly = 1) { paymentService.handlePaymentTimeout(1L) }
      verify(exactly = 1) { paymentService.handlePaymentTimeout(2L) }
      verify(exactly = 1) { paymentService.handlePaymentTimeout(3L) }
    }
    test("skipsWhenNoExpiredPayments") {
      every { paymentRepository.findByStatusAndExpiredAtBefore(any(), any(), any()) } returns
        PageImpl(emptyList<PaymentModel>())

      scheduler.expirePendingPayments()

      verify(exactly = 0) { paymentService.handlePaymentTimeout(any()) }
    }
    test("continuesScanningWhenSinglePaymentFails") {
      val now = LocalDateTime.now()
      every { paymentRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, any(), any()) } returns
        PageImpl(
          listOf(
            payment(1L, now.minusMinutes(1)),
            payment(2L, now.minusMinutes(2)),
          ),
        )
      every { paymentService.handlePaymentTimeout(1L) } throws RuntimeException("boom")

      scheduler.expirePendingPayments()

      verify(exactly = 1) { paymentService.handlePaymentTimeout(1L) }
      verify(exactly = 1) { paymentService.handlePaymentTimeout(2L) }
    }
    test("pageQueryFailurePropagatesForRetryNextCycle") {
      every { paymentRepository.findByStatusAndExpiredAtBefore(any(), any(), any()) } throws RuntimeException("db down")

      shouldThrow<RuntimeException> { scheduler.expirePendingPayments() }
    }
  })
