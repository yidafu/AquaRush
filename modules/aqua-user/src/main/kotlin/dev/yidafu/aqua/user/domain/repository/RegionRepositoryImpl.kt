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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.domain.table.RegionsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/** 行政区划仓储实现(JPA→Exposed 迁移 P3 收尾)。 */
@Repository
class RegionRepositoryImpl(
  private val database: Database,
) : RegionRepository {
  private fun toModel(row: ResultRow): RegionModel =
    RegionModel(
      id = row[RegionsTable.id],
      name = row[RegionsTable.name],
      code = row[RegionsTable.code],
      parentCode = row[RegionsTable.parentCode],
      level = row[RegionsTable.level],
      createdAt = row[RegionsTable.createdAt],
      updatedAt = row[RegionsTable.updatedAt],
    )

  private fun selectAllWhere(condition: () -> Op<Boolean>) = RegionsTable.selectAll().where { RegionsTable.activeOnly(condition) }

  override fun findRootRegions(level: Int): List<RegionModel> =
    transaction(database) {
      selectAllWhere { (RegionsTable.parentCode eq "0") and (RegionsTable.level eq level) }.toList()
    }.map(::toModel)

  override fun existsByNameAndLevelAndParentCode(
    name: String,
    level: Int,
    parentCode: String,
  ): Boolean =
    transaction(database) {
      selectAllWhere {
        (RegionsTable.name eq name) and (RegionsTable.level eq level) and (RegionsTable.parentCode eq parentCode)
      }.limit(1).any()
    }

  override fun findByCode(code: String): RegionModel? =
    transaction(database) {
      selectAllWhere { RegionsTable.code eq code }.limit(1).firstOrNull()
    }?.let(::toModel)

  override fun findByParentCodeAndLevel(
    parentCode: String,
    level: Int,
  ): List<RegionModel> =
    transaction(database) {
      selectAllWhere { (RegionsTable.parentCode eq parentCode) and (RegionsTable.level eq level) }
        .orderBy(RegionsTable.code to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByLevel(level: Int): List<RegionModel> =
    transaction(database) {
      selectAllWhere { RegionsTable.level eq level }.toList()
    }.map(::toModel)

  override fun existsByCode(code: String): Boolean = findByCode(code) != null

  override fun existsByCodeAndLevel(
    code: String,
    level: Int,
  ): Boolean =
    transaction(database) {
      selectAllWhere { (RegionsTable.code eq code) and (RegionsTable.level eq level) }.limit(1).any()
    }

  override fun findByNameContainingAndLevelOrderByCode(
    name: String,
    level: Int,
  ): List<RegionModel> =
    transaction(database) {
      selectAllWhere { (RegionsTable.name like "%$name%") and (RegionsTable.level eq level) }
        .orderBy(RegionsTable.code to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByNameContainingOrderByCode(name: String): List<RegionModel> =
    transaction(database) {
      selectAllWhere { RegionsTable.name like "%$name%" }
        .orderBy(RegionsTable.code to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByParentCodeOrderByCode(parentCode: String): List<RegionModel> =
    transaction(database) {
      selectAllWhere { RegionsTable.parentCode eq parentCode }
        .orderBy(RegionsTable.code to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByLevelOrderByCode(level: Int): List<RegionModel> =
    transaction(database) {
      selectAllWhere { RegionsTable.level eq level }
        .orderBy(RegionsTable.code to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findById(id: Long): RegionModel? =
    transaction(database) {
      selectAllWhere { RegionsTable.id eq id }.limit(1).firstOrNull()
    }?.let(::toModel)

  override fun save(entity: RegionModel): RegionModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        RegionsTable
          .selectAll()
          .where { RegionsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        RegionsTable.update({ RegionsTable.id eq id }) {
          it[name] = entity.name
          it[RegionsTable.code] = entity.code
          it[parentCode] = entity.parentCode
          it[level] = entity.level
          it[updatedAt] = now
        }
      } else {
        RegionsTable.insert {
          it[RegionsTable.id] = id
          it[RegionsTable.name] = entity.name
          it[RegionsTable.code] = entity.code
          it[RegionsTable.parentCode] = entity.parentCode
          it[RegionsTable.level] = entity.level
          it[RegionsTable.createdAt] = entity.createdAt
          it[RegionsTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun delete(entity: RegionModel) {
    val id = requireNotNull(entity.id) { "RegionModel.id 不能为空(软删必须携带主键)" }
    transaction(database) {
      val now = LocalDateTime.now()
      RegionsTable.update({ RegionsTable.id eq id }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }
}
