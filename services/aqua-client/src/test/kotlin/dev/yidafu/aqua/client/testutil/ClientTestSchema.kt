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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.testutil

import dev.yidafu.aqua.common.domain.table.UserActionLogsTable
import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * client 服务集成测试的建表入口。
 *
 * client 生产环境**不跑 Liquibase**（application.yml 里 enabled=false,schema 由 admin
 * 服务的 changelog 维护）；而 JPA/Hibernate 已随 Exposed 迁移移除,于是「测试库表结构
 * 从哪来」必须显式回答 —— 这里按 foundation 的 Exposed 表定义（单一事实来源）用
 * [SchemaUtils] 建表,等价于迁移前 Hibernate ddl-auto=create 提供的测试建表能力,
 * 但不再依赖实体元数据。
 *
 * 新增 client 集成测试若触达新表,在 [tables] 补一行即可。
 */
object ClientTestSchema {
  /** client 集成测试触达的表。 */
  private val tables: Array<Table> = arrayOf(UserActionLogsTable)

  @Volatile
  private var created = false

  /** 幂等建表；[database] 默认复用 client 测试容器连接。 */
  fun createIfNeeded(database: Database = PostgresTestContainer.connect(DB_NAME)) {
    if (created) return
    synchronized(this) {
      if (created) return
      transaction(database) { SchemaUtils.create(*tables) }
      created = true
    }
  }

  const val DB_NAME = "aquaclienttest"
}
