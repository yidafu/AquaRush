/*
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

package dev.yidafu.aqua.common.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.QOrderModel.Companion.orderModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Enhanced OrderRepository implementation using QueryDSL
 * Provides type-safe complex queries for order management
 */
@Repository
class OrderRepositoryImpl : OrderRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findOrdersWithFilters(
    userId: Long?,
    status: OrderStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    orderNumber: String?,
    statuses: List<OrderStatus>?,
  ): List<OrderModel> {
    val builder = BooleanBuilder()

    userId?.let { builder.and(orderModel.userId.eq(it)) }
    status?.let { builder.and(orderModel.status.eq(it)) }
    deliveryWorkerId?.let { builder.and(orderModel.deliveryWorkerId.eq(it)) }
    orderNumber?.let { builder.and(orderModel.orderNo.eq(it)) }
    statuses?.let { builder.and(orderModel.status.`in`(it)) }
    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(orderModel.createdAt.between(start, end))
      }
    }

    return queryFactory
      .selectFrom(orderModel)
      .where(builder)
      .orderBy(orderModel.createdAt.desc())
      .fetch()
  }

  override fun findDeliveryWorkerOrdersWithFilters(
    deliveryWorkerId: Long,
    status: OrderStatus,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    limit: Int?,
  ): List<OrderModel> {
    var query =
      queryFactory
        .selectFrom(orderModel)
        .where(
          orderModel.deliveryWorkerId
            .eq(deliveryWorkerId)
            .and(orderModel.status.eq(status)),
        )

    startDate?.let { start ->
      endDate?.let { end ->
        query = query.where(orderModel.createdAt.between(start, end))
      }
    }

    var result = query.orderBy(orderModel.createdAt.desc())

    limit?.let {
      result = result.limit(it.toLong())
    }

    return result.fetch()
  }

  override fun countOrdersWithFilters(
    userId: Long?,
    status: OrderStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    statuses: List<OrderStatus>?,
  ): Long {
    val builder = BooleanBuilder()

    userId?.let { builder.and(orderModel.userId.eq(it)) }
    status?.let { builder.and(orderModel.status.eq(it)) }
    deliveryWorkerId?.let { builder.and(orderModel.deliveryWorkerId.eq(it)) }
    statuses?.let { builder.and(orderModel.status.`in`(it)) }
    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(orderModel.createdAt.between(start, end))
      }
    }

    return queryFactory
      .query()
      .from(orderModel)
      .where(builder)
      .fetchCount()
  }

  override fun findOrdersPaginated(
    keyword: String?,
    status: OrderStatus?,
    userId: Long?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    minAmount: Long?,
    maxAmount: Long?,
    page: Int,
    size: Int,
    sortField: String,
    sortDirection: String,
  ): org.springframework.data.domain.Page<OrderModel> {
    val builder = BooleanBuilder()

    keyword?.let { keywordVal ->
      // Search in order number (using contains)
      builder.and(orderModel.orderNo.containsIgnoreCase(keywordVal))
    }
    status?.let { builder.and(orderModel.status.eq(it)) }
    userId?.let { builder.and(orderModel.userId.eq(it)) }
    deliveryWorkerId?.let { builder.and(orderModel.deliveryWorkerId.eq(it)) }

    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(orderModel.createdAt.between(start, end))
      }
    }

    minAmount?.let { builder.and(orderModel.amountCents.goe(it)) }
    maxAmount?.let { builder.and(orderModel.amountCents.loe(it)) }

    // Build sorting - use Spring Data Sort
    val sortFieldName =
      when (sortField.lowercase()) {
        "totalamount", "total_amount" -> "amountCents"
        "updatedat", "updated_at" -> "updatedAt"
        else -> "createdAt"
      }

    val springSort =
      if (sortDirection.equals("asc", ignoreCase = true)) {
        Sort.by(Sort.Order.asc(sortFieldName))
      } else {
        Sort.by(Sort.Order.desc(sortFieldName))
      }

    val pageable = PageRequest.of(page, size, springSort)

    val query =
      queryFactory
        .selectFrom(orderModel)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())

    // Apply ordering using QueryDSL
    when (sortField.lowercase()) {
      "totalamount", "total_amount" -> {
        if (sortDirection.equals("asc", ignoreCase = true)) {
          query.orderBy(orderModel.amountCents.asc())
        } else {
          query.orderBy(orderModel.amountCents.desc())
        }
      }

      "updatedat", "updated_at" -> {
        if (sortDirection.equals("asc", ignoreCase = true)) {
          query.orderBy(orderModel.updatedAt.asc())
        } else {
          query.orderBy(orderModel.updatedAt.desc())
        }
      }

      else -> {
        if (sortDirection.equals("asc", ignoreCase = true)) {
          query.orderBy(orderModel.createdAt.asc())
        } else {
          query.orderBy(orderModel.createdAt.desc())
        }
      }
    }

    @Suppress("UNCHECKED_CAST")
    val content = query.fetch() as List<OrderModel>
    val total =
      queryFactory
        .query()
        .from(orderModel)
        .where(builder)
        .fetchCount()

    return PageImpl(content, pageable, total)
  }

  @Transactional
  override fun bulkUpdateOrderStatus(
    orderIds: List<Long>,
    newStatus: OrderStatus,
    deliveryWorkerId: Long?,
  ): Int {
    var update =
      queryFactory
        .update(orderModel)
        .set(orderModel.status, newStatus)
        .where(orderModel.id.`in`(orderIds))

    deliveryWorkerId?.let {
      update = update.set(orderModel.deliveryWorkerId, it)
    }

    return update.execute().toInt()
  }

  override fun getOrderAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderAnalyticsRow> {
    // Create date expression for PostgreSQL DATE() function
    val dateExpr =
      Expressions.dateTemplate(
        java.time.LocalDate::class.java,
        "DATE({0})",
        orderModel.createdAt,
      )

    val sumAmount: NumberExpression<Long> = orderModel.amountCents.sumLong()
    val avgAmount = orderModel.amountCents.avg()

    val results =
      queryFactory
        .select(
          dateExpr,
          orderModel.status,
          orderModel.count(),
          sumAmount,
          avgAmount,
          orderModel.userId.countDistinct(),
          orderModel.deliveryWorkerId.countDistinct(),
        ).from(orderModel)
        .where(orderModel.createdAt.between(startDate, endDate))
        .groupBy(dateExpr, orderModel.status)
        .orderBy(dateExpr.desc(), orderModel.status.asc())
        .fetch()

    @Suppress("UNCHECKED_CAST")
    return results.map { tuple ->
      OrderAnalyticsRow(
        orderDate = tuple.get(dateExpr) ?: java.time.LocalDate.now(),
        status = tuple.get(orderModel.status) ?: OrderStatus.PENDING_PAYMENT,
        orderCount = tuple.get(orderModel.count()) ?: 0L,
        // Convert from cents to yuan (divide by 100)
        totalRevenue = (tuple.get(sumAmount) as? Long? ?: 0L).toDouble() / 100.0,
        averageOrderValue = (tuple.get(avgAmount) as? Double? ?: 0.0) / 100.0,
        uniqueCustomers = tuple.get(orderModel.userId.countDistinct()) ?: 0L,
        activeWorkers = tuple.get(orderModel.deliveryWorkerId.countDistinct()) ?: 0L,
      )
    }
  }
}

