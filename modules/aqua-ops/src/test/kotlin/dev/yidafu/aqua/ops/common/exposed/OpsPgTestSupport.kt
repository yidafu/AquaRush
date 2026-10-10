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

package dev.yidafu.aqua.ops.common.exposed

import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import dev.yidafu.aqua.ops.banner.domain.table.BannerTable
import dev.yidafu.aqua.ops.deliveryfeerule.domain.table.DeliveryFeeRuleTable
import dev.yidafu.aqua.ops.feedback.domain.table.FeedbackTable
import dev.yidafu.aqua.ops.notice.domain.table.NoticeTable
import dev.yidafu.aqua.ops.storeprofile.domain.table.StoreProfileTable
import dev.yidafu.aqua.ops.systemconfig.domain.table.SystemSettingsTable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * ops 仓储实现测试的 Testcontainers 真实 PG 支撑（JPA -> Exposed 迁移，P1 覆盖率门禁配套）。
 *
 * 表结构由 Exposed 的 `SchemaUtils.create` 生成（与 Liquibase DDL 逐列对齐），
 * 每个测试类 `beforeSpec` 调用 [reset] 清空全部表，保证用例互不串数据。
 */
object OpsPgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquaopsrepo")

  private val allTables: Array<Table> =
    arrayOf(
      BannerTable,
      NoticeTable,
      SystemSettingsTable,
      DeliveryFeeRuleTable,
      StoreProfileTable,
      FeedbackTable,
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
}
