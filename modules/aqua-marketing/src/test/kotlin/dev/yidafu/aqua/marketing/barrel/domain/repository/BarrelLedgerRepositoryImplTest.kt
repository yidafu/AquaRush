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

import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * 桶账聚合读侧测试(Testcontainers 真实 PG,raw SQL):
 * 在租/已交付/已回收按用户×规格聚合 + 台账分页(仅看有欠桶/关键词/欠桶降序)。
 * 口径(R-PRO-04/05):已交付与已回收仅计押金价目规格;欠桶 = max(交付 − 回收, 0)。
 */
class BarrelLedgerRepositoryImplTest :
  FunSpec({
    val repo = BarrelLedgerRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    /**
     * 种子场景:
     * - 用户 A(id=2001):押金价目「19L」;完成订单交付 19L×2;HELD 押金 1 桶 4000 分;
     *   回收登记 19L×1 → 欠 1 桶,在租 1 桶,押金 4000 分
     * - 用户 B(id=2002):仅交付 5L×1(价目 5L 也启用),无回收无押金 → 欠 1 桶
     * - 非押金价目规格(无价目行)的交付/回收不计
     */
    fun seedScenario(): Pair<Long, Long> {
      val userA = MarketingPgTestSupport.depositUser(id = 2001L, nickname = "张三", phone = "13811112222")
      val userB = MarketingPgTestSupport.depositUser(id = 2002L, nickname = "李四", phone = "13933334444")
      MarketingPgTestSupport.barrelFee(specification = "19L", priceCents = 4000)
      MarketingPgTestSupport.barrelFee(specification = "5L", priceCents = 3000)

      val orderA = MarketingPgTestSupport.completedOrder(id = 3001L, userId = userA)
      MarketingPgTestSupport.orderItem(orderId = orderA, productId = 11L, quantity = 2, specification = "19L")
      // 非价目规格交付:不计欠桶
      MarketingPgTestSupport.orderItem(orderId = orderA, productId = 12L, quantity = 5, specification = "桶架")
      MarketingPgTestSupport.depositRecord(
        orderId = orderA,
        userId = userA,
        depositAmountCents = 4000,
        specification = "19L",
        unitDepositCents = 4000,
        bucketCount = 1,
        status = "HELD",
      )
      MarketingPgTestSupport.barrelRecoveryRecord(workerId = 7L, orderId = orderA, specification = "19L", bucketCount = 1)
      // 非价目规格回收:不计
      MarketingPgTestSupport.barrelRecoveryRecord(workerId = 7L, orderId = orderA, specification = "桶架", bucketCount = 2)

      val orderB = MarketingPgTestSupport.completedOrder(id = 3002L, userId = userB)
      MarketingPgTestSupport.orderItem(orderId = orderB, productId = 13L, quantity = 1, specification = "5L")
      return userA to userB
    }

    test("sum 三路聚合按用户×规格,非价目规格不计交付/回收") {
      val (userA, _) = seedScenario()

      val held = repo.sumHeldByUserAndSpec(listOf(userA))
      held shouldHaveSize 1
      held[0].specification shouldBe "19L"
      held[0].bucketCount shouldBe 1L
      held[0].amountCents shouldBe 4000L

      val delivered = repo.sumDeliveredByUserAndSpec(listOf(userA))
      delivered shouldHaveSize 1
      delivered[0].specification shouldBe "19L"
      delivered[0].bucketCount shouldBe 2L

      val recovered = repo.sumRecoveredByUserAndSpec(listOf(userA))
      recovered shouldHaveSize 1
      recovered[0].bucketCount shouldBe 1L
    }

    test("pageLedger 聚合口径:欠桶=交付−回收,仅价目规格") {
      val (userA, userB) = seedScenario()
      val rows = repo.pageLedger(keyword = null, onlyOwed = false, page = 0, size = 10)

      rows.totalElements shouldBe 2L
      val a = rows.content.first { it.userId == userA }
      a.heldBucketCount shouldBe 1L
      a.depositHeldCents shouldBe 4000L
      a.deliveredBucketCount shouldBe 2L
      a.recoveredBucketCount shouldBe 1L
      a.owedBucketCount shouldBe 1L
      a.nickname shouldBe "张三"

      val b = rows.content.first { it.userId == userB }
      b.heldBucketCount shouldBe 0L
      b.deliveredBucketCount shouldBe 1L
      b.owedBucketCount shouldBe 1L
    }

    test("pageLedger onlyOwed 只出行欠桶>0;无欠桶用户被过滤") {
      val (userA, userB) = seedScenario()
      // 用户 C:交付与回收持平 → 欠 0,onlyOwed 应过滤
      val userC = MarketingPgTestSupport.depositUser(id = 2003L, nickname = "王五")
      val orderC = MarketingPgTestSupport.completedOrder(id = 3003L, userId = userC)
      MarketingPgTestSupport.orderItem(orderId = orderC, productId = 14L, quantity = 1, specification = "19L")
      MarketingPgTestSupport.barrelRecoveryRecord(workerId = 7L, orderId = orderC, specification = "19L", bucketCount = 1)

      val all = repo.pageLedger(keyword = null, onlyOwed = false, page = 0, size = 10)
      all.totalElements shouldBe 3L

      val owed = repo.pageLedger(keyword = null, onlyOwed = true, page = 0, size = 10)
      owed.totalElements shouldBe 2L
      owed.content.map { it.userId } shouldBe listOf(userA, userB)
      owed.content.none { it.userId == userC } shouldBe true
    }

    test("pageLedger 关键词匹配昵称/手机号") {
      seedScenario()

      repo.pageLedger(keyword = "张三", onlyOwed = false, page = 0, size = 10).totalElements shouldBe 1L
      repo.pageLedger(keyword = "1393333", onlyOwed = false, page = 0, size = 10).totalElements shouldBe 1L
      repo.pageLedger(keyword = "不存在", onlyOwed = false, page = 0, size = 10).totalElements shouldBe 0L
    }

    test("pageLedger 欠桶降序分页不重不漏") {
      seedScenario()
      val page0 = repo.pageLedger(keyword = null, onlyOwed = false, page = 0, size = 1)
      val page1 = repo.pageLedger(keyword = null, onlyOwed = false, page = 1, size = 1)
      page0.totalElements shouldBe 2L
      page0.content shouldHaveSize 1
      page1.content shouldHaveSize 1
      (page0.content[0].userId != page1.content[0].userId) shouldBe true
      // 欠桶同为 1 时按用户 id 破平:第一页为较小 id
      page0.content[0].userId shouldBe 2001L
    }
  })
