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

package dev.yidafu.aqua.analytics.common.exposed

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
import java.time.LocalDateTime

// Exposed 聚合查询使用的 SQL 函数(替代 QueryDSL Expressions.dateTemplate/numberTemplate);
// 拼接的 SQL 与原 QueryDSL 版逐字一致(H2 PostgreSQL 模式 / PostgreSQL 兼容)。

/** `DATE_TRUNC('<part>', expr)` —— 按日/周/月分组(P2-9:返回 TIMESTAMP,查询侧转 LocalDate)。 */
class DateTrunc(
  private val part: String,
  private val target: Expression<*>,
) : ExpressionWithColumnType<LocalDateTime>() {
  override val columnType: IColumnType<LocalDateTime> = JavaLocalDateTimeColumnType()

  override fun toQueryBuilder(queryBuilder: QueryBuilder) {
    queryBuilder.append("DATE_TRUNC('")
    queryBuilder.append(part)
    queryBuilder.append("', ")
    target.toQueryBuilder(queryBuilder)
    queryBuilder.append(")")
  }
}

fun dateTrunc(
  part: String,
  expr: Expression<*>,
): DateTrunc = DateTrunc(part, expr)

/**
 * `CAST(EXTRACT(HOUR FROM expr) AS INTEGER)` —— 订单高峰时段分组。
 * PostgreSQL 的 EXTRACT 返回 numeric,JDBC 无法直接转 Integer,必须 CAST(原 G37 注释)。
 */
class ExtractHour(
  private val target: Expression<*>,
) : ExpressionWithColumnType<Int>() {
  override val columnType: IColumnType<Int> = IntegerColumnType()

  override fun toQueryBuilder(queryBuilder: QueryBuilder) {
    queryBuilder.append("CAST(EXTRACT(HOUR FROM ")
    target.toQueryBuilder(queryBuilder)
    queryBuilder.append(") AS INTEGER)")
  }
}

fun extractHour(expr: Expression<*>): ExtractHour = ExtractHour(expr)

/**
 * 从 ResultRow 取数值聚合结果(SUM/COUNT 等返回 Number 族的泛型表达式)并转 Long,空值回退 0。
 * Exposed 对 sum() 的泛型推断为 Number & Comparable<*> 投影,直接取值赋给 Long 不匹配。
 */
fun org.jetbrains.exposed.v1.core.ResultRow.longOf(expr: Expression<*>): Long = (this[expr] as? Number)?.toLong() ?: 0L
