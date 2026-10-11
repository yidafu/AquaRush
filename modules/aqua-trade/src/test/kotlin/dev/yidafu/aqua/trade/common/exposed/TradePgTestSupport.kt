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

import dev.yidafu.aqua.common.domain.table.DailyCollectionRecordsTable
import dev.yidafu.aqua.common.domain.table.DailyReconciliationsTable
import dev.yidafu.aqua.common.domain.table.DeliveryAreasTable
import dev.yidafu.aqua.common.domain.table.DeliveryTrajectoriesTable
import dev.yidafu.aqua.common.domain.table.DeliveryWorkerStatisticsTable
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrderOperationsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.PaymentRefundsTable
import dev.yidafu.aqua.common.domain.table.PaymentsTable
import dev.yidafu.aqua.common.domain.table.ReviewsTable
import dev.yidafu.aqua.common.domain.table.SubscriptionItemsTable
import dev.yidafu.aqua.common.domain.table.SubscriptionsTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * trade 仓储实现测试的 **Testcontainers 真实 PostgreSQL** 支撑(JPA→Exposed 迁移 P4 覆盖率门禁配套)。
 *
 * 为什么不用 H2:
 * - `INSERT ... ON CONFLICT`(票号/幂等 upsert)在 H2 是 `MERGE`,行锁与冲突递增语义不同;
 * - `jsonString` 列绑定、varchar 长度校验、类型转换等 PG 方言差异 H2 测不出;
 * - 复审已指出「H2 单线程验证」不背书生产 PG 语义 —— 这里直接对真实 PG 验证。
 *
 * 容器启动/镜像/参数统一由 foundation testFixtures 的 [PostgresTestContainer] 提供
 * (含 podman 运行时接线与内存参数,见其类注释);本文件只声明本域的表集合与种子辅助。
 *
 * 生命周期:JVM 级单例容器 + 单例 [database],随测试 JVM 退出由 Testcontainers shutdown hook 停容器。
 *
 * 注意:
 * - 全表 deleteAll + 非线程安全 id 计数 → **依赖 Kotest 串行执行**;若开启 spec 并行需改造。
 * - 各域测试的种子构造函数(插入指定列值的 helper)放各自的测试文件,勿改本文件,
 *   避免并行迁移分支合并冲突。
 */
object TradePgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquatrade")

  private val allTables: Array<Table> =
    arrayOf(
      // order 子域(SourceTables)
      OrdersTable,
      OrderItemsTable,
      OrderGiftsTable,
      OrderOperationsTable,
      PaymentRefundsTable,
      // payment/review/delivery 子域(TradeTables)
      PaymentsTable,
      ReviewsTable,
      DeliveryWorkerStatisticsTable,
      DeliveryTrajectoriesTable,
      DailyCollectionRecordsTable,
      DailyReconciliationsTable,
      DeliveryAreasTable,
      // 周期订购计划(G77/F-ORD-026)
      SubscriptionsTable,
      SubscriptionItemsTable,
      // 计划列表关键字筛选联查用户子查询(search keyword → users)
      UsersTable,
    )

  @Volatile
  private var schemaCreated = false

  fun reset() {
    transaction(database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }

  private var nextId = 70000L

  fun id(): Long = nextId++
}
