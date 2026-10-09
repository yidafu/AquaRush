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

import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

/**
 * 订单商品明细仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * saveAll 单事务批量 upsert / 派生查询软删可见性 / 明细回环。
 */
class OrderItemRepositoryImplTest :
  FunSpec({
    val repo = OrderItemRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    fun seedItemRow(
      orderId: Long,
      productId: Long,
      deleted: Boolean = false,
    ): Long {
      val id = TradePgTestSupport.id()
      transaction(TradePgTestSupport.database) {
        OrderItemsTable.insert {
          it[OrderItemsTable.id] = id
          it[OrderItemsTable.orderId] = orderId
          it[OrderItemsTable.productId] = productId
          it[OrderItemsTable.quantity] = 1
          it[OrderItemsTable.unitPriceCents] = 100L
          it[OrderItemsTable.totalPriceCents] = 100L
          it[OrderItemsTable.productSnapshot] = """{"id":$productId,"name":"seed"}"""
          it[OrderItemsTable.createdAt] = LocalDateTime.of(2026, 5, 1, 0, 0)
          it[OrderItemsTable.updatedAt] = LocalDateTime.of(2026, 5, 1, 0, 0)
        }
        if (deleted) {
          OrderItemsTable.update({ OrderItemsTable.id eq id }) { it[isDeleted] = true }
        }
      }
      return id
    }

    test("saveAll 单事务批量插入回填 id,findByOrderId/findByProductId 可查") {
      val orderId = TradePgTestSupport.id()
      val saved =
        repo.saveAll(
          listOf(
            OrderItemModel.create(
              orderId = orderId,
              productId = 11L,
              quantity = 2,
              unitPriceCents = 350L,
              productSnapshot = """{"id":11,"name":"农夫山泉"}""",
            ),
            OrderItemModel.create(
              orderId = orderId,
              productId = 12L,
              quantity = 1,
              unitPriceCents = 500L,
              productSnapshot = """{"id":12,"name":"怡宝"}""",
            ),
          ),
        )
      saved shouldHaveSize 2
      saved.forEach { it.id.shouldNotBeNull() }
      saved.map { it.totalPriceCents } shouldContainExactly listOf(700L, 500L)

      val byOrder = repo.findByOrderId(orderId)
      byOrder.map { it.id } shouldContainExactly saved.map { it.id }
      byOrder.first().productSnapshot shouldBe """{"id":11,"name":"农夫山泉"}"""
      repo.findByProductId(12L).map { it.orderId } shouldContainExactly listOf(orderId)
    }

    test("saveAll 更新分支:已有 id 覆盖业务列,createdAt 不可变") {
      val id = seedItemRow(orderId = 1L, productId = 21L)
      val entity = repo.findByOrderId(1L).single()
      entity.id shouldBe id

      val updated =
        OrderItemModel(
          id = id,
          orderId = 1L,
          productId = 22L,
          quantity = 9,
          unitPriceCents = 10L,
          totalPriceCents = 90L,
          productSnapshot = """{"id":22,"name":"updated"}""",
          createdAt = LocalDateTime.of(2020, 1, 1, 0, 0), // updatable=false,不落库
        )
      repo.saveAll(listOf(updated))

      val reloaded = repo.findByOrderId(1L).single()
      reloaded.productId shouldBe 22L
      reloaded.quantity shouldBe 9
      reloaded.totalPriceCents shouldBe 90L
      reloaded.productSnapshot shouldBe """{"id":22,"name":"updated"}"""
      reloaded.createdAt shouldBe LocalDateTime.of(2026, 5, 1, 0, 0)
    }

    test("findByOrderId/findByProductId:id 升序 + 软删不可见") {
      val orderA = TradePgTestSupport.id()
      val a = seedItemRow(orderId = orderA, productId = 31L)
      seedItemRow(orderId = orderA, productId = 32L, deleted = true)
      val b = seedItemRow(orderId = orderA, productId = 33L)
      seedItemRow(orderId = TradePgTestSupport.id(), productId = 34L)

      repo.findByOrderId(orderA).map { it.id } shouldContainExactly listOf(a, b)
      repo.findByProductId(31L).map { it.id } shouldContainExactly listOf(a)
      repo.findByOrderId(TradePgTestSupport.id()) shouldHaveSize 0
      repo.findByProductId(99L) shouldHaveSize 0
    }
  })
