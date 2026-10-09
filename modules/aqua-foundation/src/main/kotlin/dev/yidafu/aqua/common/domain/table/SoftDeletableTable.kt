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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * 带软删列的 Exposed 表基类。
 *
 * 语义**必须**与 JPA 侧的 `@SoftDelete(columnName = "is_deleted")` 对齐：Hibernate 会把所有
 * 查询/删除自动改写成 `is_deleted = false` 过滤 + `UPDATE ... SET is_deleted = true`。
 * Exposed 没有这类隐式改写，因此：
 * - 仓储的**每个**查询都要显式带上 [notDeleted]；
 * - 仓储的「删除」要显式写成 `update { it[isDeleted] = true }`。
 * 漏掉任意一处都会让已删数据重新出现在业务查询里（迁移方案 §9 R3）。
 *
 * 列定义来源：`db/changelog/changes/v1.0.6-add-ops-config-tables.xml` /
 * `v1.0.7-add-ops-notice-store-delivery-fee.xml`（列名、可空性、默认值逐字对齐）。
 */
abstract class SoftDeletableTable(
  name: String,
) : Table(name) {
  /**
   * 软删标记列，DDL 为 `boolean NOT NULL DEFAULT false`。
   * 用 clientDefault 而非 default：default 仅生成 DDL,运行时 INSERT 不附加该列;
   * 而部分环境的 DDL(如 Liquibase defaultValueBoolean)可能未落 DEFAULT,会导致 NULL 约束违反。
   */
  val isDeleted = bool("is_deleted").clientDefault { false }

  /** 软删时间（业务侧留痕，与 [isDeleted] 同时写入）。 */
  val deletedAt = datetime("deleted_at").nullable()

  /** 软删操作人（业务侧留痕）。 */
  val deletedBy = long("deleted_by").nullable()
}
