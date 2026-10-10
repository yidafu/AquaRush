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

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * 只带 `is_deleted` 软删标记的表基类（无 `deleted_at`/`deleted_by` 留痕列）。
 *
 * 语义**必须**与 JPA 侧的 `@SoftDelete(columnName = "is_deleted")` 对齐：Hibernate 会把所有
 * 查询自动改写成 `is_deleted = false` 过滤 + `UPDATE ... SET is_deleted = true`。
 * Exposed 没有这类隐式改写，本类用**同名成员遮蔽扩展函数**把过滤固定到查询入口上：
 * Exposed 的 `selectAll()`/`select()` 是 `FieldSet` 上的扩展函数，而 Kotlin 解析规则里
 * 成员函数优先于扩展函数 —— 因此只要静态类型是本类，`XxxTable.selectAll()` 就一定是
 * 基类这个已过滤的成员，**单表查询漏过滤在语法上不再可能**（迁移方案 §9 R3）。
 *
 * 使用约定：
 * - 单表读：`XxxTable.selectAll().andWhere { … }` / `XxxTable.select(cols…).andWhere { … }`；
 *   追加条件**必须**用 `andWhere {}`/`orWhere {}` —— Exposed 的 `Query.where` 只能调用一次，
 *   第二次会抛 `IllegalStateException: WHERE clause is specified twice`；
 * - 计数：`countActive {}`；
 * - join、聚合表达式、子查询：`SoftDeleteOps.activeOnly {}` 取条件后手动 `and`（join 里每个
 *   软删表都要各自过滤）；
 * - 软删：`softDelete {}`（等价 JPA `@SoftDelete` 的 `UPDATE ... SET is_deleted = true`）；
 * - 审计/恢复、以及 `save()` 的主键存在性判定需要读到已删行时，**只能**走
 *   `selectAllIncludingDeleted()` 这个显式出口。
 *
 * DDL 另有 `deleted_at`/`deleted_by` 留痕列的表用 [SoftDeletableTable]。
 */
abstract class SoftDeleteFlagTable(
  name: String,
) : Table(name) {
  /**
   * 软删标记列，DDL 为 `boolean NOT NULL DEFAULT false`。
   * 用 clientDefault 而非 default：default 仅生成 DDL,运行时 INSERT 不附加该列;
   * 而部分环境的 DDL(如 Liquibase defaultValueBoolean)可能未落 DEFAULT,会导致 NULL 约束违反。
   */
  val isDeleted = bool("is_deleted").clientDefault { false }

  // ==================== 查询入口（自动带 is_deleted = false） ====================

  /** 单表查询活跃行；追加条件用 [andWhere]。 */
  fun selectAll(): Query = rawSelectAll().where { isDeleted eq false }

  /** 单表列裁剪/聚合查询活跃行；追加条件用 [andWhere]。 */
  fun select(
    expression: Expression<*>,
    vararg expressions: Expression<*>,
  ): Query = rawSelect(expression, *expressions).where { isDeleted eq false }

  /** 活跃行计数。 */
  fun countActive(where: () -> Op<Boolean> = { Op.TRUE }): Long = selectAll().andWhere(where).count()

  // ==================== 显式逃逸口（审计/恢复/主键存在性判定） ====================

  /**
   * **唯一**可以读到已删行的查询入口：审计、恢复、以及 `save()` 里“主键是否已存在”的判定。
   * 主键判定必须走这里 —— 若按活跃行判定，重启一个已软删的主键会落到 INSERT 上并撞主键唯一约束。
   */
  fun selectAllIncludingDeleted(): Query = rawSelectAll()

  // ==================== 软删 ====================

  /**
   * 软删：把满足条件的活跃行标记为 `is_deleted = true`，返回受影响行数。
   * 与 JPA `@SoftDelete` / P1 仓储语义一致：不写 deleted_at/deleted_by。
   */
  fun softDelete(where: () -> Op<Boolean>): Int = update({ activeOnly(where) }) { it[isDeleted] = true }

  // 上提到 Table 静态类型后只剩扩展函数可选，避免成员自递归。
  private fun rawSelectAll(): Query = (this as Table).selectAll()

  private fun rawSelect(
    expression: Expression<*>,
    vararg expressions: Expression<*>,
  ): Query = (this as Table).select(expression, *expressions)
}
