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

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.common.domain.model.QOrderModel.Companion.orderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class StatisticsOrderRepositoryImpl : StatisticsOrderRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun getDailyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): List<DailyStatisticsDTO> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId, statuses)

    // 使用 DATE(created_at) 按日期分组
    val dateExpr = Expressions.dateTemplate(LocalDate::class.java, "DATE({0})", orderModel.createdAt)

    val results =
      queryFactory
        .select(
          dateExpr,
          orderModel.count(),
          orderModel.amountCents.sumLong(),
          orderModel.quantity.sumLong(),
        ).from(orderModel)
        .where(whereClause)
        .groupBy(dateExpr)
        .fetch()

    return results.map { row ->
      DailyStatisticsDTO(
        date = row.get(dateExpr)!!,
        orderCount = row.get(orderModel.count())!!,
        orderProductCount = row.get(orderModel.quantity.sumLong()) ?: 0L,
        totalAmountCents = row.get(orderModel.amountCents.sumLong()) ?: 0L,
      )
    }
  }

  override fun getWeeklyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): List<DailyStatisticsDTO> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId, statuses)

    // 使用 DATE_TRUNC('week', created_at) 按周分组（周一作为周开始）
    // DATE_TRUNC('week') 返回 LocalDateTime，需要转换为 LocalDate
    val weekExpr = Expressions.dateTemplate(LocalDateTime::class.java, "DATE_TRUNC('week', {0})", orderModel.createdAt)

    val results =
      queryFactory
        .select(
          weekExpr,
          orderModel.count(),
          orderModel.amountCents.sumLong(),
          orderModel.quantity.sumLong(),
        ).from(orderModel)
        .where(whereClause)
        .groupBy(weekExpr)
        .fetch()

    return results.map { row ->
      DailyStatisticsDTO(
        date = row.get(weekExpr)!!.toLocalDate(),
        orderCount = row.get(orderModel.count())!!,
        orderProductCount = row.get(orderModel.quantity.sumLong()) ?: 0L,
        totalAmountCents = row.get(orderModel.amountCents.sumLong()) ?: 0L,
      )
    }
  }

  override fun getMonthlyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): List<DailyStatisticsDTO> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId, statuses)

    // 使用 DATE_TRUNC('month', created_at) 按月分组，返回 LocalDateTime 需要转换为 LocalDate
    val monthExpr = Expressions.dateTemplate(LocalDateTime::class.java, "DATE_TRUNC('month', {0})", orderModel.createdAt)

    val results =
      queryFactory
        .select(
          monthExpr,
          orderModel.count(),
          orderModel.amountCents.sumLong(),
          orderModel.quantity.sumLong(),
        ).from(orderModel)
        .where(whereClause)
        .groupBy(monthExpr)
        .fetch()

    return results.map { row ->
      DailyStatisticsDTO(
        date = row.get(monthExpr)!!.toLocalDate(),
        orderCount = row.get(orderModel.count())!!,
        orderProductCount = row.get(orderModel.quantity.sumLong()) ?: 0L,
        totalAmountCents = row.get(orderModel.amountCents.sumLong()) ?: 0L,
      )
    }
  }

  private fun buildWhereClause(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): BooleanBuilder {
    val baseCondition = orderModel.createdAt.goe(startDateTime).and(orderModel.createdAt.loe(endDateTime))
    val whereClause = BooleanBuilder(baseCondition)

    deliveryWorkerId?.let {
      whereClause.and(orderModel.deliveryWorkerId.eq(it))
    }

    statuses?.let {
      whereClause.and(orderModel.status.`in`(it))
    }

    return whereClause
  }
}
