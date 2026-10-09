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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 购票单仓储 Exposed 实现测试(H2):save/软删/派生查询/按日销售聚合/支付入账原子流转。
 */
class TicketPurchaseRepositoryImplTest :
  FunSpec({
    val repo = TicketPurchaseRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 双分支 + findByOrderNo/count/findById 软删过滤") {
      val saved =
        repo.save(
          TicketPurchaseModel(userId = 100L, ticketTypeId = 1L, orderNo = "TK-001", totalCount = 10, packagePriceCents = 1000L),
        )
      saved.id.shouldNotBeNull()
      repo.findByOrderNo("TK-001").shouldNotBeNull().userId shouldBe 100L

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.paymentStatus = TicketPurchaseStatus.PAID
      loaded.paidAt = LocalDateTime.of(2026, 3, 1, 12, 0)
      repo.save(loaded)
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .paymentStatus shouldBe TicketPurchaseStatus.PAID

      val gone = MarketingExposedH2TestSupport.ticketPurchase(userId = 1L, ticketTypeId = 1L, deleted = true)
      repo.findById(gone).isPresent shouldBe false
      repo.findByOrderNo("TK-no") shouldBe null
      repo.countByUserIdAndTicketTypeIdAndPaymentStatus(100L, 1L, TicketPurchaseStatus.PAID) shouldBe 1L
      repo.countByUserIdAndTicketTypeIdAndPaymentStatus(999L, 1L, TicketPurchaseStatus.PAID) shouldBe 0L
    }

    test("findPaidSalesGroupedByDay:按日聚合 paidAt 回退 createdAt,[from,to),空日不出现") {
      val d1 = LocalDate.of(2026, 3, 1)
      val d2 = LocalDate.of(2026, 3, 2)
      val tid = 7L
      // d1:两笔已支付(paidAt 落 d1)
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 1L,
        ticketTypeId = tid,
        packageCount = 1,
        totalCount = 10,
        packagePriceCents = 1000L,
        paymentStatus = "PAID",
        paidAt = d1.atTime(10, 0),
        createdAt = d1.atTime(9, 0),
      )
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 1L,
        ticketTypeId = tid,
        packageCount = 1,
        totalCount = 5,
        packagePriceCents = 500L,
        paymentStatus = "PAID",
        paidAt = d1.atTime(18, 0),
        createdAt = d1.atTime(9, 0),
      )
      // d2:paidAt 为 null → 回退 createdAt
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 2L,
        ticketTypeId = tid,
        totalCount = 3,
        packagePriceCents = 300L,
        paymentStatus = "PAID",
        paidAt = null,
        createdAt = d2.atTime(8, 0),
      )
      // PENDING 不计;他票种不计;软删不计
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 3L,
        ticketTypeId = tid,
        paymentStatus = "PENDING",
        paidAt = d1.atTime(12, 0),
        createdAt = d1.atTime(12, 0),
      )
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 4L,
        ticketTypeId = 8L,
        totalCount = 99,
        packagePriceCents = 999L,
        paymentStatus = "PAID",
        paidAt = d1.atTime(12, 0),
        createdAt = d1.atTime(12, 0),
      )
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 5L,
        ticketTypeId = tid,
        paymentStatus = "PAID",
        paidAt = d1.atTime(12, 0),
        createdAt = d1.atTime(12, 0),
        deleted = true,
      )

      val from = LocalDateTime.of(2026, 3, 1, 0, 0)
      val to = LocalDateTime.of(2026, 3, 3, 0, 0)
      val rows = repo.findPaidSalesGroupedByDay(tid, from, to)
      rows.size shouldBe 2
      val r1 = rows[0]
      r1.getDay() shouldBe d1
      r1.getPurchaseCount() shouldBe 2L
      r1.getTicketCount() shouldBe 15L
      r1.getRevenueCents() shouldBe 1500L
      val r2 = rows[1]
      r2.getDay() shouldBe d2
      r2.getPurchaseCount() shouldBe 1L
      r2.getTicketCount() shouldBe 3L
      r2.getRevenueCents() shouldBe 300L

      // 窗口外
      repo.findPaidSalesGroupedByDay(tid, LocalDateTime.of(2027, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 2, 0, 0)) shouldBe emptyList()
    }

    test("findPaidSalesGroupedByDay:边界 [from, to) — to 当日不计") {
      val tid = 9L
      MarketingExposedH2TestSupport.ticketPurchase(
        userId = 1L,
        ticketTypeId = tid,
        paymentStatus = "PAID",
        paidAt = LocalDateTime.of(2026, 3, 5, 0, 30),
        createdAt = LocalDateTime.of(2026, 3, 5, 0, 30),
      )
      repo
        .findPaidSalesGroupedByDay(tid, LocalDateTime.of(2026, 3, 5, 0, 0), LocalDateTime.of(2026, 3, 5, 0, 0))
        .shouldBe(emptyList())
      repo
        .findPaidSalesGroupedByDay(tid, LocalDateTime.of(2026, 3, 5, 0, 0), LocalDateTime.of(2026, 3, 5, 1, 0))
        .size shouldBe 1
    }

    test("markPaidAtomically:PENDING→PAID 幂等,软删/已处理 no-op") {
      val id =
        MarketingExposedH2TestSupport.ticketPurchase(
          userId = 100L,
          ticketTypeId = 1L,
          paymentStatus = "PENDING",
          paidAt = null,
        )
      val now = LocalDateTime.of(2026, 3, 1, 12, 0)
      repo.markPaidAtomically(id, "wx-1", now) shouldBe 1
      val paid = repo.findById(id).orElse(null).shouldNotBeNull()
      paid.paymentStatus shouldBe TicketPurchaseStatus.PAID
      paid.transactionId shouldBe "wx-1"
      paid.paidAt shouldBe now

      repo.markPaidAtomically(id, "wx-2", now.plusMinutes(1)) shouldBe 0

      val gone =
        MarketingExposedH2TestSupport.ticketPurchase(
          userId = 1L,
          ticketTypeId = 1L,
          paymentStatus = "PENDING",
          deleted = true,
        )
      repo.markPaidAtomically(gone, "wx", now) shouldBe 0
    }
  })
