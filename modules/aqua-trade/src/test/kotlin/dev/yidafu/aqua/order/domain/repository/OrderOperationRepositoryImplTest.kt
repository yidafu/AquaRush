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

import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.table.OrderOperationsTable
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
 * 订单操作流水仓储 Exposed 实现测试(Testcontainers 真实 PG):
 * save upsert / 时间线倒序 / countByOrderId(裸表无软删过滤,与原 JPA 一致)。
 */
class OrderOperationRepositoryImplTest :
  FunSpec({
    val repo = OrderOperationRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    fun seedOperationRow(
      orderId: Long,
      createdAt: LocalDateTime,
      type: OrderOperationType = OrderOperationType.ORDER_CREATED,
    ): Long {
      val id = TradePgTestSupport.id()
      transaction(TradePgTestSupport.database) {
        OrderOperationsTable.insert {
          it[OrderOperationsTable.id] = id
          it[OrderOperationsTable.orderId] = orderId
          it[OrderOperationsTable.operationType] = type.name
          it[OrderOperationsTable.operatorType] = OperatorType.SYSTEM.name
          it[OrderOperationsTable.createdAt] = createdAt
        }
      }
      return id
    }

    test("save 新增回填雪花 id,字段经查询往返") {
      val orderId = TradePgTestSupport.id()
      val saved =
        repo.save(
          OrderOperationModel(
            orderId = orderId,
            operationType = OrderOperationType.ORDER_PAID,
            operatorId = 42L,
            operatorType = OperatorType.USER,
            description = "支付成功",
            extraData = """{"transactionId":"TX-1"}""",
            createdAt = LocalDateTime.of(2026, 5, 1, 8, 0),
          ),
        )
      saved.id.shouldNotBeNull()

      val loaded = repo.findByOrderIdOrderByCreatedAtDesc(orderId).single()
      loaded.id shouldBe saved.id
      loaded.operationType shouldBe OrderOperationType.ORDER_PAID
      loaded.operatorId shouldBe 42L
      loaded.operatorType shouldBe OperatorType.USER
      loaded.description shouldBe "支付成功"
      loaded.extraData shouldBe """{"transactionId":"TX-1"}"""
      loaded.createdAt shouldBe LocalDateTime.of(2026, 5, 1, 8, 0)
      repo.countByOrderId(orderId) shouldBe 1L
    }

    test("save 更新分支:已有 id 覆盖业务列,createdAt 不可变") {
      val orderId = TradePgTestSupport.id()
      seedOperationRow(orderId = orderId, createdAt = LocalDateTime.of(2026, 5, 2, 0, 0))
      val entity = repo.findByOrderIdOrderByCreatedAtDesc(orderId).single()

      repo.save(
        OrderOperationModel(
          id = entity.id,
          orderId = orderId,
          operationType = OrderOperationType.ORDER_CANCELLED,
          operatorId = null,
          operatorType = OperatorType.ADMIN,
          description = "管理员取消",
          extraData = null,
          createdAt = LocalDateTime.of(2020, 1, 1, 0, 0), // updatable=false,不落库
        ),
      )

      val reloaded = repo.findByOrderIdOrderByCreatedAtDesc(orderId).single()
      reloaded.operationType shouldBe OrderOperationType.ORDER_CANCELLED
      reloaded.operatorType shouldBe OperatorType.ADMIN
      reloaded.description shouldBe "管理员取消"
      reloaded.createdAt shouldBe LocalDateTime.of(2026, 5, 2, 0, 0)
    }

    test("findByOrderIdOrderByCreatedAtDesc:创建时间倒序 + id 破平 + count/多订单隔离") {
      val orderA = TradePgTestSupport.id()
      val t1 = LocalDateTime.of(2026, 5, 3, 10, 0)
      val t2 = LocalDateTime.of(2026, 5, 3, 11, 0)
      val t3 = LocalDateTime.of(2026, 5, 3, 12, 0)
      val first = seedOperationRow(orderId = orderA, createdAt = t1)
      val third = seedOperationRow(orderId = orderA, createdAt = t3)
      val second = seedOperationRow(orderId = orderA, createdAt = t2)
      seedOperationRow(orderId = TradePgTestSupport.id(), createdAt = t1)

      val timeline = repo.findByOrderIdOrderByCreatedAtDesc(orderA)
      timeline.map { it.id } shouldContainExactly listOf(third, second, first)
      timeline.map { it.createdAt } shouldContainExactly listOf(t3, t2, t1)
      repo.countByOrderId(orderA) shouldBe 3L
      repo.countByOrderId(TradePgTestSupport.id()) shouldBe 0L
      repo.findByOrderIdOrderByCreatedAtDesc(orderA) shouldHaveSize 3
    }
  })
