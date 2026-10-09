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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.common.domain.model.OrderGiftModel
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * 订单赠品行仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * saveAll 单事务批量 upsert / findByOrderId(裸表无软删过滤,与原 JPA 一致)。
 */
class OrderGiftRepositoryImplTest :
  FunSpec({
    val repo = OrderGiftRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    fun seedGiftRow(
      orderId: Long,
      giftActivityId: Long,
      productId: Long,
      quantity: Int = 1,
    ): Long {
      val id = TradePgTestSupport.id()
      transaction(TradePgTestSupport.database) {
        OrderGiftsTable.insert {
          it[OrderGiftsTable.id] = id
          it[OrderGiftsTable.orderId] = orderId
          it[OrderGiftsTable.giftActivityId] = giftActivityId
          it[OrderGiftsTable.giftActivityName] = "seed 活动"
          it[OrderGiftsTable.productId] = productId
          it[OrderGiftsTable.productName] = "seed 赠品"
          it[OrderGiftsTable.quantity] = quantity
          it[OrderGiftsTable.createdAt] = LocalDateTime.of(2026, 5, 1, 0, 0)
        }
      }
      return id
    }

    test("saveAll 单事务批量插入回填 id,findByOrderId 按 id 升序") {
      val orderId = TradePgTestSupport.id()
      val saved =
        repo.saveAll(
          listOf(
            OrderGiftModel(
              orderId = orderId,
              giftActivityId = 51L,
              giftActivityName = "买赠活动",
              productId = 61L,
              productName = "赠品水桶",
              quantity = 2,
            ),
            OrderGiftModel(
              orderId = orderId,
              giftActivityId = 51L,
              giftActivityName = "买赠活动",
              productId = 62L,
              productName = "赠品杯",
              quantity = 1,
            ),
          ),
        )
      saved shouldHaveSize 2
      saved.forEach { it.id.shouldNotBeNull() }

      val byOrder = repo.findByOrderId(orderId)
      byOrder.map { it.id } shouldContainExactly saved.map { it.id }
      byOrder.first().giftActivityName shouldBe "买赠活动"
      byOrder.first().quantity shouldBe 2
    }

    test("saveAll 更新分支:已有 id 覆盖业务列,createdAt 不可变") {
      val orderId = TradePgTestSupport.id()
      seedGiftRow(orderId = orderId, giftActivityId = 1L, productId = 71L, quantity = 1)
      val entity = repo.findByOrderId(orderId).single()

      repo.saveAll(
        listOf(
          entity.apply {
            productId = 72L
            quantity = 5
            productName = "换名赠品"
          },
        ),
      )

      val reloaded = repo.findByOrderId(orderId).single()
      reloaded.productId shouldBe 72L
      reloaded.quantity shouldBe 5
      reloaded.productName shouldBe "换名赠品"
      reloaded.createdAt shouldBe LocalDateTime.of(2026, 5, 1, 0, 0)
    }

    test("findByOrderId 多订单隔离,赠品行不过滤软删(裸表无 is_deleted 列)") {
      val orderA = TradePgTestSupport.id()
      val orderB = TradePgTestSupport.id()
      val a1 = seedGiftRow(orderId = orderA, giftActivityId = 9L, productId = 81L)
      val a2 = seedGiftRow(orderId = orderA, giftActivityId = 9L, productId = 82L)
      seedGiftRow(orderId = orderB, giftActivityId = 9L, productId = 81L)

      repo.findByOrderId(orderA).map { it.id } shouldContainExactly listOf(a1, a2)
      repo.findByOrderId(orderB) shouldHaveSize 1
      repo.findByOrderId(TradePgTestSupport.id()) shouldHaveSize 0
    }
  })
