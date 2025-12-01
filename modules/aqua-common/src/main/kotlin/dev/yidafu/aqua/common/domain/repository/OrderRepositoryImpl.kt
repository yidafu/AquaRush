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

import dev.yidafu.aqua.common.domain.model.Order
import dev.yidafu.aqua.common.domain.model.OrderStatus
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
  ): List<Order> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Order::class.java)
    val root = query.from(Order::class.java)

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
  ): List<Order> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Order::class.java)
    val root = query.from(Order::class.java)

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
    val root = query.from(Order::class.java)

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
    val update = cb.createCriteriaUpdate(Order::class.java)
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
   * Native query for complex order analytics
   */
  fun getOrderAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderAnalyticsRow> {
    val query =
      entityManager.createNativeQuery(
        """
        SELECT
            DATE(o.created_at) as order_date,
            o.status,
            COUNT(*) as order_count,
            SUM(o.total_amount) as total_revenue,
            AVG(o.total_amount) as average_order_value,
            COUNT(DISTINCT o.user_id) as unique_customers,
            COUNT(DISTINCT o.delivery_worker_id) as active_workers
        FROM orders o
        WHERE o.created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE(o.created_at), o.status
        ORDER BY order_date DESC, o.status
        """.trimIndent(),
      )

    query.setParameter("startDate", startDate)
    query.setParameter("endDate", endDate)

    val results = query.resultList as Array<Array<Any>>
    return results.map { row ->
      OrderAnalyticsRow(
        orderDate = row[0] as java.time.LocalDate,
        status = OrderStatus.valueOf(row[1] as String),
        orderCount = (row[2] as Number).toLong(),
        totalRevenue = (row[3] as Number).toDouble(),
        averageOrderValue = (row[4] as Number).toDouble(),
        uniqueCustomers = (row[5] as Number).toLong(),
        activeWorkers = (row[6] as Number).toLong(),
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