/**
 * Data class for order analytics results
 */
data class OrderAnalyticsRow(
  val orderDate: java.time.LocalDate,
  val status: OrderStatus,
  val orderCount: Long,
  val totalRevenue: Double,
  val averageOrderValue: Double,
  val uniqueCustomers: Long,
  val activeWorkers: Long,
)

interface OrderRepositoryCustom {
  fun findOrdersWithFilters(
    userId: Long?,
    status: OrderStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    orderNumber: String?,
    statuses: List<OrderStatus>?,
  ): List<OrderModel>

  fun findDeliveryWorkerOrdersWithFilters(
    deliveryWorkerId: Long,
    status: OrderStatus,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    limit: Int?,
  ): List<OrderModel>

  fun countOrdersWithFilters(
    userId: Long?,
    status: OrderStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    statuses: List<OrderStatus>?,
  ): Long

  /**
   * 分页查询订单
   */
  fun findOrdersPaginated(
    keyword: String? = null,
    status: OrderStatus? = null,
    userId: Long? = null,
    deliveryWorkerId: Long? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    minAmount: Long? = null,
    maxAmount: Long? = null,
    page: Int = 0,
    size: Int = 20,
    sortField: String = "createdAt",
    sortDirection: String = "desc",
  ): org.springframework.data.domain.Page<OrderModel>

  fun bulkUpdateOrderStatus(
    orderIds: List<Long>,
    newStatus: OrderStatus,
    deliveryWorkerId: Long?,
  ): Int

  fun getOrderAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderAnalyticsRow>
}
