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

import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.table.AdminsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 送水员统计仓储实现(JPA→Exposed 迁移 P3)。
 * 口径(R-ORD-11 2026-10 简化):业绩「已送达」= 已完成,按 completed_at(= 送达时间)归属。
 * ResultRow 取值全部在事务闭包内(聚合表达式 hashCode 依赖事务上下文)。
 */
@Repository
class StatisticsDeliveryWorkerRepositoryImpl(
  private val database: Database,
) : StatisticsDeliveryWorkerRepository {
  companion object {
    /** 送水工业绩「已送达」状态集合(R-ORD-11 送达即完成:仅已完成) */
    private val DELIVERED_STATUSES = listOf(OrderModelStatus.COMPLETED)
  }

  override fun countTodayActiveWorkers(sinceDateTime: LocalDateTime): Long {
    // 送水员档案并入 admins 单表:在岗送水员 = 具备配送能力的账号(role ∈ DELIVERY_WORKER/ADMIN)
    val deliveryCapableRoles = listOf(AdminRoleModel.DELIVERY_WORKER.name, AdminRoleModel.ADMIN.name)

    // 原子查询:窗口内有活跃订单的送水员 id(delivery_worker_id 非空去重)
    val activeWorkerIds =
      transaction(database) {
        OrdersTable
          .select(OrdersTable.deliveryWorkerId)
          .andWhere {
            (OrdersTable.createdAt greaterEq sinceDateTime) and
              OrdersTable.deliveryWorkerId.isNotNull()
          }.withDistinct()
          .toList()
      }.mapNotNull { it[OrdersTable.deliveryWorkerId] }

    if (activeWorkerIds.isEmpty()) return 0L

    return transaction(database) {
      AdminsTable
        .select(AdminsTable.id)
        .andWhere {
          AdminsTable.role inList deliveryCapableRoles and
            (AdminsTable.id inList activeWorkerIds)
        }.toList()
    }.size.toLong()
  }

  override fun countDeliveringOrders(): Long =
    transaction(database) {
      OrdersTable
        .select(OrdersTable.id)
        .andWhere { OrdersTable.status eq OrderModelStatus.DELIVERING.name }
        .count()
    }

  override fun countCompletedOrdersSince(dateTime: LocalDateTime): Long {
    // 今日完成/业绩口径(R-ORD-11):已完成,按 completed_at(送达时间)归属,送达即时计入
    val deliveredAt = OrdersTable.completedAt

    return transaction(database) {
      OrdersTable
        .select(OrdersTable.id)
        .andWhere {
          (OrdersTable.status inList DELIVERED_STATUSES.map { it.name }) and
            (deliveredAt greaterEq dateTime)
        }.count()
    }
  }

  override fun findCompletedOrderTimingsSince(dateTime: LocalDateTime): List<CompletedOrderTiming> {
    // 履约时长终点 = 送达时间(= completed_at,R-ORD-11 送达即完成)
    val deliveredAt = OrdersTable.completedAt

    return transaction(database) {
      OrdersTable
        .select(
          OrdersTable.id,
          deliveredAt,
          OrdersTable.deliveryStartedAt,
          OrdersTable.createdAt,
        ).andWhere {
          (OrdersTable.status inList DELIVERED_STATUSES.map { it.name }) and
            (deliveredAt greaterEq dateTime)
        }.toList()
        .map { row ->
          CompletedOrderTiming(
            orderId = row[OrdersTable.id],
            completedAt = row[deliveredAt]!!,
            deliveryStartedAt = row[OrdersTable.deliveryStartedAt],
            createdAt = row[OrdersTable.createdAt],
          )
        }
    }
  }

  override fun findEarliestAssignedAtByOrderIds(orderIds: List<Long>): Map<Long, LocalDateTime> {
    if (orderIds.isEmpty()) return emptyMap()

    // 派单时间 = orders.assigned_at(缺陷 #9 补列,V344 已从历史 DELIVERY_ASSIGNED 操作回填;
    // 新写入由 assignDeliveryWorker/batchAssignOrders/acceptDelivery 直接落列)
    return transaction(database) {
      OrdersTable
        .select(OrdersTable.id, OrdersTable.assignedAt)
        .where {
          (OrdersTable.id inList orderIds) and (OrdersTable.assignedAt.isNotNull())
        }.toList()
        .associate { row -> row[OrdersTable.id] to row[OrdersTable.assignedAt]!! }
    }
  }

  override fun countTodayCompletedOrdersByWorker(sinceDateTime: LocalDateTime): Map<Long, Int> {
    val deliveredAt = OrdersTable.completedAt
    val orderCount = OrdersTable.id.count()
    return transaction(database) {
      OrdersTable
        .select(OrdersTable.deliveryWorkerId, orderCount)
        .andWhere {
          (OrdersTable.status inList DELIVERED_STATUSES.map { it.name }) and
            (deliveredAt greaterEq sinceDateTime) and
            OrdersTable.deliveryWorkerId.isNotNull()
        }.groupBy(OrdersTable.deliveryWorkerId)
        .toList()
        .mapNotNull { row ->
          val workerId = row[OrdersTable.deliveryWorkerId] ?: return@mapNotNull null
          workerId to row[orderCount].toInt()
        }.toMap()
    }
  }
}
