/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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
package dev.yidafu.aqua.common.domain.table

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * foundation 23 张新表定义(Trade/Marketing/Product + Source 等)的真实 PG 运行时冒烟:
 * 仅编译+静态比对挡不住列名拼错、PG 保留字冲突、jsonString 类型映射问题,
 * 在这里以最低成本执行一遍 [SchemaUtils][org.jetbrains.exposed.v1.jdbc.SchemaUtils].create 与真实读写。
 */
class TableDefinitionsSmokeTest :
  FunSpec({
    beforeTest { FoundationPgTestSupport.reset() }

    test("全部 foundation 表可在 PG 建表且建表后空查询不抛") {
      transaction(FoundationPgTestSupport.database) {
        FoundationPgTestSupport.allTables.forEach { table ->
          table.selectAll().toList().shouldBeEmpty()
        }
      }
    }

    test("jsonString 回环:OrderOperationsTable.extra_data 插入什么读回什么") {
      val json = """{"reason":"manual","operatorId":900719925,"amount":12800}"""
      transaction(FoundationPgTestSupport.database) {
        OrderOperationsTable.selectAll().toList().shouldBeEmpty()

        OrderOperationsTable.insert {
          it[OrderOperationsTable.id] = 910000000001L
          it[orderId] = 910000000002L
          it[operationType] = "CANCEL"
          it[extraData] = json
          it[createdAt] = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        }

        val loaded = OrderOperationsTable.selectAll().single()[OrderOperationsTable.extraData]
        loaded shouldBe json
      }
    }

    test("软删可见性:activeOnly 只返回 is_deleted=false 的行") {
      val now = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
      transaction(FoundationPgTestSupport.database) {
        CustomerGroupsTable.selectAll().toList().shouldBeEmpty()

        CustomerGroupsTable.insert {
          it[CustomerGroupsTable.id] = 920000000001L
          it[name] = "active-group"
          it[description] = "未删除"
          it[createdAt] = now
          it[updatedAt] = now
          it[isDeleted] = false
        }
        CustomerGroupsTable.insert {
          it[CustomerGroupsTable.id] = 920000000002L
          it[name] = "deleted-group"
          it[description] = "已删除"
          it[createdAt] = now
          it[updatedAt] = now
          it[isDeleted] = true
        }

        CustomerGroupsTable.selectAll().toList().size shouldBe 2

        val activeRows =
          CustomerGroupsTable
            .selectAll()
            .where { CustomerGroupsTable.activeOnly() }
            .toList()
        activeRows.size shouldBe 1
        activeRows.single()[CustomerGroupsTable.name] shouldBe "active-group"
      }
    }
  })
