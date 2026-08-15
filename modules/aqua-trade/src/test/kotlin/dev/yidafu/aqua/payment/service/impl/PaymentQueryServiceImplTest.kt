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
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 支付查询服务单元测试:按时间区间查询支付记录。
 */
class PaymentQueryServiceImplTest {
  private lateinit var paymentRepository: PaymentRepository
  private lateinit var service: PaymentQueryServiceImpl

  @BeforeEach
  fun setUp() {
    paymentRepository = mockk(relaxed = true)
    service = PaymentQueryServiceImpl(paymentRepository)
  }

  @Test
  @DisplayName("getByDateRange 透传仓储查询")
  fun `getByDateRange delegates to repository`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)
    val payments = listOf(PaymentModel(orderId = 1L, userId = 100L, amount = 1200))
    every { paymentRepository.findByCreatedAtBetween(start, end) } returns payments

    assertEquals(payments, service.getByDateRange(start, end))
    verify(exactly = 1) { paymentRepository.findByCreatedAtBetween(start, end) }
  }

  @Test
  @DisplayName("空区间返回空列表")
  fun `empty range returns empty list`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)
    every { paymentRepository.findByCreatedAtBetween(start, end) } returns emptyList()

    assertEquals(emptyList(), service.getByDateRange(start, end))
  }
}
