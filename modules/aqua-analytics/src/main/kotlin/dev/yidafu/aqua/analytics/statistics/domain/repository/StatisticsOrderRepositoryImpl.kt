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
import dev.yidafu.aqua.analytics.common.exposed.extractHour
import dev.yidafu.aqua.analytics.common.exposed.longOf
import dev.yidafu.aqua.analytics.statistics.dto.DailyStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.OrderValueBucketDTO
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 订单统计仓储实现(JPA→Exposed 迁移 P3)。
 * 金额口径与原 QueryDSL 版逐字一致:金额按订单只计一次(orders 单表聚合,防 P1-3),
 * 商品件数按 order_items 明细汇总,两者按日/周/月键合并(F-ORD-004)。
 * ResultRow 取值全部在事务闭包内(聚合表达式 hashCode 依赖事务上下文)。
 */
@Repository
class StatisticsOrderRepositoryImpl(
  private val database: Database,
) : StatisticsOrderRepositoryCustom {
  override fun getDailyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): List<DailyStatisticsDTO> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId, statuses)
    val dateExpr = dateTrunc("day", OrdersTable.createdAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val amountSum = OrdersTable.totalAmountCents.sum()
    val qtySum = OrderItemsTable.quantity.sum()

    // 查询 1:仅 orders 表聚合 —— 金额按订单只计一次(不 join order_items,P1-3)
    val orderRows: List<Pair<LocalDate, Pair<Long, Long>>> =
      transaction(database) {
        OrdersTable
          .select(dateExpr, distinctOrders, amountSum)
          .andWhere(whereClause)
          .groupBy(dateExpr)
          .toList()
          .map { row -> row[dateExpr]!!.toLocalDate() to Pair(row[distinctOrders], row.longOf(amountSum)) }
      }

    // 查询 2:商品件数按 order_items 明细汇总(F-ORD-004 多商品下单;仅取件数,不与金额混算)
    val quantityRows: List<Pair<LocalDate, Long>> =
      transaction(database) {
        OrdersTable
          .join(OrderItemsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
          .select(dateExpr, qtySum)
          .where { OrdersTable.activeOnly(whereClause).and(OrderItemsTable.activeOnly()) }
          .groupBy(dateExpr)
          .toList()
          .map { row -> row[dateExpr]!!.toLocalDate() to row.longOf(qtySum) }
      }

    return mergeDailyStatistics(orderRows, quantityRows)
  }

  /**
   * 已完成营收按完成时间归属日(实时段营收口径,PRD F-STA-002):
   * status=COMPLETED 且 completed_at ∈ [start, end],按完成日分组 —— 跨日完成订单计入完成日。
   */
  override fun getDailyRevenueStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): List<DailyStatisticsDTO> {
    val dateExpr = dateTrunc("day", OrdersTable.completedAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val amountSum = OrdersTable.totalAmountCents.sum()
    return transaction(database) {
      OrdersTable
        .select(dateExpr, distinctOrders, amountSum)
        .andWhere {
          (OrdersTable.status eq OrderModelStatus.COMPLETED.name) and
            (OrdersTable.completedAt greaterEq startDateTime) and
            (OrdersTable.completedAt lessEq endDateTime)
        }.groupBy(dateExpr)
        .toList()
        .map { row ->
          DailyStatisticsDTO(
            date = row[dateExpr]!!.toLocalDate(),
            orderCount = row[distinctOrders],
            orderProductCount = 0L,
            totalAmountCents = row.longOf(amountSum),
          )
        }
    }
  }

  override fun getWeeklyOrderStatistics(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): List<DailyStatisticsDTO> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId, statuses)
    val weekExpr = dateTrunc("week", OrdersTable.createdAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val amountSum = OrdersTable.totalAmountCents.sum()
    val qtySum = OrderItemsTable.quantity.sum()

    // 查询 1:仅 orders 表聚合(金额按订单只计一次,P1-3)
    val orderRows: List<Pair<LocalDate, Pair<Long, Long>>> =
      transaction(database) {
        OrdersTable
          .select(weekExpr, distinctOrders, amountSum)
          .andWhere(whereClause)
          .groupBy(weekExpr)
          .toList()
          .map { row -> row[weekExpr]!!.toLocalDate() to Pair(row[distinctOrders], row.longOf(amountSum)) }
      }

    // 查询 2:商品件数按周汇总(仅取件数,不与金额混算)
    val quantityRows: List<Pair<LocalDate, Long>> =
      transaction(database) {
        OrdersTable
          .join(OrderItemsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
          .select(weekExpr, qtySum)
          .where { OrdersTable.activeOnly(whereClause).and(OrderItemsTable.activeOnly()) }
          .groupBy(weekExpr)
          .toList()
          .map { row -> row[weekExpr]!!.toLocalDate() to row.longOf(qtySum) }
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
    val monthExpr = dateTrunc("month", OrdersTable.createdAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val amountSum = OrdersTable.totalAmountCents.sum()
    val qtySum = OrderItemsTable.quantity.sum()

    val orderRows: List<Pair<LocalDate, Pair<Long, Long>>> =
      transaction(database) {
        OrdersTable
          .select(monthExpr, distinctOrders, amountSum)
          .andWhere(whereClause)
          .groupBy(monthExpr)
          .toList()
          .map { row -> row[monthExpr]!!.toLocalDate() to Pair(row[distinctOrders], row.longOf(amountSum)) }
      }

    val quantityRows: List<Pair<LocalDate, Long>> =
      transaction(database) {
        OrdersTable
          .join(OrderItemsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
          .select(monthExpr, qtySum)
          .where { OrdersTable.activeOnly(whereClause).and(OrderItemsTable.activeOnly()) }
          .groupBy(monthExpr)
          .toList()
          .map { row -> row[monthExpr]!!.toLocalDate() to row.longOf(qtySum) }
      }

    return mergeDailyStatistics(orderRows, quantityRows)
  }

  /** 按当前状态快照计数(存量统计)。 */
  override fun countByStatusIn(statuses: List<OrderModelStatus>): Map<OrderModelStatus, Long> {
    val statusCount = OrdersTable.id.count()
    return transaction(database) {
      OrdersTable
        .select(OrdersTable.status, statusCount)
        .andWhere { OrdersTable.status inList statuses.map { it.name } }
        .groupBy(OrdersTable.status)
        .toList()
        .associate { row -> OrderModelStatus.valueOf(row[OrdersTable.status]) to row[statusCount] }
    }
  }

  /** 按状态分组统计日期窗口内订单数(G37 状态分布,全状态聚合)。 */
  override fun countOrdersGroupByStatus(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<OrderModelStatus, Long> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null)
    val statusCount = OrdersTable.id.count()
    return transaction(database) {
      OrdersTable
        .select(OrdersTable.status, statusCount)
        .andWhere(whereClause)
        .groupBy(OrdersTable.status)
        .toList()
        .associate { row -> OrderModelStatus.valueOf(row[OrdersTable.status]) to row[statusCount] }
    }
  }

  /**
   * 按订单创建小时(0-23)分组统计(G37 高峰时段分析,全状态聚合)。
   * EXTRACT 返回 numeric,JDBC 无法直接转 Integer → 必须 CAST(... AS INTEGER)(原 G37 注释)。
   */
  override fun countOrdersGroupByHour(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<Int, Long> {
    val whereClause = buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null)
    val hourExpr = extractHour(OrdersTable.createdAt)
    val orderCount = OrdersTable.id.count()
    return transaction(database) {
      OrdersTable
        .select(hourExpr, orderCount)
        .andWhere(whereClause)
        .groupBy(hourExpr)
        .toList()
        .associate { row -> row[hourExpr] to row[orderCount] }
    }
  }

  /** 按金额区间分桶统计(G37 客单价分布):金额按订单 total_amount_cents 只计一次;
   * 桶区间互斥(服务层按 fromCents 升序定义、末桶开区间),等价于原 QueryDSL 的单条 CASE 分桶。
   */
  override fun countOrdersByAmountBucket(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    buckets: List<OrderValueBucketDTO>,
  ): Map<String, Long> {
    require(buckets.isNotEmpty()) { "客单价分布至少需要一个金额区间" }
    val sortedBuckets = buckets.sortedBy { it.fromCents ?: Long.MIN_VALUE }
    require(sortedBuckets.last().toCents == null) { "客单价分布末桶必须为开区间(toCents = null)" }

    return sortedBuckets.associate { bucket ->
      bucket.label to
        transaction(database) {
          OrdersTable
            .select(OrdersTable.id)
            .andWhere {
              (OrdersTable.createdAt greaterEq startDateTime) and
                (OrdersTable.createdAt lessEq endDateTime) and
                (if (bucket.fromCents != null) OrdersTable.totalAmountCents greaterEq bucket.fromCents!! else Op.TRUE) and
                (if (bucket.toCents != null) OrdersTable.totalAmountCents less bucket.toCents!! else Op.TRUE)
            }.count()
        }
    }
  }

  /** 近30天下单用户分布(G35):orders 按 userId 分组计数(全状态聚合,与活跃用户同窗口同源)。 */
  override fun countOrdersPerUserInRange(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
  ): Map<Long, Long> {
    val orderCount = OrdersTable.id.count()
    return transaction(database) {
      OrdersTable
        .select(OrdersTable.userId, orderCount)
        .andWhere(buildWhereClause(startDateTime, endDateTime, deliveryWorkerId = null, statuses = null))
        .groupBy(OrdersTable.userId)
        .toList()
        .associate { row -> row[OrdersTable.userId] to row[orderCount] }
    }
  }

  /** 合并「仅订单聚合」与「明细件数聚合」两组结果(P1-3)。 */
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
  ): () -> Op<Boolean> =
    {
      var op: Op<Boolean> =
        (OrdersTable.createdAt greaterEq startDateTime) and (OrdersTable.createdAt lessEq endDateTime)
      deliveryWorkerId?.let {
        op = op and (OrdersTable.deliveryWorkerId eq it)
      }
      statuses?.let {
        op = op and (OrdersTable.status inList it.map { s -> s.name })
      }
      op
    }
}
