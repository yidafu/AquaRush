package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import java.time.LocalDateTime

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

/**
 * 跨模块订单查询服务接口
 * 由 aqua-order 模块实现，供其他模块使用
 */
interface OrderQueryApiService {
  fun findById(id: Long): OrderModel?

  fun findByIds(ids: List<Long>): List<OrderModel>

  fun findByOrderNumber(orderNumber: String): OrderModel?

  fun findByUserId(userId: Long): List<OrderModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  fun findByStatus(status: OrderModelStatus): List<OrderModel>

  fun findByDeliveryWorkerId(deliveryWorkerId: Long): List<OrderModel>

  fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel>

  fun findByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel>

  fun countByStatusAndCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long

  fun sumAmountCentsByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long
}
