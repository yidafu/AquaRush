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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository :
  JpaRepository<OrderModel, Long>,
  OrderRepositoryCustom {
  fun findByOrderNo(orderNo: String): OrderModel?

  fun findByUserId(userId: Long): List<OrderModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  fun findByStatus(status: OrderModelStatus): List<OrderModel>

  fun findByStatusOrderByCreatedAtAsc(status: OrderModelStatus): List<OrderModel>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<OrderModel>

  fun countByStatus(status: OrderModelStatus): Long
}
