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

import dev.yidafu.aqua.common.domain.model.OrderGiftModel
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
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

/**
 * 订单赠品行仓储实现(JPA→Exposed 迁移 P4)。
 * order_gifts 为裸表(无软删列,读侧不作 is_deleted 过滤,与原 JPA 一致);
 * saveAll = 单事务批量 upsert(全部成功或全部回滚)。
 */
@Repository
class OrderGiftRepositoryImpl(
  private val database: Database,
) : OrderGiftRepository {
  private fun toModel(row: ResultRow): OrderGiftModel =
    OrderGiftModel(
      id = row[OrderGiftsTable.id],
      orderId = row[OrderGiftsTable.orderId],
      giftActivityId = row[OrderGiftsTable.giftActivityId],
      giftActivityName = row[OrderGiftsTable.giftActivityName],
      productId = row[OrderGiftsTable.productId],
      productName = row[OrderGiftsTable.productName],
      quantity = row[OrderGiftsTable.quantity],
      createdAt = row[OrderGiftsTable.createdAt],
    )

  override fun findByOrderId(orderId: Long): List<OrderGiftModel> =
    transaction(database) {
      OrderGiftsTable
        .selectAll()
        .where { OrderGiftsTable.orderId eq orderId }
        // 原 JPA 无序,按 id 升序破平
        .orderBy(OrderGiftsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun saveAll(entities: List<OrderGiftModel>): List<OrderGiftModel> =
    transaction(database) {
      entities.map { entity ->
        val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
        val exists =
          OrderGiftsTable
            .selectAll()
            .where { OrderGiftsTable.id eq id }
            .limit(1)
            .any()
        if (exists) {
          // 对齐原 JPA merge:业务列覆盖,createdAt 不可变(裸表无 updated_at 列)
          OrderGiftsTable.update({ OrderGiftsTable.id eq id }) {
            it[orderId] = entity.orderId
            it[giftActivityId] = entity.giftActivityId
            it[giftActivityName] = entity.giftActivityName
            it[productId] = entity.productId
            it[productName] = entity.productName
            it[quantity] = entity.quantity
          }
        } else {
          OrderGiftsTable.insert {
            it[OrderGiftsTable.id] = id
            it[orderId] = entity.orderId
            it[giftActivityId] = entity.giftActivityId
            it[giftActivityName] = entity.giftActivityName
            it[productId] = entity.productId
            it[productName] = entity.productName
            it[quantity] = entity.quantity
            it[OrderGiftsTable.createdAt] = entity.createdAt
          }
        }
        entity
      }
    }
}
