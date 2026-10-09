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
import java.time.LocalDateTime
import java.util.Optional

/**
 * 订单仓储(orders,trade 域)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法
 * (含原 JPA 继承方法 findById/findAll/findAllById/save 的显式声明);
 * 查询过滤 is_deleted(软删可见性与原 @SoftDelete 一致);
 * 复杂聚合/分页/原子更新见 [OrderRepositoryCustom]。
 *
 * 注意:原 JPA EAGER 关联字段(user/address/deliveryWorker)由 Exposed 实现装载为 null
 * (列值经 userId/addressId/deliveryWorkerId 提供,由服务层/Resolver 联查补全)。
 */
interface OrderRepository : OrderRepositoryCustom {
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
   * 排序取 pageable.sort(支持 id/createdAt/updatedAt/amountCents,未知属性忽略);
   * 无排序时按 id 升序破平 —— 原 JPA derived 无序,取稳定游标。
   */
  fun findByStatusAndDeliveryStartedAtBefore(
    status: OrderModelStatus,
    before: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel>

  /**
   * 分页查询已送达且送达时间早于截止时刻的待确认收货订单(R-ORD-11 自动签收扫描用,
   * 分页游标,禁止全表,CLAUDE.md 规则 13):status=AWAITING_CONFIRMATION 且
   * delivery_confirmed_at < 截止时刻(送达 N 天未确认)。
   * 排序口径同 [findByStatusAndDeliveryStartedAtBefore](调用方传 Sort by id ASC)。
   */
  fun findByStatusAndDeliveryConfirmedAtBefore(
    status: OrderModelStatus,
    before: LocalDateTime,
    pageable: Pageable,
  ): Page<OrderModel>

  fun findByDeliveryWorkerIdOrderByCreatedAtDesc(deliveryWorkerId: Long): List<OrderModel>

  fun findByAddressId(addressId: Long): List<OrderModel>

  fun countByStatus(status: OrderModelStatus): Long

  fun findById(id: Long): Optional<OrderModel>

  /** 全量读取(调用方:findAllOrders 展示;原 JPA 无序,按 id 升序破平)。 */
  fun findAll(): List<OrderModel>

  fun findAllById(ids: Iterable<Long>): List<OrderModel>

  /**
   * upsert 语义:无 id 或 id 不存在时插入(雪花 id,与原 @SnowflakeIdGenerator 预分配一致),
   * 否则按 id 更新业务列(updatedAt=now,等价原 @PreUpdate);createdAt 不可变(updatable=false)。
   */
  fun save(entity: OrderModel): OrderModel
}
