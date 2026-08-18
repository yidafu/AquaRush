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

package dev.yidafu.aqua.deposit.service.impl

import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.deposit.domain.repository.DepositRecordRepository
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * 历史桶押金变更服务单元测试(G06,2026-08 水桶费改造后仅保留按订单退还):
 * 新购桶(水桶费)走 BarrelPurchaseMutationServiceImpl。
 */
class DepositMutationApiServiceImplTest :
  FunSpec({
    var depositRecordRepository: DepositRecordRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: DepositMutationApiServiceImpl = mockk()

    fun collected(id: Long = 1L) =
      DepositRecordModel(
        id = id,
        orderId = 10L,
        userId = 100L,
        depositAmountCents = 500,
        status = DepositStatus.COLLECTED,
      )

    beforeTest {
      depositRecordRepository = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service = DepositMutationApiServiceImpl(depositRecordRepository, bizLogger)
      every { depositRecordRepository.saveAll(any<Iterable<DepositRecordModel>>()) } answers
        { firstArg<Iterable<DepositRecordModel>>().toList() }
    }

    test("markRefundedByOrder refunds all collected records of order (历史兼容)") {
      val r1 = collected(id = 1L)
      val r2 = collected(id = 2L)
      every { depositRecordRepository.findByOrderIdAndStatus(10L, DepositStatus.COLLECTED) } returns listOf(r1, r2)

      val saved = service.markRefundedByOrder(10L, refundedBy = 7L, remark = "订单取消自动退还")

      saved.size shouldBe 2
      saved.all { it.status == DepositStatus.REFUNDED } shouldBe true
      saved.all { it.refundedBy == 7L } shouldBe true
    }

    test("markRefundedByOrder is no-op without collected records") {
      every { depositRecordRepository.findByOrderIdAndStatus(10L, DepositStatus.COLLECTED) } returns emptyList()

      service.markRefundedByOrder(10L, refundedBy = 7L, remark = null) shouldBe emptyList()
    }
  })
