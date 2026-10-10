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

import dev.yidafu.aqua.common.domain.model.BarrelRecoveryRecordModel
import dev.yidafu.aqua.common.domain.table.BarrelRecoveryRecordsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

/**
 * 空桶回收登记仓储实现(F-ORD-011)。查询过滤 is_deleted。
 */
@Repository
class BarrelRecoveryRecordRepositoryImpl(
  private val database: Database,
) : BarrelRecoveryRecordRepository {
  private fun toModel(row: ResultRow): BarrelRecoveryRecordModel =
    BarrelRecoveryRecordModel(
      id = row[BarrelRecoveryRecordsTable.id],
      workerId = row[BarrelRecoveryRecordsTable.workerId],
      orderId = row[BarrelRecoveryRecordsTable.orderId],
      specification = row[BarrelRecoveryRecordsTable.specification],
      bucketCount = row[BarrelRecoveryRecordsTable.bucketCount],
      createdAt = row[BarrelRecoveryRecordsTable.createdAt],
      updatedAt = row[BarrelRecoveryRecordsTable.updatedAt],
      deletedAt = row[BarrelRecoveryRecordsTable.deletedAt],
      deletedBy = row[BarrelRecoveryRecordsTable.deletedBy],
    )

  override fun save(entity: BarrelRecoveryRecordModel): BarrelRecoveryRecordModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      BarrelRecoveryRecordsTable.insert {
        it[BarrelRecoveryRecordsTable.id] = id
        it[workerId] = entity.workerId
        it[orderId] = entity.orderId
        it[specification] = entity.specification
        it[bucketCount] = entity.bucketCount
        it[BarrelRecoveryRecordsTable.createdAt] = entity.createdAt
        it[updatedAt] = entity.updatedAt
      }
      entity
    }

  override fun listByOrderId(orderId: Long): List<BarrelRecoveryRecordModel> =
    transaction(database) {
      BarrelRecoveryRecordsTable
        .selectAll()
        .andWhere { BarrelRecoveryRecordsTable.orderId eq orderId }
        .orderBy(BarrelRecoveryRecordsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
}
