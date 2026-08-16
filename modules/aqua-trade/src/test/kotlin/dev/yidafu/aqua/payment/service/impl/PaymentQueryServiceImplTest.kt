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

package dev.yidafu.aqua.payment.service.impl

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 支付/退款查询服务单元测试(PRD 08-statistics.md §F-STA-002 退款/净营收维度):
 * 支付记录按时间区间查询;退款聚合委托支付域仓储,口径 = payment_refunds status=COMPLETED。
 */
class PaymentQueryServiceImplTest :
  FunSpec({
    var paymentRepository: PaymentRepository = mockk()
    var paymentRefundRepository: PaymentRefundRepository = mockk()
    var service: PaymentQueryServiceImpl = mockk()

    beforeTest {
      paymentRepository = mockk(relaxed = true)
      paymentRefundRepository = mockk(relaxed = true)
      service = PaymentQueryServiceImpl(paymentRepository, paymentRefundRepository)
    }
    test("getByDateRange delegates to repository") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      val payments = listOf(PaymentModel(orderId = 1L, userId = 100L, amount = 1200))
      every { paymentRepository.findByCreatedAtBetween(start, end) } returns payments

      service.getByDateRange(start, end) shouldBe payments
      verify(exactly = 1) { paymentRepository.findByCreatedAtBetween(start, end) }
    }
    test("empty range returns empty list") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      every { paymentRepository.findByCreatedAtBetween(start, end) } returns emptyList()

      service.getByDateRange(start, end) shouldBe emptyList()
    }
    test("sumRefundedAmountCents delegates to refund repository") {
      val start = LocalDateTime.of(2025, 6, 1, 0, 0)
      val end = LocalDateTime.of(2025, 6, 30, 23, 59, 59)
      every { paymentRefundRepository.sumRefundedAmountByProcessedAtBetween(start, end) } returns 500L

      service.sumRefundedAmountCents(start, end) shouldBe 500L
      verify(exactly = 1) { paymentRefundRepository.sumRefundedAmountByProcessedAtBetween(start, end) }
    }
    test("getDailyRefundedAmountCents delegates to refund repository") {
      val start = LocalDateTime.of(2025, 6, 1, 0, 0)
      val end = LocalDateTime.of(2025, 6, 30, 23, 59, 59)
      val daily = mapOf(LocalDate.of(2025, 6, 3) to 200L, LocalDate.of(2025, 6, 10) to 300L)
      every { paymentRefundRepository.sumRefundedAmountGroupByDay(start, end) } returns daily

      service.getDailyRefundedAmountCents(start, end) shouldBe daily
      verify(exactly = 1) { paymentRefundRepository.sumRefundedAmountGroupByDay(start, end) }
    }
  })
