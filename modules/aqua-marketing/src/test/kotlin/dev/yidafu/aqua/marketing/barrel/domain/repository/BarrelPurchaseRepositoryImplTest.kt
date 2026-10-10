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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.domain.table.BarrelPurchasesTable
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 水桶购买单仓储 Exposed 实现测试(Testcontainers 真实 PG):save/软删/派生查询/分页/原子流转。
 */
class BarrelPurchaseRepositoryImplTest :
  FunSpec({
    val repo = BarrelPurchaseRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增回填 id,findByOrderNo/existsBy 可查") {
      val saved = repo.save(BarrelPurchaseModel(userId = 100L, orderNo = "BK001", totalAmountCents = 3000L))
      saved.id.shouldNotBeNull()
      repo.findByOrderNo("BK001").shouldNotBeNull().userId shouldBe 100L
      repo.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PENDING) shouldBe true
      repo.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PAID) shouldBe false
    }

    test("save 更新已有实体走 update 分支") {
      val id = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK002")
      val loaded = repo.findById(id).orElse(null).shouldNotBeNull()
      loaded.paymentStatus = BarrelPaymentStatus.PAID
      loaded.transactionId = "wx123"
      loaded.paidAt = LocalDateTime.of(2026, 3, 1, 12, 0)
      repo.save(loaded)

      val reloaded = repo.findById(id).orElse(null).shouldNotBeNull()
      reloaded.paymentStatus shouldBe BarrelPaymentStatus.PAID
      reloaded.transactionId shouldBe "wx123"
      repo.findByOrderNo("BK002").shouldNotBeNull().paymentStatus shouldBe BarrelPaymentStatus.PAID
    }

    test("软删购桶单不可见:findById/findByOrderNo/exists/findFirst 均过滤") {
      val keep = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK-keep")
      MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK-gone", deleted = true)

      repo.findById(keep).isPresent shouldBe true
      repo.findByOrderNo("BK-gone") shouldBe null
      repo.existsByUserIdAndPaymentStatus(100L, BarrelPaymentStatus.PENDING) shouldBe true
      repo.findFirstByUserIdOrderByCreatedAtDesc(100L).shouldNotBeNull().orderNo shouldBe "BK-keep"
      repo.findFirstByUserIdOrderByCreatedAtDesc(999L) shouldBe null
    }

    test("findFirstByUserIdOrderByCreatedAtDesc:最新优先,id 破平") {
      val older = LocalDateTime.of(2026, 1, 1, 10, 0)
      val newer = LocalDateTime.of(2026, 6, 1, 10, 0)
      val first = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK-old", createdAt = older)
      val second = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK-new", createdAt = newer)
      val third = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK-old2", createdAt = older)

      repo.findFirstByUserIdOrderByCreatedAtDesc(100L).shouldNotBeNull().id shouldBe second
      // 同 createdAt 最新组按 id 降序破平(显式接收两个 seed id,组内取 id 大者)
      val tie1 = MarketingPgTestSupport.barrelPurchase(userId = 101L, orderNo = "BK-tie1", createdAt = older)
      val tie2 = MarketingPgTestSupport.barrelPurchase(userId = 101L, orderNo = "BK-tie2", createdAt = older)
      repo.findFirstByUserIdOrderByCreatedAtDesc(101L).shouldNotBeNull().id shouldBe tie2
      // sanity: older/third 存在
      repo.findById(first).isPresent shouldBe true
      repo.findById(third).isPresent shouldBe true
    }

    test("searchPurchases:状态+paidAt 区间过滤,软删不进结果") {
      val paidAt = LocalDateTime.of(2026, 5, 1, 12, 0)
      MarketingPgTestSupport.barrelPurchase(userId = 1L, paymentStatus = "PAID", paidAt = paidAt)
      MarketingPgTestSupport.barrelPurchase(userId = 2L, paymentStatus = "PENDING", paidAt = null)
      MarketingPgTestSupport.barrelPurchase(userId = 3L, paymentStatus = "PAID", paidAt = paidAt, deleted = true)

      repo
        .searchPurchases(BarrelPaymentStatus.PAID, null, null, PageRequest.of(0, 10))
        .totalElements shouldBe 1L
      repo
        .searchPurchases(
          null,
          LocalDateTime.of(2026, 5, 1, 0, 0),
          LocalDateTime.of(2026, 5, 31, 23, 59),
          PageRequest.of(0, 10),
        ).totalElements shouldBe 1L
      repo
        .searchPurchases(null, LocalDateTime.of(2026, 6, 1, 0, 0), null, PageRequest.of(0, 10))
        .totalElements shouldBe 0L
    }

    test("searchPurchases:createdAt 降序 + 同值 id 破平,跨页不重不漏") {
      val tie = LocalDateTime.of(2026, 5, 2, 8, 0)
      val a = MarketingPgTestSupport.barrelPurchase(userId = 1L, orderNo = "A", createdAt = tie)
      val b = MarketingPgTestSupport.barrelPurchase(userId = 1L, orderNo = "B", createdAt = tie)
      val c = MarketingPgTestSupport.barrelPurchase(userId = 1L, orderNo = "C", createdAt = tie)

      val page0 = repo.searchPurchases(null, null, null, PageRequest.of(0, 2))
      val page1 = repo.searchPurchases(null, null, null, PageRequest.of(1, 2))
      page0.totalElements shouldBe 3L
      val desc = listOf(c, b, a)
      page0.content.map { it.id } shouldContainExactly desc.take(2)
      page1.content.map { it.id } shouldContainExactly desc.drop(2)
    }

    test("markPaidAtomically:PENDING→PAID 幂等,二次调用 no-op") {
      val id = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "BK-pay", paymentStatus = "PENDING")
      val now = LocalDateTime.of(2026, 3, 1, 12, 0)

      repo.markPaidAtomically(id, "wx-1", now) shouldBe 1
      val paid = repo.findById(id).orElse(null).shouldNotBeNull()
      paid.paymentStatus shouldBe BarrelPaymentStatus.PAID
      paid.transactionId shouldBe "wx-1"
      paid.paidAt shouldBe now

      repo.markPaidAtomically(id, "wx-2", now.plusMinutes(1)) shouldBe 0
      repo
        .findById(id)
        .orElse(null)
        .shouldNotBeNull()
        .transactionId shouldBe "wx-1"
    }

    test("markPaidAtomically 对软删行 no-op") {
      val id = MarketingPgTestSupport.barrelPurchase(userId = 1L, paymentStatus = "PENDING", deleted = true)
      repo.markPaidAtomically(id, "wx", LocalDateTime.now()) shouldBe 0
    }

    test("cancelPendingByUser:批量取消该用户 PENDING 单,非 PENDING 不动") {
      val p1 = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "c1", paymentStatus = "PENDING")
      val p2 = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "c2", paymentStatus = "PENDING")
      val other = MarketingPgTestSupport.barrelPurchase(userId = 101L, orderNo = "c3", paymentStatus = "PENDING")
      val paid = MarketingPgTestSupport.barrelPurchase(userId = 100L, orderNo = "c4", paymentStatus = "PAID")
      val now = LocalDateTime.now()

      repo.cancelPendingByUser(100L, now) shouldBe 2
      repo
        .findById(p1)
        .orElse(null)
        .shouldNotBeNull()
        .paymentStatus shouldBe BarrelPaymentStatus.CANCELLED
      repo
        .findById(p2)
        .orElse(null)
        .shouldNotBeNull()
        .paymentStatus shouldBe BarrelPaymentStatus.CANCELLED
      repo
        .findById(other)
        .orElse(null)
        .shouldNotBeNull()
        .paymentStatus shouldBe BarrelPaymentStatus.PENDING
      repo
        .findById(paid)
        .orElse(null)
        .shouldNotBeNull()
        .paymentStatus shouldBe BarrelPaymentStatus.PAID

      repo.cancelPendingByUser(100L, now) shouldBe 0
    }

    test("delete 语义由行内三列验证:软删行 updated_at/deleted_at 写入") {
      val id = MarketingPgTestSupport.barrelPurchase(userId = 1L)
      transaction(MarketingPgTestSupport.database) {
        BarrelPurchasesTable.update({ BarrelPurchasesTable.id eq id }) {
          it[isDeleted] = true
          it[deletedAt] = LocalDateTime.of(2020, 1, 1, 0, 0)
          it[updatedAt] = LocalDateTime.of(2020, 1, 1, 0, 0)
        }
      }
      repo.findById(id).isPresent shouldBe false
    }
  })
