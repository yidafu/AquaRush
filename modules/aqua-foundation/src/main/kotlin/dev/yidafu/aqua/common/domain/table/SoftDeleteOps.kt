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

// [SoftDeleteFlagTable] 的组合场景软删条件(JPA→Exposed 迁移 P3)。
//
// 单表查询**不需要**它:SoftDeleteFlagTable 的 selectAll()/select() 成员已经把
// `is_deleted = false` 固定在查询入口上,追加条件用 andWhere {} 即可。
// 只有下列场景需要显式取条件再自己 and:
// - join:每个参与 join 的软删表都要各自过滤(Exposed 的 Join.joinParts 是 internal,
//   无法自动枚举参与 join 的表);
// - 聚合表达式 / groupBy / 子查询里手写 where;
// - 软删之外的批量 UPDATE ... WHERE(软删本身用 SoftDeleteFlagTable.softDelete {})。

/** 软删过滤条件:`is_deleted = false AND where()`。供 join/聚合/子查询组合。 */
fun SoftDeleteFlagTable.activeOnly(where: () -> Op<Boolean> = { Op.TRUE }): Op<Boolean> {
  val userOp = where()
  val notDeleted = isDeleted.eq(false)
  return if (userOp == Op.TRUE) notDeleted else notDeleted.and(userOp)
}
