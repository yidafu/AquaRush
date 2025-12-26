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

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import dev.yidafu.aqua.common.domain.model.QOrderModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Enhanced OrderRepository implementation with modern Spring Data JPA 3.0+ features
 * Provides type-safe complex queries for order management
 */
@Repository
class OrderRepositoryImpl(
  @PersistenceContext private val entityManager: EntityManager,
) {
  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }
  /**
   * Find orders with complex filtering criteria
   * Demonstrates flexible query building with multiple conditions
   */
  fun findOrdersWithFilters(
    userId: Long? = null,
    status: OrderStatus? = null,
    deliveryWorkerId: Long? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    orderNumber: String? = null,
    statuses: List<OrderStatus>? = null,
  ): List<OrderModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(OrderModel::class.java)
    val root = query.from(OrderModel::class.java)

    val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

    userId?.let {
      predicates.add(cb.equal(root.get<Long>("userId"), it))
    }

    status?.let {
      predicates.add(cb.equal(root.get<OrderStatus>("status"), it))
    }

    deliveryWorkerId?.let {
      predicates.add(cb.equal(root.get<Long>("deliveryWorkerId"), it))
    }

    orderNumber?.let {
      predicates.add(cb.equal(root.get<String>("orderNumber"), it))
    }

    statuses?.let { statusList ->
      predicates.add(root.get<OrderStatus>("status").`in`(statusList))
    }

    startDate?.let { start ->
      endDate?.let { end ->
        predicates.add(cb.between(root.get<LocalDateTime>("createdAt"), start, end))
      }
    }

    if (predicates.isNotEmpty()) {
      query.where(cb.and(*predicates.toTypedArray()))
    }

    query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))
    return entityManager.createQuery(query).resultList
  }

  /**
   * Find orders for a delivery worker with specific status and date range
   * Combines multiple conditions efficiently
   */
  fun findDeliveryWorkerOrdersWithFilters(
    deliveryWorkerId: Long,
    status: OrderStatus,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    limit: Int? = null,
  ): List<OrderModel> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(OrderModel::class.java)
    val root = query.from(OrderModel::class.java)

    val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

    predicates.add(cb.equal(root.get<Long>("deliveryWorkerId"), deliveryWorkerId))
    predicates.add(cb.equal(root.get<OrderStatus>("status"), status))

    startDate?.let { start ->
      endDate?.let { end ->
        predicates.add(cb.between(root.get<LocalDateTime>("createdAt"), start, end))
      }
    }

    if (predicates.isNotEmpty()) {
      query.where(cb.and(*predicates.toTypedArray()))
    }

    query.orderBy(cb.desc(root.get<LocalDateTime>("createdAt")))

    val typedQuery = entityManager.createQuery(query)
    limit?.let {
      typedQuery.maxResults = it
    }

    return typedQuery.resultList
  }

  /**
   * Count orders by multiple criteria for reporting
   */
  fun countOrdersWithFilters(
    userId: Long? = null,
    status: OrderStatus? = null,
    deliveryWorkerId: Long? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    statuses: List<OrderStatus>? = null,
  ): Long {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(OrderModel::class.java)

    val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

    userId?.let {
      predicates.add(cb.equal(root.get<Long>("userId"), it))
    }

    status?.let {
      predicates.add(cb.equal(root.get<OrderStatus>("status"), it))
    }

    deliveryWorkerId?.let {
      predicates.add(cb.equal(root.get<Long>("deliveryWorkerId"), it))
    }

    statuses?.let { statusList ->
      predicates.add(root.get<OrderStatus>("status").`in`(statusList))
    }

    startDate?.let { start ->
      endDate?.let { end ->
        predicates.add(cb.between(root.get<LocalDateTime>("createdAt"), start, end))
      }
    }

    if (predicates.isNotEmpty()) {
      query.where(cb.and(*predicates.toTypedArray()))
    }

    query.select(cb.count(root))
    return entityManager.createQuery(query).singleResult ?: 0L
  }

  /**
   * Bulk update order status for efficient operations
   */
  fun bulkUpdateOrderStatus(
    orderIds: List<Long>,
    newStatus: OrderStatus,
    deliveryWorkerId: Long? = null,
  ): Int {
    val cb = entityManager.criteriaBuilder
    val update = cb.createCriteriaUpdate(OrderModel::class.java)
    val root = update.root

    val idPredicate = root.get<Long>("id").`in`(orderIds)
    update.set(root.get<OrderStatus>("status"), newStatus)

    deliveryWorkerId?.let {
      update.set(root.get<Long>("deliveryWorkerId"), it)
    }

    update.where(idPredicate)
    return entityManager.createQuery(update).executeUpdate()
  }

  /**
   * QueryDSL implementation for complex order analytics
   */
  fun getOrderAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderAnalyticsRow> {
    val qOrder = QOrderModel.orderModel

    // Create date expression for PostgreSQL DATE() function
    val dateExpr = Expressions.dateTemplate(
      java.time.LocalDate::class.java,
      "DATE({0})",
      qOrder.createdAt
    )

    val results = queryFactory
      .select(
        dateExpr,
        qOrder.status,
        qOrder.count(),
        qOrder.amountCents.sum().coalesce(0L),
        qOrder.amountCents.avg().coalesce(0.0),
        qOrder.userId.countDistinct(),
        qOrder.deliveryWorkerId.countDistinct()
      )
      .from(qOrder)
      .where(qOrder.createdAt.between(startDate, endDate))
      .groupBy(dateExpr, qOrder.status)
      .orderBy(dateExpr.desc(), qOrder.status.asc())
      .fetch()

    return results.map { tuple ->
      OrderAnalyticsRow(
        orderDate = tuple.get(dateExpr) ?: java.time.LocalDate.now(),
        status = tuple.get(qOrder.status) ?: OrderStatus.PENDING_PAYMENT,
        orderCount = tuple.get(qOrder.count()) ?: 0L,
        // Convert from cents to yuan (divide by 100)
        totalRevenue = (tuple.get(qOrder.amountCents.sum()) ?: 0L).toDouble() / 100.0,
        averageOrderValue = (tuple.get(qOrder.amountCents.avg()) ?: 0.0) / 100.0,
        uniqueCustomers = tuple.get(qOrder.userId.countDistinct()) ?: 0L,
        activeWorkers = tuple.get(qOrder.deliveryWorkerId.countDistinct()) ?: 0L
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
