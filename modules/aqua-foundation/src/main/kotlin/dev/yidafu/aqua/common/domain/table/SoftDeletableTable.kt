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

import org.jetbrains.exposed.v1.javatime.datetime

/**
 * 带软删列 + `deleted_at`/`deleted_by` 留痕列的 Exposed 表基类。
 *
 * 查询过滤、软删、以及各入口的语义见 [SoftDeleteFlagTable]（本类只多两个留痕列）。
 * 仅 `is_deleted`、DDL 没有留痕列的表请直接继承 [SoftDeleteFlagTable]。
 *
 * 列定义来源：`db/changelog/changes/v1.0.6-add-ops-config-tables.xml` /
 * `v1.0.7-add-ops-notice-store-delivery-fee.xml`（列名、可空性、默认值逐字对齐）。
 */
abstract class SoftDeletableTable(
  name: String,
) : SoftDeleteFlagTable(name) {
  /** 软删时间（业务侧留痕，与 [isDeleted] 同时写入）。 */
  val deletedAt = datetime("deleted_at").nullable()

  /** 软删操作人（业务侧留痕）。 */
  val deletedBy = long("deleted_by").nullable()
}
