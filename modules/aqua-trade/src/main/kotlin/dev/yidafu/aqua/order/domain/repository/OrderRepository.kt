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

package dev.yidafu.aqua.order.domain.repository

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OrderRepository :
  JpaRepository<OrderModel, Long>,
  OrderRepositoryCustom {
  fun findByOrderNo(orderNo: String): OrderModel?

  /**
   * 服务端下单幂等(R-ORD-06):同一用户同一 clientRequestId 已下单则直接返回已存在订单,
   * 由 (user_id, client_request_id) 唯一索引兜底并发重复提交。
   */
  fun findByUserIdAndClientRequestId(
    userId: Long,
    clientRequestId: String,
  ): OrderModel?

  fun findByUserId(userId: Long): List<OrderModel>

  fun findByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): List<OrderModel>

  fun existsByUserIdAndStatus(
    userId: Long,
    status: OrderModelStatus,
  ): Boolean

  fun findByStatus(status: OrderModelStatus): List<OrderModel>

  fun findByStatusOrderByCreatedAtAsc(status: OrderModelStatus): List<OrderModel>

  /**
   * 分页查询指定状态且开始配送时间早于截止时刻的订单(配送超时扫描用,分页游标,规则 13)。
   */
  fun findByStatusAndDeliveryStartedAtBefore(
    status: OrderModelStatus,
    before: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<OrderModel>

  fun findByAddressId(addressId: Long): List<OrderModel>

  fun countByStatus(status: OrderModelStatus): Long
}
