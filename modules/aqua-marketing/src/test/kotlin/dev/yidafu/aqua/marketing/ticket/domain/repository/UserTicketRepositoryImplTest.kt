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

import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingExposedH2TestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 用户水票实例仓储 Exposed 实现测试(H2):save/软删/派生查询/按日使用聚合/核销与退回/票号取号。
 */
class UserTicketRepositoryImplTest :
  FunSpec({
    val repo = UserTicketRepositoryImpl(MarketingExposedH2TestSupport.database)

    beforeTest { reset() }

    test("save 双分支 + findById 软删过滤") {
      val saved =
        repo.save(
          UserTicketModel(userId = 100L, ticketTypeId = 1L, purchaseId = 10L, ticketNo = "TKT-001"),
        )
      saved.id.shouldNotBeNull()
      repo
        .findById(saved.id!!)
        .orElse(null)
        .shouldNotBeNull()
        .ticketNo shouldBe "TKT-001"

      val loaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      loaded.status = UserTicketStatus.USED
      loaded.usedOrderId = 777L
      loaded.usedAt = LocalDateTime.now()
      repo.save(loaded)
      val reloaded = repo.findById(saved.id!!).orElse(null).shouldNotBeNull()
      reloaded.status shouldBe UserTicketStatus.USED
      reloaded.usedOrderId shouldBe 777L

      val gone = MarketingExposedH2TestSupport.userTicket(userId = 1L, ticketTypeId = 1L, purchaseId = 1L, deleted = true)
      repo.findById(gone).isPresent shouldBe false
    }

    test("派生查询:按用户/状态过滤 + 软删不可见 + id 升序") {
      val u1 = MarketingExposedH2TestSupport.userTicket(userId = 100L, ticketTypeId = 1L, purchaseId = 1L, status = "UNUSED")
      val u2 = MarketingExposedH2TestSupport.userTicket(userId = 100L, ticketTypeId = 1L, purchaseId = 1L, status = "RETURNED")
      MarketingExposedH2TestSupport.userTicket(userId = 100L, ticketTypeId = 1L, purchaseId = 1L, status = "UNUSED", deleted = true)
      MarketingExposedH2TestSupport.userTicket(userId = 101L, ticketTypeId = 1L, purchaseId = 2L, status = "UNUSED")

      repo.findByUserId(100L).map { it.id } shouldContainExactly listOf(u1, u2)
      repo.findByUserIdAndStatus(100L, UserTicketStatus.UNUSED).map { it.id } shouldContainExactly listOf(u1)
      repo
        .findByUserIdAndStatusIn(100L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED, UserTicketStatus.USED))
        .map { it.id } shouldContainExactly listOf(u1, u2)
      repo.findByUserIdAndStatusIn(100L, emptyList()).size shouldBe 0
      repo.findByUserId(999L).size shouldBe 0
    }

    test("countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan:状态+未过期口径,空状态列表=0") {
      val now = LocalDateTime.now()
      MarketingExposedH2TestSupport.userTicket(
        userId = 1L,
        ticketTypeId = 7L,
        purchaseId = 1L,
        status = "UNUSED",
        expireTime = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.userTicket(
        userId = 2L,
        ticketTypeId = 7L,
        purchaseId = 1L,
        status = "RETURNED",
        expireTime = now.plusDays(10),
      )
      MarketingExposedH2TestSupport.userTicket(
        userId = 3L,
        ticketTypeId = 7L,
        purchaseId = 1L,
        status = "RETURNED",
        expireTime = now.minusDays(1),
      )
      MarketingExposedH2TestSupport.userTicket(
        userId = 4L,
        ticketTypeId = 7L,
        purchaseId = 1L,
        status = "UNUSED",
        expireTime = now.plusDays(10),
        deleted = true,
      )

      repo.countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
        7L,
        listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED),
        now,
      ) shouldBe
        2L
      repo.countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(7L, listOf(UserTicketStatus.UNUSED), now) shouldBe 1L
      repo.countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(8L, listOf(UserTicketStatus.UNUSED), now) shouldBe 0L
      repo.countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(7L, emptyList(), now) shouldBe 0L
    }

    test("findUsageGroupedByDay:按状态对应时间戳聚合,[from,to)") {
      val tid = 5L
      val d1 = LocalDate.of(2026, 3, 1)
      val d2 = LocalDate.of(2026, 3, 2)
      // USED → usedAt
      MarketingExposedH2TestSupport.userTicket(
        userId = 1L,
        ticketTypeId = tid,
        purchaseId = 1L,
        status = "USED",
        usedOrderId = 1L,
        usedAt = d1.atTime(10, 0),
        returnedAt = null,
        expireTime = d1.plusDays(30).atStartOfDay(),
      )
      // RETURNED → returnedAt 落 d2
      MarketingExposedH2TestSupport.userTicket(
        userId = 1L,
        ticketTypeId = tid,
        purchaseId = 1L,
        status = "RETURNED",
        usedOrderId = null,
        usedAt = null,
        returnedAt = d2.atTime(9, 0),
        expireTime = d1.plusDays(30).atStartOfDay(),
      )
      // EXPIRED → returnedAt 为空回退 updatedAt(种子 updatedAt=createdAt 落 d1)
      MarketingExposedH2TestSupport.userTicket(
        userId = 2L,
        ticketTypeId = tid,
        purchaseId = 1L,
        status = "EXPIRED",
        returnedAt = null,
        usedAt = null,
        createdAt = d1.atTime(23, 0),
        expireTime = d1.plusDays(30).atStartOfDay(),
      )
      // UNUSED 不计;他票种不计;软删不计
      MarketingExposedH2TestSupport.userTicket(
        userId = 3L,
        ticketTypeId = tid,
        purchaseId = 1L,
        status = "UNUSED",
        expireTime = d1.plusDays(30).atStartOfDay(),
      )
      MarketingExposedH2TestSupport.userTicket(
        userId = 4L,
        ticketTypeId = 6L,
        purchaseId = 1L,
        status = "USED",
        usedOrderId = 2L,
        usedAt = d1.atTime(11, 0),
        expireTime = d1.plusDays(30).atStartOfDay(),
      )
      MarketingExposedH2TestSupport.userTicket(
        userId = 5L,
        ticketTypeId = tid,
        purchaseId = 1L,
        status = "USED",
        usedOrderId = 3L,
        usedAt = d1.atTime(11, 0),
        expireTime = d1.plusDays(30).atStartOfDay(),
        deleted = true,
      )

      val from = LocalDateTime.of(2026, 3, 1, 0, 0)
      val to = LocalDateTime.of(2026, 3, 3, 0, 0)
      val rows = repo.findUsageGroupedByDay(tid, from, to)
      rows.size shouldBe 2
      val r1 = rows[0]
      r1.getDay() shouldBe d1
      r1.getRedeemedCount() shouldBe 1L
      r1.getReturnedCount() shouldBe 0L
      r1.getExpiredCount() shouldBe 1L
      val r2 = rows[1]
      r2.getDay() shouldBe d2
      r2.getRedeemedCount() shouldBe 0L
      r2.getReturnedCount() shouldBe 1L
      r2.getExpiredCount() shouldBe 0L

      repo.findUsageGroupedByDay(tid, LocalDateTime.of(2027, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 2, 0, 0)).size shouldBe 0
    }

    test("redeemAtomically:UNUSED/RETURNED 可核销,USED/过期/软删 no-op") {
      val now = LocalDateTime.now()
      val unused =
        MarketingExposedH2TestSupport.userTicket(
          userId = 100L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "UNUSED",
          expireTime = now.plusDays(5),
        )
      val returned =
        MarketingExposedH2TestSupport.userTicket(
          userId = 100L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "RETURNED",
          expireTime = now.plusDays(5),
        )
      val used =
        MarketingExposedH2TestSupport.userTicket(
          userId = 100L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "USED",
          usedOrderId = 1L,
          usedAt = now.minusDays(1),
          expireTime = now.plusDays(5),
        )
      val expired =
        MarketingExposedH2TestSupport.userTicket(
          userId = 100L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "UNUSED",
          expireTime = now.minusDays(1),
        )
      val gone =
        MarketingExposedH2TestSupport.userTicket(
          userId = 100L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "UNUSED",
          expireTime = now.plusDays(5),
          deleted = true,
        )

      repo.redeemAtomically(unused, 100L, 888L, now) shouldBe 1
      repo.findById(unused).orElse(null).shouldNotBeNull().also {
        it.status shouldBe UserTicketStatus.USED
        it.usedOrderId shouldBe 888L
        it.usedAt shouldBe now
      }
      repo.redeemAtomically(returned, 100L, 889L, now) shouldBe 1
      repo.redeemAtomically(used, 100L, 890L, now) shouldBe 0
      repo.redeemAtomically(expired, 100L, 891L, now) shouldBe 0
      repo.redeemAtomically(gone, 100L, 892L, now) shouldBe 0
      // 用户不匹配
      val otherUser =
        MarketingExposedH2TestSupport.userTicket(
          userId = 101L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          expireTime = now.plusDays(5),
        )
      repo.redeemAtomically(otherUser, 100L, 893L, now) shouldBe 0
    }

    test("returnByOrder:未过期→RETURNED,已过期→EXPIRED,幂等") {
      val now = LocalDateTime.now()
      val unexpired =
        MarketingExposedH2TestSupport.userTicket(
          userId = 1L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "USED",
          usedOrderId = 42L,
          usedAt = now.minusDays(2),
          expireTime = now.plusDays(3),
        )
      val expired =
        MarketingExposedH2TestSupport.userTicket(
          userId = 2L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "USED",
          usedOrderId = 42L,
          usedAt = now.minusDays(9),
          expireTime = now.minusDays(1),
        )
      val other =
        MarketingExposedH2TestSupport.userTicket(
          userId = 3L,
          ticketTypeId = 1L,
          purchaseId = 1L,
          status = "USED",
          usedOrderId = 43L,
          usedAt = now.minusDays(2),
          expireTime = now.plusDays(3),
        )

      repo.returnByOrder(42L, now) shouldBe 2
      val r = repo.findById(unexpired).orElse(null).shouldNotBeNull()
      r.status shouldBe UserTicketStatus.RETURNED
      r.usedOrderId shouldBe null
      r.usedAt shouldBe null
      r.returnedAt shouldBe now
      val e = repo.findById(expired).orElse(null).shouldNotBeNull()
      e.status shouldBe UserTicketStatus.EXPIRED
      e.usedOrderId shouldBe null
      repo
        .findById(other)
        .orElse(null)
        .shouldNotBeNull()
        .status shouldBe UserTicketStatus.USED

      repo.returnByOrder(42L, now) shouldBe 0
    }

    test("nextTicketSeq:首票置 1,逐次 +1,跨月重置,冲突路径幂等递增") {
      // 首次:插入分支 → 1
      repo.nextTicketSeq("202603") shouldBe 1L
      // 冲突路径:upsert 更新分支 → +1(单线程验证 on-conflict 语义)
      repo.nextTicketSeq("202603") shouldBe 2L
      repo.nextTicketSeq("202603") shouldBe 3L
      // 跨月独立计数(重置语义:新月份从 1 开始)
      repo.nextTicketSeq("202604") shouldBe 1L
      // 交错月份各自单调
      repo.nextTicketSeq("202603") shouldBe 4L
      repo.nextTicketSeq("202604") shouldBe 2L
    }
  })
