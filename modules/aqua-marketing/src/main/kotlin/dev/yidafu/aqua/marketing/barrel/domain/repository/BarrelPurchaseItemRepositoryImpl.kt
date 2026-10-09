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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.table.BarrelPurchaseItemsTable
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
 * 购桶单明细仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class BarrelPurchaseItemRepositoryImpl(
  private val database: Database,
) : BarrelPurchaseItemRepository {
  private fun toModel(row: ResultRow): BarrelPurchaseItemModel =
    BarrelPurchaseItemModel(
      id = row[BarrelPurchaseItemsTable.id],
      purchaseId = row[BarrelPurchaseItemsTable.purchaseId],
      barrelFeeId = row[BarrelPurchaseItemsTable.barrelFeeId],
      specification = row[BarrelPurchaseItemsTable.specification],
      unitPriceCents = row[BarrelPurchaseItemsTable.unitPriceCents],
      quantity = row[BarrelPurchaseItemsTable.quantity],
      amountCents = row[BarrelPurchaseItemsTable.amountCents],
      createdAt = row[BarrelPurchaseItemsTable.createdAt],
      updatedAt = row[BarrelPurchaseItemsTable.updatedAt],
      deletedAt = row[BarrelPurchaseItemsTable.deletedAt],
      deletedBy = row[BarrelPurchaseItemsTable.deletedBy],
    )

  private fun upsert(entity: BarrelPurchaseItemModel): BarrelPurchaseItemModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        BarrelPurchaseItemsTable
          .selectAll()
          .where { BarrelPurchaseItemsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        BarrelPurchaseItemsTable.update({ BarrelPurchaseItemsTable.id eq id }) {
          it[purchaseId] = entity.purchaseId
          it[barrelFeeId] = entity.barrelFeeId
          it[specification] = entity.specification
          it[unitPriceCents] = entity.unitPriceCents
          it[quantity] = entity.quantity
          it[amountCents] = entity.amountCents
          it[updatedAt] = now
        }
      } else {
        BarrelPurchaseItemsTable.insert {
          it[BarrelPurchaseItemsTable.id] = id
          it[purchaseId] = entity.purchaseId
          it[barrelFeeId] = entity.barrelFeeId
          it[specification] = entity.specification
          it[unitPriceCents] = entity.unitPriceCents
          it[quantity] = entity.quantity
          it[amountCents] = entity.amountCents
          it[BarrelPurchaseItemsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findAllByPurchaseIdOrderByIdAsc(purchaseId: Long): List<BarrelPurchaseItemModel> =
    transaction(database) {
      BarrelPurchaseItemsTable
        .selectAll()
        .where { BarrelPurchaseItemsTable.activeOnly { BarrelPurchaseItemsTable.purchaseId eq purchaseId } }
        .orderBy(BarrelPurchaseItemsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun save(entity: BarrelPurchaseItemModel): BarrelPurchaseItemModel = upsert(entity)

  override fun saveAll(entities: List<BarrelPurchaseItemModel>): List<BarrelPurchaseItemModel> = entities.map { upsert(it) }
}
