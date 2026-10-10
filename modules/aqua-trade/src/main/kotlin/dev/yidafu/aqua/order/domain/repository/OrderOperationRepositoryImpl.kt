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

import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.table.OrderOperationsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

/**
 * 订单操作流水仓储实现(JPA→Exposed 迁移 P4)。
 * order_operations 为裸表(无软删列,读侧不作 is_deleted 过滤,与原 JPA 一致)。
 */
@Repository
class OrderOperationRepositoryImpl(
  private val database: Database,
) : OrderOperationRepository {
  private fun toModel(row: ResultRow): OrderOperationModel =
    OrderOperationModel(
      id = row[OrderOperationsTable.id],
      orderId = row[OrderOperationsTable.orderId],
      operationType = OrderOperationType.fromValue(row[OrderOperationsTable.operationType]),
      operatorId = row[OrderOperationsTable.operatorId],
      operatorType =
        row[OrderOperationsTable.operatorType]?.let { OperatorType.fromValue(it) }
          ?: OperatorType.SYSTEM,
      description = row[OrderOperationsTable.description],
      extraData = row[OrderOperationsTable.extraData],
      createdAt = row[OrderOperationsTable.createdAt],
    )

  override fun findByOrderIdOrderByCreatedAtDesc(orderId: Long): List<OrderOperationModel> =
    transaction(database) {
      OrderOperationsTable
        .selectAll()
        .where { OrderOperationsTable.orderId eq orderId }
        .orderBy(OrderOperationsTable.createdAt to SortOrder.DESC, OrderOperationsTable.id to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun countByOrderId(orderId: Long): Long =
    transaction(database) {
      OrderOperationsTable
        .selectAll()
        .where { OrderOperationsTable.orderId eq orderId }
        .count()
    }

  override fun save(entity: OrderOperationModel): OrderOperationModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        OrderOperationsTable
          .selectAll()
          .where { OrderOperationsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        // 对齐原 JPA merge:业务列覆盖,createdAt 不可变(裸表无 updated_at 列)
        OrderOperationsTable.update({ OrderOperationsTable.id eq id }) {
          it[orderId] = entity.orderId
          it[operationType] = entity.operationType.name
          it[operatorId] = entity.operatorId
          it[operatorType] = entity.operatorType.name
          it[description] = entity.description
          it[extraData] = entity.extraData
        }
      } else {
        OrderOperationsTable.insert {
          it[OrderOperationsTable.id] = id
          it[orderId] = entity.orderId
          it[operationType] = entity.operationType.name
          it[operatorId] = entity.operatorId
          it[operatorType] = entity.operatorType.name
          it[description] = entity.description
          it[extraData] = entity.extraData
          it[OrderOperationsTable.createdAt] = entity.createdAt
        }
      }
      entity
    }
}
