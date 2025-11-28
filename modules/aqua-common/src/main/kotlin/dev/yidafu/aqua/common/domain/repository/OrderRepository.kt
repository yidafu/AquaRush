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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
  fun findByOrderNumber(orderNumber: String): Order?

  fun findByUserId(userId: Long): List<Order>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderStatus,
  ): List<Order>

  fun findByStatus(status: OrderStatus): List<Order>

  fun findByStatusOrderByCreatedAtAsc(status: OrderStatus): List<Order>

  fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<Order>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<Order>

  fun findByDeliveryWorkerIdAndStatusOrderByCreatedAtDesc(
    deliveryWorkerId: Long,
    status: OrderStatus,
  ): List<Order>

  fun countByDeliveryWorkerIdAndStatus(
    deliveryWorkerId: Long,
    status: OrderStatus,
  ): Long

  fun countByStatus(status: OrderStatus): Long

    // Enhanced query methods using modern Spring Data JPA 3.0+ features
    fun findOrdersWithFilters(
        userId: Long? = null,
        status: OrderStatus? = null,
        deliveryWorkerId: Long? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        orderNumber: String? = null,
        statuses: List<OrderStatus>? = null
    ): List<Order>

    fun findDeliveryWorkerOrdersWithFilters(
        deliveryWorkerId: Long,
        status: OrderStatus,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        limit: Int? = null
    ): List<Order>

    fun countOrdersWithFilters(
        userId: Long? = null,
        status: OrderStatus? = null,
        deliveryWorkerId: Long? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        statuses: List<OrderStatus>? = null
    ): Long
}
