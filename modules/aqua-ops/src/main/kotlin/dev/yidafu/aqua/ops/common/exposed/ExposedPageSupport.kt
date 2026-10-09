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

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

/**
 * Exposed 分页查询助手：等价替代 `JpaSpecificationExecutor.findAll(spec, pageable)` 的
 * 「count + limit/offset」两段式语义。
 *
 * 用法：调用方已在 `transaction(db) { }` 内；本函数**不**开事务。
 * `buildQuery` 会被调用两次（一次取 total、一次取当页行），必须返回**全新**的 Query，
 * 避免 `count()` 与取数互相污染。
 *
 * 注意：`AGENTS.md` 规则 10 —— 所有列表查询必须分页 + 过滤，禁止无界 `SELECT`。
 * 本助手返回 `PageImpl` 以保持上层（GraphQL resolver 的 Page 字段解析）不变。
 */
internal fun <T : Any> pageQuery(
  buildQuery: () -> Query,
  orderings: List<Pair<Expression<*>, SortOrder>>,
  pageable: Pageable,
  mapper: (ResultRow) -> T,
): Page<T> {
  val total = buildQuery().count()
  val content =
    buildQuery()
      .orderBy(*orderings.toTypedArray())
      .limit(pageable.pageSize)
      .offset(pageable.offset)
      .map(mapper)
  return PageImpl(content, pageable, total)
}
