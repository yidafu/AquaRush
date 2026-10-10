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

import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.domain.table.DeliveryAreasTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 配送区域仓储实现(JPA→Exposed 迁移 P4)。
 * 每个查询经入口 selectAll()/select() 自动过滤 is_deleted(语义对齐 @SoftDelete)。
 */
@Repository
class DeliveryAreaRepositoryImpl(
  private val database: Database,
) : DeliveryAreaRepository {
  private fun toModel(row: ResultRow): DeliveryAreaModel =
    DeliveryAreaModel(
      id = row[DeliveryAreasTable.id],
      name = row[DeliveryAreasTable.name],
      province = row[DeliveryAreasTable.province],
      city = row[DeliveryAreasTable.city],
      district = row[DeliveryAreasTable.district],
      enabled = row[DeliveryAreasTable.isEnabled],
      createdAt = row[DeliveryAreasTable.createdAt],
      updatedAt = row[DeliveryAreasTable.updatedAt],
      deletedAt = row[DeliveryAreasTable.deletedAt],
      deletedBy = row[DeliveryAreasTable.deletedBy],
    )

  override fun findByProvinceAndCityAndDistrict(
    province: String,
    city: String,
    district: String,
  ): DeliveryAreaModel? =
    transaction(database) {
      DeliveryAreasTable
        .selectAll()
        .andWhere {
          (DeliveryAreasTable.province eq province) and
            (DeliveryAreasTable.city eq city) and
            (DeliveryAreasTable.district eq district)
        }.limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByEnabledTrue(): List<DeliveryAreaModel> =
    transaction(database) {
      DeliveryAreasTable
        .selectAll()
        .andWhere { DeliveryAreasTable.isEnabled eq true }
        // 原 JPA 无序,固定 id 升序取稳定插入序
        .orderBy(DeliveryAreasTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findById(id: Long): Optional<DeliveryAreaModel> =
    Optional.ofNullable(
      transaction(database) {
        DeliveryAreasTable
          .selectAll()
          .andWhere { DeliveryAreasTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: DeliveryAreaModel): DeliveryAreaModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        DeliveryAreasTable
          .selectAllIncludingDeleted()
          .where { DeliveryAreasTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        DeliveryAreasTable.update({ DeliveryAreasTable.id eq id }) {
          it[name] = entity.name
          it[province] = entity.province
          it[city] = entity.city
          it[district] = entity.district
          it[isEnabled] = entity.enabled
          it[updatedAt] = now
        }
      } else {
        DeliveryAreasTable.insert {
          it[DeliveryAreasTable.id] = id
          it[name] = entity.name
          it[province] = entity.province
          it[city] = entity.city
          it[district] = entity.district
          it[isEnabled] = entity.enabled
          it[DeliveryAreasTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
