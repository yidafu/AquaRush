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

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QOrderModel
import dev.yidafu.aqua.common.domain.model.QUserModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class StatisticsUserRepositoryImpl : StatisticsUserRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun countDailyNewUsers(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<LocalDate, Long> {
    val userModel = QUserModel.userModel

    // 按日 SQL GROUP BY(DATE_TRUNC 返回 TIMESTAMP,H2/Postgres 兼容,查询侧转 LocalDate —— 与 getDailyOrderStatistics 同模式),
    // 避免全量拉取 createdAt 后内存分组(P2-9/规则 13)。
    // 注意不能用 CAST(... AS DATE):H2/JDBC 返回 java.sql.Date 而非 LocalDate,按 LocalDate 取键会失配。
    val dateExpr = Expressions.dateTemplate(LocalDateTime::class.java, "DATE_TRUNC('day', {0})", userModel.createdAt)
    return queryFactory
      .select(dateExpr, userModel.count())
      .from(userModel)
      .where(
        userModel.createdAt.goe(startDateTime),
        userModel.createdAt.lt(endDateTime),
      ).groupBy(dateExpr)
      .fetch()
      .associate { row ->
        row.get(dateExpr)!!.toLocalDate() to (row.get(userModel.count()) ?: 0L)
      }
  }

  override fun countUsersCreatedAfter(dateTime: LocalDateTime): Long {
    val userModel = QUserModel.userModel
    return queryFactory
      .query()
      .from(userModel)
      .where(userModel.createdAt.goe(dateTime))
      .fetchCount()
  }

  override fun countActiveUsersSince(dateTime: LocalDateTime): Long {
    val userModel = QUserModel.userModel
    val orderModel = QOrderModel.orderModel

    return queryFactory
      .query()
      .from(userModel)
      .where(
        userModel.id.`in`(
          queryFactory
            .select(orderModel.userId)
            .from(orderModel)
            .where(orderModel.createdAt.goe(dateTime))
            .distinct(),
        ),
      ).fetchCount()
  }
}
