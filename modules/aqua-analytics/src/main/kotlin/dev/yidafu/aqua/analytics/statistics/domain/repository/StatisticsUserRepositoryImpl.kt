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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import dev.yidafu.aqua.analytics.common.exposed.dateTrunc
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 用户统计仓储实现(JPA→Exposed 迁移 P3)。口径与原 QueryDSL 版一致。
 * ResultRow 取值全部在事务闭包内(聚合表达式 hashCode 依赖事务上下文)。
 */
@Repository
class StatisticsUserRepositoryImpl(
  private val database: Database,
) : StatisticsUserRepositoryCustom {
  override fun countDailyNewUsers(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<LocalDate, Long> {
    // 按日 SQL GROUP BY(DATE_TRUNC 返回 TIMESTAMP,H2/Postgres 兼容,查询侧转 LocalDate —— 与 getDailyOrderStatistics 同模式),
    // 避免全量拉取 createdAt 后内存分组(P2-9/规则 13)。
    // 注意不能用 CAST(... AS DATE):H2/JDBC 返回 java.sql.Date 而非 LocalDate,按 LocalDate 取键会失配。
    val dateExpr = dateTrunc("day", UsersTable.createdAt)
    val userCount = UsersTable.id.count()
    return transaction(database) {
      UsersTable
        .select(dateExpr, userCount)
        .andWhere {
          (UsersTable.createdAt greaterEq startDateTime) and
            (UsersTable.createdAt less endDateTime)
        }.groupBy(dateExpr)
        .toList()
        .associate { row -> row[dateExpr]!!.toLocalDate() to row[userCount] }
    }
  }

  override fun countUsersCreatedAfter(dateTime: LocalDateTime): Long =
    transaction(database) {
      UsersTable
        .select(UsersTable.id)
        .andWhere { UsersTable.createdAt greaterEq dateTime }
        .count()
    }

  override fun countActiveUsersSince(dateTime: LocalDateTime): Long {
    // 原子查询:窗口内下过单的用户 id(去重),再对 users 计数(语义同原 IN 子查询)
    val activeUserIds =
      transaction(database) {
        OrdersTable
          .select(OrdersTable.userId)
          .andWhere { OrdersTable.createdAt greaterEq dateTime }
          .withDistinct()
          .toList()
      }.map { it[OrdersTable.userId] }

    if (activeUserIds.isEmpty()) return 0L

    return transaction(database) {
      UsersTable
        .select(UsersTable.id)
        .andWhere { UsersTable.id inList activeUserIds }
        .count()
    }
  }
}
