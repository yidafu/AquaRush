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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.common.domain.table.DeliveryTrajectoriesTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 配送轨迹仓储实现(JPA→Exposed 迁移 P4)。
 * 轨迹表为裸 Table(无软删列),查询不带 is_deleted 过滤;
 * 排序在原 JPA OrderBy 基础上追加 id 破平(同秒上报不乱序)。
 * saveAll 整个批次包在单个 [transaction] 内(原子:全成或全败)。
 */
@Repository
class DeliveryTrajectoryRepositoryImpl(
  private val database: Database,
) : DeliveryTrajectoryRepository {
  private fun toModel(row: ResultRow): DeliveryTrajectoryModel =
    DeliveryTrajectoryModel(
      id = row[DeliveryTrajectoriesTable.id],
      deliveryWorkerId = row[DeliveryTrajectoriesTable.deliveryWorkerId],
      orderId = row[DeliveryTrajectoriesTable.orderId],
      longitude = row[DeliveryTrajectoriesTable.longitude],
      latitude = row[DeliveryTrajectoriesTable.latitude],
      accuracy = row[DeliveryTrajectoriesTable.accuracy],
      reportedAt = row[DeliveryTrajectoriesTable.reportedAt],
      createdAt = row[DeliveryTrajectoriesTable.createdAt],
    )

  override fun findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(
    deliveryWorkerId: Long,
    orderId: Long,
  ): List<DeliveryTrajectoryModel> =
    transaction(database) {
      DeliveryTrajectoriesTable
        .selectAll()
        .where {
          (DeliveryTrajectoriesTable.deliveryWorkerId eq deliveryWorkerId) and
            (DeliveryTrajectoriesTable.orderId eq orderId)
        }.orderBy(
          DeliveryTrajectoriesTable.reportedAt to SortOrder.ASC,
          DeliveryTrajectoriesTable.id to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(
    deliveryWorkerId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<DeliveryTrajectoryModel> =
    transaction(database) {
      DeliveryTrajectoriesTable
        .selectAll()
        .where {
          (DeliveryTrajectoriesTable.deliveryWorkerId eq deliveryWorkerId) and
            (DeliveryTrajectoriesTable.reportedAt greaterEq start) and
            (DeliveryTrajectoriesTable.reportedAt lessEq end)
        }.orderBy(
          DeliveryTrajectoriesTable.reportedAt to SortOrder.ASC,
          DeliveryTrajectoriesTable.id to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByDeliveryWorkerIdOrderByReportedAtAsc(deliveryWorkerId: Long): List<DeliveryTrajectoryModel> =
    transaction(database) {
      DeliveryTrajectoriesTable
        .selectAll()
        .where { DeliveryTrajectoriesTable.deliveryWorkerId eq deliveryWorkerId }
        .orderBy(
          DeliveryTrajectoriesTable.reportedAt to SortOrder.ASC,
          DeliveryTrajectoriesTable.id to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findFirstByDeliveryWorkerIdOrderByReportedAtDesc(deliveryWorkerId: Long): DeliveryTrajectoryModel? =
    transaction(database) {
      DeliveryTrajectoriesTable
        .selectAll()
        .where { DeliveryTrajectoriesTable.deliveryWorkerId eq deliveryWorkerId }
        .orderBy(
          DeliveryTrajectoriesTable.reportedAt to SortOrder.DESC,
          DeliveryTrajectoriesTable.id to SortOrder.DESC,
        ).limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun save(entity: DeliveryTrajectoryModel): DeliveryTrajectoryModel = transaction(database) { upsert(entity) }

  override fun saveAll(entities: List<DeliveryTrajectoryModel>): List<DeliveryTrajectoryModel> =
    // 单事务原子:全部实体在同一个 transaction 内 upsert(语义与 KDoc 一致)
    transaction(database) { entities.map { upsert(it) } }

  /** 必须在 [transaction] 内调用。 */
  private fun upsert(entity: DeliveryTrajectoryModel): DeliveryTrajectoryModel {
    val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
    val exists =
      DeliveryTrajectoriesTable
        .selectAll()
        .where { DeliveryTrajectoriesTable.id eq id }
        .limit(1)
        .any()
    if (exists) {
      DeliveryTrajectoriesTable.update({ DeliveryTrajectoriesTable.id eq id }) {
        it[deliveryWorkerId] = entity.deliveryWorkerId
        it[orderId] = entity.orderId
        it[longitude] = entity.longitude
        it[latitude] = entity.latitude
        it[accuracy] = entity.accuracy
        it[reportedAt] = entity.reportedAt
      }
    } else {
      DeliveryTrajectoriesTable.insert {
        it[DeliveryTrajectoriesTable.id] = id
        it[deliveryWorkerId] = entity.deliveryWorkerId
        it[orderId] = entity.orderId
        it[longitude] = entity.longitude
        it[latitude] = entity.latitude
        it[accuracy] = entity.accuracy
        it[reportedAt] = entity.reportedAt
        it[createdAt] = entity.createdAt
      }
    }
    return entity
  }
}
