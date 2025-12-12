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

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface OrderRepository : JpaRepository<OrderModel, Long> {
  fun findByOrderNumber(orderNumber: String): OrderModel?

  fun findByUserId(userId: Long): List<OrderModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderStatus,
  ): List<OrderModel>

  fun findByStatus(status: OrderStatus): List<OrderModel>

  fun findByStatusOrderByCreatedAtAsc(status: OrderStatus): List<OrderModel>

  fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<OrderModel>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<OrderModel>

  fun findByDeliveryWorkerIdAndStatusOrderByCreatedAtDesc(
    deliveryWorkerId: Long,
    status: OrderStatus,
  ): List<OrderModel>

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
    statuses: List<OrderStatus>? = null,
  ): List<OrderModel>

  fun findDeliveryWorkerOrdersWithFilters(
    deliveryWorkerId: Long,
    status: OrderStatus,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    limit: Int? = null,
  ): List<OrderModel>

  fun countOrdersWithFilters(
    userId: Long? = null,
    status: OrderStatus? = null,
    deliveryWorkerId: Long? = null,
    startDate: LocalDateTime? = null,
    endDate: LocalDateTime? = null,
    statuses: List<OrderStatus>? = null,
  ): Long
}
