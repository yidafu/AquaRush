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

import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 订单商品明细仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;saveAll = 单事务批量 upsert(全部成功或全部回滚)。
 */
@Repository
class OrderItemRepositoryImpl(
  private val database: Database,
) : OrderItemRepository {
  private fun toModel(row: ResultRow): OrderItemModel =
    OrderItemModel(
      id = row[OrderItemsTable.id],
      orderId = row[OrderItemsTable.orderId],
      productId = row[OrderItemsTable.productId],
      quantity = row[OrderItemsTable.quantity],
      unitPriceCents = row[OrderItemsTable.unitPriceCents],
      totalPriceCents = row[OrderItemsTable.totalPriceCents],
      productSnapshot = row[OrderItemsTable.productSnapshot],
      createdAt = row[OrderItemsTable.createdAt],
      updatedAt = row[OrderItemsTable.updatedAt],
      deletedAt = row[OrderItemsTable.deletedAt],
      deletedBy = row[OrderItemsTable.deletedBy],
    )

  override fun findByOrderId(orderId: Long): List<OrderItemModel> =
    transaction(database) {
      OrderItemsTable
        .selectAll()
        .where { OrderItemsTable.activeOnly { OrderItemsTable.orderId eq orderId } }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrderItemsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByProductId(productId: Long): List<OrderItemModel> =
    transaction(database) {
      OrderItemsTable
        .selectAll()
        .where { OrderItemsTable.activeOnly { OrderItemsTable.productId eq productId } }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrderItemsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun saveAll(entities: List<OrderItemModel>): List<OrderItemModel> =
    transaction(database) {
      entities.map { entity ->
        val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
        val exists =
          OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.id eq id }
            .limit(1)
            .any()
        if (exists) {
          // 对齐原 JPA merge + @PreUpdate:业务列覆盖,createdAt 不可变,updatedAt=now
          OrderItemsTable.update({ OrderItemsTable.id eq id }) {
            it[orderId] = entity.orderId
            it[productId] = entity.productId
            it[quantity] = entity.quantity
            it[unitPriceCents] = entity.unitPriceCents
            it[totalPriceCents] = entity.totalPriceCents
            it[productSnapshot] = entity.productSnapshot
            it[updatedAt] = LocalDateTime.now()
          }
        } else {
          OrderItemsTable.insert {
            it[OrderItemsTable.id] = id
            it[orderId] = entity.orderId
            it[productId] = entity.productId
            it[quantity] = entity.quantity
            it[unitPriceCents] = entity.unitPriceCents
            it[totalPriceCents] = entity.totalPriceCents
            it[productSnapshot] = entity.productSnapshot
            it[OrderItemsTable.createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
            it[deletedBy] = entity.deletedBy
          }
        }
        entity
      }
    }
}
