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

import dev.yidafu.aqua.api.dto.BarrelLedgerRow
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelLedgerRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRefundApplicationRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

/**
 * 押金查询服务单元测试(R-PRO-04 水桶账户聚合口径 + 列表委托)。
 */
class DepositQueryServiceImplTest :
  FunSpec({
    var depositRecordRepository: DepositRecordRepository = mockk()
    var applicationRepository: DepositRefundApplicationRepository = mockk()
    var ledgerRepository: BarrelLedgerRepository = mockk()
    var service: DepositQueryServiceImpl = mockk()

    fun sum(
      spec: String,
      buckets: Long,
      cents: Long = 0L,
    ) = BarrelLedgerRepository.SpecBucketSum(userId = 100L, specification = spec, bucketCount = buckets, amountCents = cents)

    beforeTest {
      depositRecordRepository = mockk(relaxed = true)
      applicationRepository = mockk(relaxed = true)
      ledgerRepository = mockk(relaxed = true)
      service = DepositQueryServiceImpl(depositRecordRepository, applicationRepository, ledgerRepository)
    }

    fun record() =
      DepositRecordModel(
        id = 1L,
        orderId = 10L,
        userId = 100L,
        depositAmountCents = 4000,
        specification = "19L",
        unitDepositCents = 4000,
        bucketCount = 1,
        status = DepositStatus.HELD,
      )

    test("barrelAccount 聚合在租/欠桶/押金合计并过滤零行规格") {
      every { ledgerRepository.sumHeldByUserAndSpec(listOf(100L)) } returns
        listOf(sum("19L", buckets = 2, cents = 8000L))
      every { ledgerRepository.sumDeliveredByUserAndSpec(listOf(100L)) } returns
        listOf(sum("19L", buckets = 3), sum("5L", buckets = 1))
      every { ledgerRepository.sumRecoveredByUserAndSpec(listOf(100L)) } returns
        listOf(sum("19L", buckets = 1))
      every { depositRecordRepository.pageByUserId(100L, null, PageRequest.of(0, 10)) } returns
        PageImpl(listOf(record()), PageRequest.of(0, 10), 1)

      val account = service.barrelAccount(userId = 100L, page = 0, size = 10)

      // 19L:在租 2,欠 3-1=2;5L:在租 0,欠 1-0=1
      account.specs.size shouldBe 2
      val s19 = account.specs.first { it.specification == "19L" }
      s19.heldBucketCount shouldBe 2
      s19.owedBucketCount shouldBe 2
      s19.depositHeldCents shouldBe 8000L
      val s5 = account.specs.first { it.specification == "5L" }
      s5.heldBucketCount shouldBe 0
      s5.owedBucketCount shouldBe 1

      account.depositHeldCents shouldBe 8000L
      account.owedBucketCount shouldBe 3
      account.records.totalElements shouldBe 1L
    }

    test("barrelAccount 欠桶不为负(回收多于交付钳 0)") {
      every { ledgerRepository.sumHeldByUserAndSpec(listOf(100L)) } returns emptyList()
      every { ledgerRepository.sumDeliveredByUserAndSpec(listOf(100L)) } returns listOf(sum("19L", buckets = 1))
      every { ledgerRepository.sumRecoveredByUserAndSpec(listOf(100L)) } returns listOf(sum("19L", buckets = 5))

      val account = service.barrelAccount(userId = 100L, page = 0, size = 10)
      // 在租 0 且欠桶钳 0 → 无账户行,合计 0
      account.specs.size shouldBe 0
      account.owedBucketCount shouldBe 0
      account.depositHeldCents shouldBe 0L
    }

    test("列表委托仓储(用户本人 / 后台 / 记录检索 / 台账)") {
      every { applicationRepository.search(eq(100L), any(), any()) } returns PageImpl(emptyList(), PageRequest.of(0, 10), 0)
      every { applicationRepository.search(null, any(), any()) } returns PageImpl(emptyList(), PageRequest.of(0, 10), 0)
      every { depositRecordRepository.search(any(), any(), any(), any()) } returns PageImpl(emptyList(), PageRequest.of(0, 10), 0)
      val ledgerRow =
        BarrelLedgerRow(
          userId = 1L,
          nickname = "张三",
          phone = "13800000000",
          heldBucketCount = 1,
          depositHeldCents = 4000,
          depositRefundedCents = 0,
          deliveredBucketCount = 2,
          recoveredBucketCount = 1,
          owedBucketCount = 1,
        )
      every { ledgerRepository.pageLedger(null, false, 0, 20) } returns
        PageImpl(listOf(ledgerRow), PageRequest.of(0, 20), 1)

      service.listUserApplications(100L, null, 0, 10).totalElements shouldBe 0L
      service.listApplications(null, 0, 10).totalElements shouldBe 0L
      service.searchDepositRecords(100L, DepositStatus.HELD, "19L", 0, 20).totalElements shouldBe 0L
      service
        .barrelLedger(null, false, 0, 20)
        .content
        .single()
        .nickname shouldBe "张三"
    }
  })
