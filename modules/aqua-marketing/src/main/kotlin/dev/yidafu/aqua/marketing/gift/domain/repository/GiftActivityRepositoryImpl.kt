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

package dev.yidafu.aqua.marketing.gift.domain.repository

import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.common.domain.table.GiftActivitiesTable
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
import org.jetbrains.exposed.v1.jdbc.andWhere
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
 * 买赠/满赠活动仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class GiftActivityRepositoryImpl(
  private val database: Database,
) : GiftActivityRepository {
  private fun toModel(row: ResultRow): GiftActivityModel =
    GiftActivityModel(
      id = row[GiftActivitiesTable.id],
      name = row[GiftActivitiesTable.name],
      giftType = GiftActivityType.fromValue(row[GiftActivitiesTable.giftType]),
      thresholdCents = row[GiftActivitiesTable.thresholdCents],
      thresholdQuantity = row[GiftActivitiesTable.thresholdQuantity],
      scope = PromotionScope.fromValue(row[GiftActivitiesTable.scope]),
      productId = row[GiftActivitiesTable.productId],
      customerGroupId = row[GiftActivitiesTable.customerGroupId],
      giftProductId = row[GiftActivitiesTable.giftProductId],
      giftQuantity = row[GiftActivitiesTable.giftQuantity],
      validFrom = row[GiftActivitiesTable.validFrom],
      validTo = row[GiftActivitiesTable.validTo],
      status = PromotionActivityStatus.fromValue(row[GiftActivitiesTable.status]),
      createdAt = row[GiftActivitiesTable.createdAt],
      updatedAt = row[GiftActivitiesTable.updatedAt],
      deletedAt = row[GiftActivitiesTable.deletedAt],
      deletedBy = row[GiftActivitiesTable.deletedBy],
    )

  override fun findAllById(ids: List<Long>): List<GiftActivityModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      GiftActivitiesTable
        .selectAll()
        .andWhere { GiftActivitiesTable.id inList ids }
        .orderBy(GiftActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun findById(id: Long): Optional<GiftActivityModel> =
    Optional.ofNullable(
      transaction(database) {
        GiftActivitiesTable
          .selectAll()
          .andWhere { GiftActivitiesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun searchActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<GiftActivityModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (GiftActivitiesTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    val (content, total) =
      transaction(database) {
        val content =
          GiftActivitiesTable
            .selectAll()
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(GiftActivitiesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to GiftActivitiesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: GiftActivityModel): GiftActivityModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        GiftActivitiesTable
          .selectAllIncludingDeleted()
          .where { GiftActivitiesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        GiftActivitiesTable.update({ GiftActivitiesTable.id eq id }) {
          it[name] = entity.name
          it[giftType] = entity.giftType.name
          it[thresholdCents] = entity.thresholdCents
          it[thresholdQuantity] = entity.thresholdQuantity
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[customerGroupId] = entity.customerGroupId
          it[giftProductId] = entity.giftProductId
          it[giftQuantity] = entity.giftQuantity
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[updatedAt] = now
        }
      } else {
        GiftActivitiesTable.insert {
          it[GiftActivitiesTable.id] = id
          it[name] = entity.name
          it[giftType] = entity.giftType.name
          it[thresholdCents] = entity.thresholdCents
          it[thresholdQuantity] = entity.thresholdQuantity
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[customerGroupId] = entity.customerGroupId
          it[giftProductId] = entity.giftProductId
          it[giftQuantity] = entity.giftQuantity
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[GiftActivitiesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findActiveActivities(
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): List<GiftActivityModel> =
    transaction(database) {
      GiftActivitiesTable
        .selectAll()
        .andWhere {
          (GiftActivitiesTable.status eq status.name) and
            (GiftActivitiesTable.validFrom lessEq now) and
            (GiftActivitiesTable.validTo greaterEq now)
        }.orderBy(GiftActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
}
