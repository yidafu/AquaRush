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

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.table.BarrelPurchaseItemsTable
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport
import dev.yidafu.aqua.marketing.common.exposed.MarketingPgTestSupport.reset
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * 购桶单明细仓储 Exposed 实现测试(Testcontainers 真实 PG):save 双分支/软删可见性/派生查询/saveAll。
 */
class BarrelPurchaseItemRepositoryImplTest :
  FunSpec({
    val repo = BarrelPurchaseItemRepositoryImpl(MarketingPgTestSupport.database)

    beforeTest { reset() }

    test("save 新增并回填雪花 id,findAllByPurchaseId 可查") {
      val purchaseId = MarketingPgTestSupport.barrelPurchase(userId = 1L)
      val saved = repo.save(BarrelPurchaseItemModel(purchaseId = purchaseId, barrelFeeId = 1L, specification = "19L"))
      saved.id.shouldNotBeNull()
      repo.findAllByPurchaseIdOrderByIdAsc(purchaseId) shouldHaveSize 1
    }

    test("save 更新已有实体走 update 分支") {
      val purchaseId = MarketingPgTestSupport.barrelPurchase(userId = 1L)
      val id =
        MarketingPgTestSupport.barrelItem(purchaseId = purchaseId, barrelFeeId = 1L, quantity = 1)
      val row =
        transaction(MarketingPgTestSupport.database) {
          BarrelPurchaseItemsTable.selectAll().andWhere { BarrelPurchaseItemsTable.id eq id }.single()
        }
      val loaded =
        BarrelPurchaseItemModel(
          id = id,
          purchaseId = purchaseId,
          barrelFeeId = row[BarrelPurchaseItemsTable.barrelFeeId],
          specification = row[BarrelPurchaseItemsTable.specification],
          unitPriceCents = row[BarrelPurchaseItemsTable.unitPriceCents],
          quantity = 3,
          amountCents = 9000L,
          createdAt = row[BarrelPurchaseItemsTable.createdAt],
          updatedAt = row[BarrelPurchaseItemsTable.updatedAt],
          deletedAt = row[BarrelPurchaseItemsTable.deletedAt],
          deletedBy = row[BarrelPurchaseItemsTable.deletedBy],
        )
      repo.save(loaded)
      val reloaded = repo.findAllByPurchaseIdOrderByIdAsc(purchaseId).single()
      reloaded.quantity shouldBe 3
      reloaded.amountCents shouldBe 9000L
    }

    test("软删明细不可见 + 按 id 升序") {
      val purchaseId = MarketingPgTestSupport.barrelPurchase(userId = 1L)
      val first = MarketingPgTestSupport.barrelItem(purchaseId = purchaseId, barrelFeeId = 1L)
      val second = MarketingPgTestSupport.barrelItem(purchaseId = purchaseId, barrelFeeId = 2L)
      MarketingPgTestSupport.barrelItem(purchaseId = purchaseId, barrelFeeId = 3L, deleted = true)

      repo
        .findAllByPurchaseIdOrderByIdAsc(purchaseId)
        .map { it.id } shouldContainExactly listOf(first, second)
      repo.findAllByPurchaseIdOrderByIdAsc(999L) shouldHaveSize 0
    }

    test("saveAll 批量 upsert,再保存走 update 分支") {
      val purchaseId = MarketingPgTestSupport.barrelPurchase(userId = 1L)
      val items =
        listOf(
          BarrelPurchaseItemModel(purchaseId = purchaseId, barrelFeeId = 1L, quantity = 1, amountCents = 3000L),
          BarrelPurchaseItemModel(purchaseId = purchaseId, barrelFeeId = 2L, quantity = 2, amountCents = 6000L),
        )
      val saved = repo.saveAll(items)
      saved shouldHaveSize 2
      repo.findAllByPurchaseIdOrderByIdAsc(purchaseId) shouldHaveSize 2

      // 同批再保存(带 id)→ update 分支,行数不变
      saved.forEach { it.quantity = 9 }
      repo.saveAll(saved)
      val reloaded = repo.findAllByPurchaseIdOrderByIdAsc(purchaseId)
      reloaded shouldHaveSize 2
      reloaded.map { it.quantity } shouldContainExactly listOf(9, 9)
    }
  })
