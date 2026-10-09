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

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

// [SoftDeletableTable] 的软删操作扩展(JPA→Exposed 迁移 P3)。
// 语义与 JPA 侧 @SoftDelete(columnName = "is_deleted") 逐字对齐:
// - 查询自动附加 is_deleted = false;
// - 「删除」是 UPDATE ... SET is_deleted = true(P1 钉死:不写 deleted_at/deleted_by)。
// 使用约定:单表查询用 selectActive/countActive;与聚合表达式、join 多表组合时用 activeOnly
// 取条件再手动 and(join 场景每个软删表都要各自过滤);软删用 softDeleteWhere。

/** 软删过滤条件:`is_deleted = false AND where()`。供 join/聚合/删除等场景组合。 */
fun SoftDeletableTable.activeOnly(where: () -> Op<Boolean> = { Op.TRUE }): Op<Boolean> {
  val userOp = where()
  val notDeleted = isDeleted.eq(false)
  return if (userOp == Op.TRUE) notDeleted else notDeleted.and(userOp)
}

/** 单表查询活跃行(自动附加 `is_deleted = false`)。 */
fun SoftDeletableTable.selectActive(where: () -> Op<Boolean> = { Op.TRUE }): Query = selectAll().where { activeOnly(where) }

/** 活跃行计数(自动附加 `is_deleted = false`)。 */
fun SoftDeletableTable.countActive(where: () -> Op<Boolean> = { Op.TRUE }): Long =
  selectAll()
    .where { activeOnly(where) }
    .count()

/**
 * 软删:把满足条件的活跃行标记为 `is_deleted = true`,返回受影响行数。
 * 与 JPA `@SoftDelete` / P1 仓储语义一致:不写 deleted_at/deleted_by。
 */
fun SoftDeletableTable.softDeleteWhere(where: () -> Op<Boolean>): Int = update({ activeOnly(where) }) { it[isDeleted] = true }
