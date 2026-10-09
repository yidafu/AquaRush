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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class OrderQueryDeliveredStatsTest :
  FunSpec({
    val start = LocalDateTime.of(2025, 6, 1, 0, 0)
    val end = LocalDateTime.of(2025, 6, 1, 23, 59)
    val pageable = PageRequest.of(0, 10)

    test("送达口径聚合方法委托 OrderRepository") {
      val repo = mockk<OrderRepository>()
      val impl = OrderQueryServiceImpl(repo)
      val order = mockk<OrderModel>()
      val page = PageImpl(listOf(order))
      every { repo.countDeliveredByDateRange(start, end, 1L) } returns 6L
      every { repo.sumDeliveredAmountCentsByDateRange(start, end, 1L) } returns 1200L
      every { repo.countAndSumDeliveredByPaymentType(1L, PaymentType.CASH, start, end) } returns arrayOf(3L, 3000L)
      every { repo.findDeliveredOrdersByDateRange(1L, start, end, pageable) } returns page

      impl.countDeliveredByDateRange(start, end, 1L) shouldBe 6L
      impl.sumDeliveredAmountCentsByDateRange(start, end, 1L) shouldBe 1200L
      impl.countAndSumDeliveredByPaymentType(1L, PaymentType.CASH, start, end).contentEquals(arrayOf(3L, 3000L)) shouldBe true
      impl.findDeliveredOrdersByDateRange(1L, start, end, pageable) shouldBe page
    }
  })
