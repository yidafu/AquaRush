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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelRecoveryRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * 水桶押金变更服务单元测试(R-MON-03:按订单退还=HELD→REFUNDED 条件更新幂等)。
 */
class DepositMutationApiServiceImplTest :
  FunSpec({
    var depositRecordRepository: DepositRecordRepository = mockk()
    var barrelFeeRepository: DepositPriceRepository = mockk()
    var barrelRecoveryRecordRepository: BarrelRecoveryRecordRepository = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: DepositMutationApiServiceImpl = mockk()

    fun held(id: Long = 1L) =
      DepositRecordModel(
        id = id,
        orderId = 10L,
        userId = 100L,
        depositAmountCents = 500,
        status = DepositStatus.HELD,
      )

    beforeTest {
      depositRecordRepository = mockk(relaxed = true)
      barrelFeeRepository = mockk(relaxed = true)
      barrelRecoveryRecordRepository = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service =
        DepositMutationApiServiceImpl(
          depositRecordRepository,
          barrelFeeRepository,
          barrelRecoveryRecordRepository,
          productQueryApiService,
          bizLogger,
        )
    }

    test("markRefundedByOrder refunds all held records of order") {
      val r1 = held(id = 1L)
      val r2 = held(id = 2L)
      every { depositRecordRepository.findByOrderIdAndStatus(10L, DepositStatus.HELD) } returns listOf(r1, r2)
      every { depositRecordRepository.markRefundedIfHeld(any(), any(), any()) } returns 2

      val refunded = service.markRefundedByOrder(10L, refundedBy = 7L, remark = "订单取消自动退还")

      refunded.size shouldBe 2
      refunded.all { it.status == DepositStatus.REFUNDED } shouldBe true
      refunded.all { it.refundedBy == 7L } shouldBe true
    }

    test("markRefundedByOrder is no-op without held records") {
      every { depositRecordRepository.findByOrderIdAndStatus(10L, DepositStatus.HELD) } returns emptyList()

      service.markRefundedByOrder(10L, refundedBy = 7L, remark = null) shouldBe emptyList()
    }

    test("markRefundedByOrder is idempotent when concurrent refund already flipped rows") {
      every { depositRecordRepository.findByOrderIdAndStatus(10L, DepositStatus.HELD) } returns listOf(held(id = 1L))
      every { depositRecordRepository.markRefundedIfHeld(any(), any(), any()) } returns 0

      service.markRefundedByOrder(10L, refundedBy = 7L, remark = null) shouldBe emptyList()
    }
  })
