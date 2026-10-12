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
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/**
 * 支付结果主动查单兜底调度测试(官方《支付回调和查单实现指引》§5.2 定时轮询查单):
 * id keyset 游标分批扫描 PENDING 且未到期的支付记录 → 逐条委托 queryPaymentStatus;
 * 空批终止、单条失败不影响后续、游标跨批推进;查单与状态流转逻辑见 PaymentServiceImplTest。
 */
class PaymentStatusPollSchedulerTest :
  FunSpec({
    var paymentRepository: PaymentRepository = mockk()
    var paymentService: PaymentService = mockk()
    var scheduler: PaymentStatusPollScheduler = mockk()

    fun payment(
      id: Long,
      orderId: Long,
    ) = PaymentModel(
      id = id,
      orderId = orderId,
      userId = 100L,
      amount = 1200,
      status = PaymentStatus.PENDING,
      expiredAt = LocalDateTime.now().plusMinutes(30),
    )

    beforeTest {
      paymentRepository = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      scheduler = PaymentStatusPollScheduler(paymentRepository, paymentService)
    }
    test("pollsEachPendingPaymentViaQueryPaymentStatus") {
      every {
        paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), 0L, any())
      } returns listOf(payment(1L, 11L), payment(2L, 22L))

      scheduler.pollPendingPayments()

      verify(exactly = 1) { paymentService.queryPaymentStatus(11L) }
      verify(exactly = 1) { paymentService.queryPaymentStatus(22L) }
    }
    test("skipsWhenNoPendingPayments") {
      every {
        paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), any(), any())
      } returns emptyList()

      scheduler.pollPendingPayments()

      verify(exactly = 0) { paymentService.queryPaymentStatus(any()) }
      verify(exactly = 1) { paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), 0L, any()) }
    }
    test("continuesScanningWhenSinglePaymentFails") {
      every {
        paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), any(), any())
      } returns listOf(payment(1L, 11L), payment(2L, 22L))
      every { paymentService.queryPaymentStatus(11L) } throws RuntimeException("查单超时")

      scheduler.pollPendingPayments()

      verify(exactly = 1) { paymentService.queryPaymentStatus(11L) }
      verify(exactly = 1) { paymentService.queryPaymentStatus(22L) }
    }
    test("advancesCursorAcrossBatches") {
      // 第一批恰好满批(100 行)→ 游标推进到批尾 id 再取下一批;第二批未满批 → 终止
      val fullBatch = (1L..100L).map { payment(it, 1000L + it) }
      val secondBatch = listOf(payment(200L, 1200L))
      every {
        paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), 0L, any())
      } returns fullBatch
      every {
        paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), 100L, any())
      } returns secondBatch

      scheduler.pollPendingPayments()

      verify(exactly = 1) { paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), 100L, any()) }
      verify(exactly = 1) { paymentService.queryPaymentStatus(1200L) }
    }
    test("cursorQueryFailurePropagatesForRetryNextCycle") {
      every {
        paymentRepository.findPollableByStatusAfterId(PaymentStatus.PENDING, any(), any(), any())
      } throws RuntimeException("db down")

      shouldThrow<RuntimeException> { scheduler.pollPendingPayments() }
    }
  })
