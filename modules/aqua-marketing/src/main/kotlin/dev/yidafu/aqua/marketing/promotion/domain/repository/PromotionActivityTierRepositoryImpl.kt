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

import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.table.PromotionActivityTiersTable
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
 * 满减/折扣活动档位仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class PromotionActivityTierRepositoryImpl(
  private val database: Database,
) : PromotionActivityTierRepository {
  private fun toModel(row: ResultRow): PromotionActivityTierModel =
    PromotionActivityTierModel(
      id = row[PromotionActivityTiersTable.id],
      activityId = row[PromotionActivityTiersTable.activityId],
      thresholdCents = row[PromotionActivityTiersTable.thresholdCents],
      discountAmountCents = row[PromotionActivityTiersTable.discountAmountCents],
      discountRatePermille = row[PromotionActivityTiersTable.discountRatePermille],
      sort = row[PromotionActivityTiersTable.sort],
      createdAt = row[PromotionActivityTiersTable.createdAt],
      updatedAt = row[PromotionActivityTiersTable.updatedAt],
      deletedAt = row[PromotionActivityTiersTable.deletedAt],
      deletedBy = row[PromotionActivityTiersTable.deletedBy],
    )

  override fun findByActivityIdOrderBySortAsc(activityId: Long): List<PromotionActivityTierModel> =
    transaction(database) {
      PromotionActivityTiersTable
        .selectAll()
        .where { PromotionActivityTiersTable.activeOnly { PromotionActivityTiersTable.activityId eq activityId } }
        .orderBy(PromotionActivityTiersTable.sort to SortOrder.ASC, PromotionActivityTiersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByActivityId(activityId: Long): List<PromotionActivityTierModel> =
    transaction(database) {
      PromotionActivityTiersTable
        .selectAll()
        .where { PromotionActivityTiersTable.activeOnly { PromotionActivityTiersTable.activityId eq activityId } }
        // 原 JPA 无序,取稳定插入序
        .orderBy(PromotionActivityTiersTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun delete(entity: PromotionActivityTierModel) {
    val id =
      requireNotNull(entity.id) {
        "The given entity must not be transient"
      }
    transaction(database) {
      // activeOnly:对已软删行二次 delete 为 no-op(双删不重写 deleted_at)
      PromotionActivityTiersTable.update({
        PromotionActivityTiersTable.activeOnly { PromotionActivityTiersTable.id eq id }
      }) {
        it[isDeleted] = true
        it[deletedAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
      }
    }
  }

  override fun save(entity: PromotionActivityTierModel): PromotionActivityTierModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        PromotionActivityTiersTable
          .selectAll()
          .where { PromotionActivityTiersTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        PromotionActivityTiersTable.update({ PromotionActivityTiersTable.id eq id }) {
          it[activityId] = entity.activityId
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[sort] = entity.sort
          it[updatedAt] = now
        }
      } else {
        PromotionActivityTiersTable.insert {
          it[PromotionActivityTiersTable.id] = id
          it[activityId] = entity.activityId
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[sort] = entity.sort
          it[PromotionActivityTiersTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
