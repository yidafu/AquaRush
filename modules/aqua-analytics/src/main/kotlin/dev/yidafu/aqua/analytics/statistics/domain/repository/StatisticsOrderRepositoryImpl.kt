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
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.common.domain.model.QOrderItemModel.Companion.orderItemModel
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

    // 使用 DATE_TRUNC('day', created_at) 按日期分组(Postgres/H2 兼容;返回 TIMESTAMP,查询侧转 LocalDate)
    val dateExpr = Expressions.dateTemplate(LocalDateTime::class.java, "DATE_TRUNC('day', {0})", orderModel.createdAt)

    // 查询 1:仅 orders 表聚合 —— 金额按订单只计一次(不 join order_items,避免多商品订单营收按明细行数重复求和,PRD F-ORD-004 / P1-3)
    val orderRows =
      queryFactory
        .select(
          dateExpr,
          orderModel.id.countDistinct(),
          orderModel.amountCents.sumLong(),
        ).from(orderModel)
        .where(whereClause)
        .groupBy(dateExpr)
        .fetch()
        .map { row ->
          row.get(dateExpr)!!.toLocalDate() to
            (row.get(orderModel.id.countDistinct())!! to (row.get(orderModel.amountCents.sumLong()) ?: 0L))
        }

    // 查询 2:商品件数按 order_items 明细汇总(F-ORD-004 多商品下单,orders 无 quantity 列;仅取件数,不与金额混算)
    val quantityRows =
      queryFactory
        .select(
          dateExpr,
          orderItemModel.quantity.sumLong().coalesce(0L),
        ).from(orderModel)
        .leftJoin(orderItemModel)
        .on(orderItemModel.orderId.eq(orderModel.id))
        .where(whereClause)
        .groupBy(dateExpr)
        .fetch()
        .map { row ->
          row.get(dateExpr)!!.toLocalDate() to (row.get(orderItemModel.quantity.sumLong().coalesce(0L)) ?: 0L)
        }

    return mergeDailyStatistics(orderRows, quantityRows)
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

    // 查询 1:仅 orders 表聚合(金额按订单只计一次,避免多商品订单营收虚增, P1-3)
    val orderRows =
      queryFactory
        .select(
          weekExpr,
          orderModel.id.countDistinct(),
          orderModel.amountCents.sumLong(),
        ).from(orderModel)
        .where(whereClause)
        .groupBy(weekExpr)
        .fetch()
        .map { row ->
          row.get(weekExpr)!!.toLocalDate() to
            (row.get(orderModel.id.countDistinct())!! to (row.get(orderModel.amountCents.sumLong()) ?: 0L))
        }

    // 查询 2:商品件数按周汇总(仅取件数,不与金额混算)
    val quantityRows =
      queryFactory
        .select(
          weekExpr,
          orderItemModel.quantity.sumLong().coalesce(0L),
        ).from(orderModel)
        .leftJoin(orderItemModel)
        .on(orderItemModel.orderId.eq(orderModel.id))
        .where(whereClause)
        .groupBy(weekExpr)
        .fetch()
        .map { row ->
          row.get(weekExpr)!!.toLocalDate() to (row.get(orderItemModel.quantity.sumLong().coalesce(0L)) ?: 0L)
        }

    return mergeDailyStatistics(orderRows, quantityRows)
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

    // 查询 1:仅 orders 表聚合(金额按订单只计一次,避免多商品订单营收虚增, P1-3)
    val orderRows =
      queryFactory
        .select(
          monthExpr,
          orderModel.id.countDistinct(),
          orderModel.amountCents.sumLong(),
        ).from(orderModel)
        .where(whereClause)
        .groupBy(monthExpr)
        .fetch()
        .map { row ->
          row.get(monthExpr)!!.toLocalDate() to
            (row.get(orderModel.id.countDistinct())!! to (row.get(orderModel.amountCents.sumLong()) ?: 0L))
        }

    // 查询 2:商品件数按月汇总(仅取件数,不与金额混算)
    val quantityRows =
      queryFactory
        .select(
          monthExpr,
          orderItemModel.quantity.sumLong().coalesce(0L),
        ).from(orderModel)
        .leftJoin(orderItemModel)
        .on(orderItemModel.orderId.eq(orderModel.id))
        .where(whereClause)
        .groupBy(monthExpr)
        .fetch()
        .map { row ->
          row.get(monthExpr)!!.toLocalDate() to (row.get(orderItemModel.quantity.sumLong().coalesce(0L)) ?: 0L)
        }

    return mergeDailyStatistics(orderRows, quantityRows)
  }

  override fun countByStatusIn(statuses: List<OrderModelStatus>): Map<OrderModelStatus, Long> =
    queryFactory
      .select(orderModel.status, orderModel.count())
      .from(orderModel)
      .where(orderModel.status.`in`(statuses))
      .groupBy(orderModel.status)
      .fetch()
      .associate { row ->
        row.get(orderModel.status)!! to (row.get(orderModel.count()) ?: 0L)
      }

  /**
   * 按状态分组统计日期窗口内订单数(G37 状态分布,全状态聚合)
   */
  override fun countOrdersGroupByStatus(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<OrderModelStatus, Long> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null)
    return queryFactory
      .select(orderModel.status, orderModel.count())
      .from(orderModel)
      .where(whereClause)
      .groupBy(orderModel.status)
      .fetch()
      .associate { row ->
        row.get(orderModel.status)!! to (row.get(orderModel.count()) ?: 0L)
      }
  }

  /**
   * 按订单创建小时(0-23)分组统计日期窗口内订单数(G37 高峰时段分析,全状态聚合)
   * EXTRACT(HOUR FROM ...) 与现有 DATE()/DATE_TRUNC() 同为 H2(PostgreSQL 模式)/PostgreSQL 兼容写法。
   * 注意:PostgreSQL 的 EXTRACT 返回 numeric,JDBC 无法直接转 Integer → 必须 CAST(... AS INT)
   * (否则抛 "conversion to class java.lang.Integer from numeric not supported" → INTERNAL_ERROR)。
   */
  override fun countOrdersGroupByHour(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<Int, Long> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null)
    val hourExpr =
      Expressions.numberTemplate(
        Integer::class.java,
        "CAST(EXTRACT(HOUR FROM {0}) AS INTEGER)",
        orderModel.createdAt,
      )
    return queryFactory
      .select(hourExpr, orderModel.count())
      .from(orderModel)
      .where(whereClause)
      .groupBy(hourExpr)
      .fetch()
      .associate { row ->
        row.get(hourExpr)!!.toInt() to (row.get(orderModel.count()) ?: 0L)
      }
  }

  /**
   * 按金额区间分桶统计日期窗口内订单数(G37 客单价分布,全状态聚合)
   * 金额按订单 amountCents 只计一次(与 P1-3 口径一致);buckets 由服务层定义(默认 0-20/20-50/50-100/100-200/200+ 元),
   * 按 fromCents 升序,末桶须为开区间(toCents = null,走 CASE ELSE)。
   */
  override fun countOrdersByAmountBucket(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    buckets: List<OrderValueBucketDTO>,
  ): Map<String, Long> {
    require(buckets.isNotEmpty()) { "客单价分布至少需要一个金额区间" }
    val sortedBuckets = buckets.sortedBy { it.fromCents ?: Long.MIN_VALUE }
    require(sortedBuckets.last().toCents == null) { "客单价分布末桶必须为开区间(toCents = null)" }

    // 构建 CASE WHEN {0} < toCents THEN label ... ELSE label END;边界/标签为服务层常量,内联无注入面
    val caseSql =
      buildString {
        append("case")
        sortedBuckets.forEach { bucket ->
          val to = bucket.toCents
          if (to != null) {
            append(" when {0} < ")
            append(to)
            append(" then '")
            append(bucket.label)
            append("'")
          } else {
            append(" else '")
            append(bucket.label)
            append("'")
          }
        }
        append(" end")
      }
    val bucketExpr = Expressions.stringTemplate(caseSql, orderModel.amountCents)

    return queryFactory
      .select(bucketExpr, orderModel.count())
      .from(orderModel)
      .where(buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null))
      .groupBy(bucketExpr)
      .fetch()
      .associate { row ->
        row.get(bucketExpr)!! to (row.get(orderModel.count()) ?: 0L)
      }
  }

  /**
   * 近30天下单用户分布(G35):orders 按 userId 分组计数(全状态聚合,与活跃用户同窗口同源)
   */
  override fun countOrdersPerUserInRange(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<Long, Long> =
    queryFactory
      .select(orderModel.userId, orderModel.id.count())
      .from(orderModel)
      .where(buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null))
      .groupBy(orderModel.userId)
      .fetch()
      .associate { row ->
        row.get(orderModel.userId)!! to (row.get(orderModel.id.count()) ?: 0L)
      }

  /**
   * 合并「仅订单聚合」与「明细件数聚合」两组结果(P1-3):
   * 金额只按订单计一次,商品件数按 order_items 明细汇总,两者按日期/周/月键合并。
   */
  private fun mergeDailyStatistics(
    orderRows: List<Pair<LocalDate, Pair<Long, Long>>>,
    quantityRows: List<Pair<LocalDate, Long>>,
  ): List<DailyStatisticsDTO> {
    val quantityMap = quantityRows.toMap()
    return orderRows.map { (date, counts) ->
      DailyStatisticsDTO(
        date = date,
        orderCount = counts.first,
        orderProductCount = quantityMap[date] ?: 0L,
        totalAmountCents = counts.second,
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
