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

package dev.yidafu.aqua.marketing.firstorder.domain.repository

import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.common.domain.table.FirstOrderActivitiesTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
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
 * 首单立减/新人专享价活动仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class FirstOrderActivityRepositoryImpl(
  private val database: Database,
) : FirstOrderActivityRepository {
  private fun toModel(row: ResultRow): FirstOrderActivityModel =
    FirstOrderActivityModel(
      id = row[FirstOrderActivitiesTable.id],
      name = row[FirstOrderActivitiesTable.name],
      activityType = FirstOrderActivityType.fromValue(row[FirstOrderActivitiesTable.activityType]),
      thresholdCents = row[FirstOrderActivitiesTable.thresholdCents],
      discountAmountCents = row[FirstOrderActivitiesTable.discountAmountCents],
      discountRatePermille = row[FirstOrderActivitiesTable.discountRatePermille],
      scope = PromotionScope.fromValue(row[FirstOrderActivitiesTable.scope]),
      productId = row[FirstOrderActivitiesTable.productId],
      newcomerPriceCents = row[FirstOrderActivitiesTable.newcomerPriceCents],
      validFrom = row[FirstOrderActivitiesTable.validFrom],
      validTo = row[FirstOrderActivitiesTable.validTo],
      status = PromotionActivityStatus.fromValue(row[FirstOrderActivitiesTable.status]),
      createdAt = row[FirstOrderActivitiesTable.createdAt],
      updatedAt = row[FirstOrderActivitiesTable.updatedAt],
      deletedAt = row[FirstOrderActivitiesTable.deletedAt],
      deletedBy = row[FirstOrderActivitiesTable.deletedBy],
    )

  override fun findAllById(ids: List<Long>): List<FirstOrderActivityModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      FirstOrderActivitiesTable
        .selectAll()
        .andWhere { FirstOrderActivitiesTable.id inList ids }
        .orderBy(FirstOrderActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun findById(id: Long): Optional<FirstOrderActivityModel> =
    Optional.ofNullable(
      transaction(database) {
        FirstOrderActivitiesTable
          .selectAll()
          .andWhere { FirstOrderActivitiesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun searchActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<FirstOrderActivityModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (FirstOrderActivitiesTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    val (content, total) =
      transaction(database) {
        val content =
          FirstOrderActivitiesTable
            .selectAll()
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(FirstOrderActivitiesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to FirstOrderActivitiesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: FirstOrderActivityModel): FirstOrderActivityModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        FirstOrderActivitiesTable
          .selectAllIncludingDeleted()
          .where { FirstOrderActivitiesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        FirstOrderActivitiesTable.update({ FirstOrderActivitiesTable.id eq id }) {
          it[name] = entity.name
          it[activityType] = entity.activityType.name
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[newcomerPriceCents] = entity.newcomerPriceCents
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[updatedAt] = now
        }
      } else {
        FirstOrderActivitiesTable.insert {
          it[FirstOrderActivitiesTable.id] = id
          it[name] = entity.name
          it[activityType] = entity.activityType.name
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[newcomerPriceCents] = entity.newcomerPriceCents
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[FirstOrderActivitiesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findActiveActivities(
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): List<FirstOrderActivityModel> =
    transaction(database) {
      FirstOrderActivitiesTable
        .selectAll()
        .andWhere {
          (FirstOrderActivitiesTable.status eq status.name) and
            (FirstOrderActivitiesTable.validFrom lessEq now) and
            (FirstOrderActivitiesTable.validTo greaterEq now)
        }.orderBy(FirstOrderActivitiesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findActiveNewcomerPriceByProductId(
    productId: Long,
    activityType: FirstOrderActivityType,
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): FirstOrderActivityModel? =
    transaction(database) {
      FirstOrderActivitiesTable
        .selectAll()
        .andWhere {
          (FirstOrderActivitiesTable.productId eq productId) and
            (FirstOrderActivitiesTable.activityType eq activityType.name) and
            (FirstOrderActivitiesTable.status eq status.name) and
            (FirstOrderActivitiesTable.validFrom lessEq now) and
            (FirstOrderActivitiesTable.validTo greaterEq now)
        }.orderBy(
          FirstOrderActivitiesTable.validFrom to SortOrder.DESC,
          FirstOrderActivitiesTable.id to SortOrder.DESC,
        ).limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsOverlap(
    productId: Long,
    activityType: FirstOrderActivityType,
    status: PromotionActivityStatus,
    newValidFrom: LocalDateTime,
    newValidTo: LocalDateTime,
    excludeId: Long?,
  ): Boolean =
    transaction(database) {
      FirstOrderActivitiesTable
        .selectAll()
        .andWhere {
          (FirstOrderActivitiesTable.productId eq productId) and
            (FirstOrderActivitiesTable.activityType eq activityType.name) and
            (FirstOrderActivitiesTable.status eq status.name) and
            (FirstOrderActivitiesTable.validFrom less newValidTo) and
            (FirstOrderActivitiesTable.validTo greater newValidFrom) and
            (if (excludeId != null) FirstOrderActivitiesTable.id neq excludeId else Op.TRUE)
        }.limit(1)
        .any()
    }
}
