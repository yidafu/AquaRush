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

import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 统计日事实仓储的 Exposed 公共基类(JPA→Exposed 迁移 P3)。
 *
 * 覆盖 7 张 `daily_*_facts` 的公共行为,语义对齐原 Spring Data JpaRepository:
 * - [deleteByStatDateIn] 是**物理删除**(原派生 delete 语义;事实表无 is_deleted,与软删无关);
 * - [saveAll]/[save] 只做 INSERT(快照任务为 delete-then-insert 全新构建,无 update 路径;
 *   id 由 [SnowflakeIdGenerator] 生成,与 P1 ops 仓储同模式);
 * - 无软删过滤 —— 事实表不带 is_deleted。
 *
 * 子类提供表、id/stat_date/created_at/updated_at 列引用、行映射与业务列写入。
 */
abstract class ExposedDailyFactRepository<T : Any>(
  protected val database: Database,
  private val tableRef: Table,
  private val idColumn: Column<Long>,
  private val statDateColumn: Column<LocalDate>,
  private val createdAtColumn: Column<LocalDateTime>,
  private val updatedAtColumn: Column<LocalDateTime>,
  private val toModel: (ResultRow) -> T,
) {
  /** INSERT 业务列(id/stat_date/created_at/updated_at 由基类写)。 */
  protected abstract fun writeColumns(
    stmt: InsertStatement<Number>,
    entity: T,
  )

  /** 从实体取归属日(INSERT 写 stat_date 用)。 */
  protected abstract fun statDateOf(entity: T): LocalDate

  fun count(): Long =
    transaction(database) {
      tableRef.selectAll().count()
    }

  fun findByStatDateBetween(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<T> =
    transaction(database) {
      tableRef
        .selectAll()
        .where {
          (statDateColumn greaterEq startDate) and (statDateColumn lessEq endDate)
        }.map(toModel)
    }

  fun deleteByStatDateIn(dates: Collection<LocalDate>): Long =
    transaction(database) {
      tableRef.deleteWhere { statDateColumn inList dates }.toLong()
    }

  fun save(entity: T): T =
    transaction(database) {
      insert(entity)
    }

  fun saveAll(entities: List<T>): List<T> =
    transaction(database) {
      entities.map(::insert)
    }

  fun findFirstByOrderByStatDateDesc(): T? =
    transaction(database) {
      tableRef
        .selectAll()
        .orderBy(statDateColumn, SortOrder.DESC)
        .limit(1)
        .firstOrNull()
        ?.let(toModel)
    }

  fun findFirstByOrderByStatDateAsc(): T? =
    transaction(database) {
      tableRef
        .selectAll()
        .orderBy(statDateColumn, SortOrder.ASC)
        .limit(1)
        .firstOrNull()
        ?.let(toModel)
    }

  private fun insert(entity: T): T {
    val now = LocalDateTime.now()
    tableRef.insert {
      it[idColumn] = SnowflakeIdGenerator.getInstance().generate()
      it[statDateColumn] = statDateOf(entity)
      it[createdAtColumn] = now
      it[updatedAtColumn] = now
      writeColumns(it, entity)
    }
    return entity
  }
}
