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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QAdminModel
import dev.yidafu.aqua.common.domain.model.QOrderModel
import dev.yidafu.aqua.common.domain.model.QOrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StatisticsDeliveryWorkerRepositoryImpl : StatisticsDeliveryWorkerRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun countTodayActiveWorkers(sinceDateTime: LocalDateTime): Long {
    // 送水员档案并入 admins 单表:在岗送水员 = 具备配送能力的账号(role ∈ DELIVERY_WORKER/ADMIN)
    val adminModel = QAdminModel.adminModel
    val orderModel = QOrderModel.orderModel
    val deliveryCapableRoles = listOf(AdminRoleModel.DELIVERY_WORKER, AdminRoleModel.ADMIN)

    return queryFactory
      .select(adminModel.id)
      .from(adminModel)
      .where(
        adminModel.role.`in`(deliveryCapableRoles),
        adminModel.id.`in`(
          queryFactory
            .select(orderModel.deliveryWorkerId)
            .from(orderModel)
            .where(
              orderModel.createdAt.goe(sinceDateTime),
              orderModel.deliveryWorkerId.isNotNull,
            ).distinct(),
        ),
      ).fetchCount()
  }

  override fun countDeliveringOrders(): Long {
    val orderModel = QOrderModel.orderModel

    return queryFactory
      .query()
      .from(orderModel)
      .where(orderModel.status.eq(OrderModelStatus.DELIVERING))
      .fetchCount()
  }

  override fun countCompletedOrdersSince(dateTime: LocalDateTime): Long {
    val orderModel = QOrderModel.orderModel

    return queryFactory
      .query()
      .from(orderModel)
      .where(
        orderModel.status.eq(OrderModelStatus.COMPLETED),
        orderModel.updatedAt.goe(dateTime),
      ).fetchCount()
  }

  override fun findCompletedOrderTimingsSince(dateTime: LocalDateTime): List<CompletedOrderTiming> {
    val orderModel = QOrderModel.orderModel

    return queryFactory
      .select(
        orderModel.id,
        orderModel.completedAt,
        orderModel.deliveryStartedAt,
        orderModel.createdAt,
      ).from(orderModel)
      .where(
        orderModel.status.eq(OrderModelStatus.COMPLETED),
        orderModel.completedAt.goe(dateTime),
      ).fetch()
      .map { row ->
        CompletedOrderTiming(
          orderId = row.get(orderModel.id)!!,
          completedAt = row.get(orderModel.completedAt)!!,
          deliveryStartedAt = row.get(orderModel.deliveryStartedAt),
          createdAt = row.get(orderModel.createdAt)!!,
        )
      }
  }

  override fun findEarliestAssignedAtByOrderIds(orderIds: List<Long>): Map<Long, LocalDateTime> {
    if (orderIds.isEmpty()) return emptyMap()
    val orderOperationModel = QOrderOperationModel.orderOperationModel

    // 派单时间 = 各订单最早 DELIVERY_ASSIGNED 操作时间(管理员派单/送水员接单均记录该操作)
    val createdAtExpr = orderOperationModel.createdAt.min()
    return queryFactory
      .select(orderOperationModel.orderId, createdAtExpr)
      .from(orderOperationModel)
      .where(
        orderOperationModel.orderId.`in`(orderIds),
        orderOperationModel.operationType.eq(OrderOperationType.DELIVERY_ASSIGNED),
      ).groupBy(orderOperationModel.orderId)
      .fetch()
      .associate { row ->
        row.get(orderOperationModel.orderId)!! to row.get(createdAtExpr)!!
      }
  }
}
