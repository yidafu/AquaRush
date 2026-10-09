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

package dev.yidafu.aqua.marketing.promotion.domain.repository

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.table.PromotionActivitiesTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 满减/折扣活动仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class PromotionActivityRepositoryImpl(
  private val database: Database,
) : PromotionActivityRepository {
  private fun toModel(row: ResultRow): PromotionActivityModel =
    PromotionActivityModel(
      id = row[PromotionActivitiesTable.id],
      name = row[PromotionActivitiesTable.name],
      activityType =
        dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
          .valueOf(row[PromotionActivitiesTable.activityType]),
      scope =
        dev.yidafu.aqua.common.domain.model.enums.PromotionScope
          .valueOf(row[PromotionActivitiesTable.scope]),
      productId = row[PromotionActivitiesTable.productId],
      customerGroupId = row[PromotionActivitiesTable.customerGroupId],
      validFrom = row[PromotionActivitiesTable.validFrom],
      validTo = row[PromotionActivitiesTable.validTo],
      status = PromotionActivityStatus.valueOf(row[PromotionActivitiesTable.status]),
      createdAt = row[PromotionActivitiesTable.createdAt],
      updatedAt = row[PromotionActivitiesTable.updatedAt],
      deletedAt = row[PromotionActivitiesTable.deletedAt],
      deletedBy = row[PromotionActivitiesTable.deletedBy],
    )

  override fun findAll(): List<PromotionActivityModel> =
    transaction(database) {
      PromotionActivitiesTable
        .selectAll()
        .where { PromotionActivitiesTable.activeOnly() }
        .orderBy(PromotionActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findAllById(ids: List<Long>): List<PromotionActivityModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      PromotionActivitiesTable
        .selectAll()
        .where { PromotionActivitiesTable.activeOnly { PromotionActivitiesTable.id inList ids } }
        .orderBy(PromotionActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun findById(id: Long): Optional<PromotionActivityModel> =
    Optional.ofNullable(
      transaction(database) {
        PromotionActivitiesTable
          .selectAll()
          .where { PromotionActivitiesTable.activeOnly { PromotionActivitiesTable.id eq id } }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun searchActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<PromotionActivityModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (PromotionActivitiesTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    val (content, total) =
      transaction(database) {
        val content =
          PromotionActivitiesTable
            .selectAll()
            .where { PromotionActivitiesTable.activeOnly { condition } }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(PromotionActivitiesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to PromotionActivitiesTable.selectAll().where { PromotionActivitiesTable.activeOnly { condition } }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: PromotionActivityModel): PromotionActivityModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        PromotionActivitiesTable
          .selectAll()
          .where { PromotionActivitiesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        PromotionActivitiesTable.update({ PromotionActivitiesTable.id eq id }) {
          it[name] = entity.name
          it[activityType] = entity.activityType.name
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[customerGroupId] = entity.customerGroupId
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[updatedAt] = now
        }
      } else {
        PromotionActivitiesTable.insert {
          it[PromotionActivitiesTable.id] = id
          it[name] = entity.name
          it[activityType] = entity.activityType.name
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[customerGroupId] = entity.customerGroupId
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[PromotionActivitiesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  // ===== PromotionActivityRepositoryCustom =====

  override fun findActiveActivities(
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): List<PromotionActivityModel> =
    transaction(database) {
      PromotionActivitiesTable
        .selectAll()
        .where {
          PromotionActivitiesTable.activeOnly {
            (PromotionActivitiesTable.status eq status.name) and
              (PromotionActivitiesTable.validFrom lessEq now) and
              (PromotionActivitiesTable.validTo greaterEq now)
          }
        }.orderBy(PromotionActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
}
