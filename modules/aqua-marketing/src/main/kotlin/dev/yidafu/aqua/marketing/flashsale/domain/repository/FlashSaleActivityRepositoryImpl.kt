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

package dev.yidafu.aqua.marketing.flashsale.domain.repository

import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.table.FlashSaleActivitiesTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
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
 * 限时特价活动仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class FlashSaleActivityRepositoryImpl(
  private val database: Database,
) : FlashSaleActivityRepository {
  private fun toModel(row: ResultRow): FlashSaleActivityModel =
    FlashSaleActivityModel(
      id = row[FlashSaleActivitiesTable.id],
      name = row[FlashSaleActivitiesTable.name],
      productId = row[FlashSaleActivitiesTable.productId],
      salePriceCents = row[FlashSaleActivitiesTable.salePriceCents],
      validFrom = row[FlashSaleActivitiesTable.validFrom],
      validTo = row[FlashSaleActivitiesTable.validTo],
      status = PromotionActivityStatus.valueOf(row[FlashSaleActivitiesTable.status]),
      createdAt = row[FlashSaleActivitiesTable.createdAt],
      updatedAt = row[FlashSaleActivitiesTable.updatedAt],
      deletedAt = row[FlashSaleActivitiesTable.deletedAt],
      deletedBy = row[FlashSaleActivitiesTable.deletedBy],
    )

  override fun findById(id: Long): Optional<FlashSaleActivityModel> =
    Optional.ofNullable(
      transaction(database) {
        FlashSaleActivitiesTable
          .selectAll()
          .andWhere { FlashSaleActivitiesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun searchActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<FlashSaleActivityModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (FlashSaleActivitiesTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    val (content, total) =
      transaction(database) {
        val content =
          FlashSaleActivitiesTable
            .selectAll()
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(FlashSaleActivitiesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to FlashSaleActivitiesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: FlashSaleActivityModel): FlashSaleActivityModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        FlashSaleActivitiesTable
          .selectAllIncludingDeleted()
          .where { FlashSaleActivitiesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        FlashSaleActivitiesTable.update({ FlashSaleActivitiesTable.id eq id }) {
          it[name] = entity.name
          it[productId] = entity.productId
          it[salePriceCents] = entity.salePriceCents
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[updatedAt] = now
        }
      } else {
        FlashSaleActivitiesTable.insert {
          it[FlashSaleActivitiesTable.id] = id
          it[name] = entity.name
          it[productId] = entity.productId
          it[salePriceCents] = entity.salePriceCents
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[status] = entity.status.name
          it[FlashSaleActivitiesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findActiveByProductId(
    productId: Long,
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): FlashSaleActivityModel? =
    transaction(database) {
      FlashSaleActivitiesTable
        .selectAll()
        .andWhere {
          (FlashSaleActivitiesTable.productId eq productId) and
            (FlashSaleActivitiesTable.status eq status.name) and
            (FlashSaleActivitiesTable.validFrom lessEq now) and
            (FlashSaleActivitiesTable.validTo greaterEq now)
        }.orderBy(
          FlashSaleActivitiesTable.validFrom to SortOrder.DESC,
          FlashSaleActivitiesTable.id to SortOrder.DESC,
        ).limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsOverlap(
    productId: Long,
    status: PromotionActivityStatus,
    newValidFrom: LocalDateTime,
    newValidTo: LocalDateTime,
    excludeId: Long?,
  ): Boolean =
    transaction(database) {
      FlashSaleActivitiesTable
        .selectAll()
        .andWhere {
          (FlashSaleActivitiesTable.productId eq productId) and
            (FlashSaleActivitiesTable.status eq status.name) and
            (FlashSaleActivitiesTable.validFrom less newValidTo) and
            (FlashSaleActivitiesTable.validTo greater newValidFrom) and
            (if (excludeId != null) FlashSaleActivitiesTable.id neq excludeId else Op.TRUE)
        }.limit(1)
        .any()
    }
}
