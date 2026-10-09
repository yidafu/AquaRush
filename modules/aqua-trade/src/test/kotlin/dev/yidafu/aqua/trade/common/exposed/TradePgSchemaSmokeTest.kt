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

package dev.yidafu.aqua.trade.common.exposed

import dev.yidafu.aqua.common.domain.table.DeliveryAreasTable
import dev.yidafu.aqua.common.domain.table.DeliveryTrajectoriesTable
import dev.yidafu.aqua.common.domain.table.selectActive
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

/**
 * trade 域表定义在 **真实 PostgreSQL**(Testcontainers)上的冒烟测试
 * (JPA→Exposed 迁移 P4,配套 [TradePgTestSupport])。
 *
 * 验证三件事(H2 模拟测不出或语义不同的部分):
 * 1. [TradePgTestSupport.reset] 的 `SchemaUtils.create` 生成的 DDL 在 PG 上可执行(13 张表);
 * 2. 裸表读写往返;
 * 3. `SoftDeletableTable.isDeleted` 的 clientDefault 在 PG 上自动写入 false,
 *    以及 [selectActive] 软删可见性语义。
 */
class TradePgSchemaSmokeTest :
  FunSpec({
    test("13 张 trade 表在真实 PostgreSQL 上建表、读写、软删可见性") {
      TradePgTestSupport.reset()
      val db = TradePgTestSupport.database

      transaction(db) {
        // 1) 裸表(delivery_trajectories,无软删列)读写往返
        DeliveryTrajectoriesTable.selectAll().count() shouldBe 0
        val trajId = TradePgTestSupport.id()
        val now = LocalDateTime.now()
        DeliveryTrajectoriesTable.insert {
          it[DeliveryTrajectoriesTable.id] = trajId
          it[deliveryWorkerId] = 1L
          it[orderId] = 2L
          it[longitude] = 116.4
          it[latitude] = 39.9
          it[accuracy] = 5.0
          it[reportedAt] = now
          it[createdAt] = now
        }
        DeliveryTrajectoriesTable
          .selectAll()
          .where { DeliveryTrajectoriesTable.id eq trajId }
          .count() shouldBe 1

        // 2) 软删表(delivery_areas):insert 不写 is_deleted → clientDefault 自动 false
        val areaId = TradePgTestSupport.id()
        DeliveryAreasTable.insert {
          it[DeliveryAreasTable.id] = areaId
          it[name] = "朝阳区"
          it[province] = "北京市"
          it[city] = "北京市"
          it[district] = "朝阳区"
          it[isEnabled] = true
          it[createdAt] = now
          it[updatedAt] = now
        }
        DeliveryAreasTable.selectActive { DeliveryAreasTable.id eq areaId }.count() shouldBe 1

        // 3) 软删后 selectActive 不可见,裸 select 仍可见(对齐 @SoftDelete 语义)
        DeliveryAreasTable.update({ DeliveryAreasTable.id eq areaId }) {
          it[isDeleted] = true
          it[deletedAt] = now
        }
        DeliveryAreasTable.selectActive { DeliveryAreasTable.id eq areaId }.count() shouldBe 0
        DeliveryAreasTable
          .selectAll()
          .where { DeliveryAreasTable.id eq areaId }
          .count() shouldBe 1
      }
    }
  })
