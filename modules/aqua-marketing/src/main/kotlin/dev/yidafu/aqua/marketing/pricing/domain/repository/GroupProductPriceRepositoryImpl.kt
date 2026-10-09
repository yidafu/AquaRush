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

package dev.yidafu.aqua.marketing.pricing.domain.repository

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.common.domain.table.GroupProductPricesTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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

/**
 * 分组商品专属价仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class GroupProductPriceRepositoryImpl(
  private val database: Database,
) : GroupProductPriceRepository {
  private fun toModel(row: ResultRow): GroupProductPriceModel =
    GroupProductPriceModel(
      id = row[GroupProductPricesTable.id],
      customerGroupId = row[GroupProductPricesTable.customerGroupId],
      productId = row[GroupProductPricesTable.productId],
      priceCents = row[GroupProductPricesTable.priceCents],
      createdAt = row[GroupProductPricesTable.createdAt],
      updatedAt = row[GroupProductPricesTable.updatedAt],
      deletedAt = row[GroupProductPricesTable.deletedAt],
      deletedBy = row[GroupProductPricesTable.deletedBy],
    )

  override fun findByCustomerGroupIdAndProductId(
    groupId: Long,
    productId: Long,
  ): GroupProductPriceModel? =
    transaction(database) {
      GroupProductPricesTable
        .selectAll()
        .where {
          GroupProductPricesTable.activeOnly {
            (GroupProductPricesTable.customerGroupId eq groupId) and
              (GroupProductPricesTable.productId eq productId)
          }
        }.limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByCustomerGroupId(groupId: Long): List<GroupProductPriceModel> =
    transaction(database) {
      GroupProductPricesTable
        .selectAll()
        .where { GroupProductPricesTable.activeOnly { GroupProductPricesTable.customerGroupId eq groupId } }
        .orderBy(GroupProductPricesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByCustomerGroupId(
    groupId: Long,
    pageable: Pageable,
  ): Page<GroupProductPriceModel> {
    val condition =
      GroupProductPricesTable.customerGroupId eq groupId
    val (content, total) =
      transaction(database) {
        val content =
          GroupProductPricesTable
            .selectAll()
            .where { GroupProductPricesTable.activeOnly { condition } }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(GroupProductPricesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to GroupProductPricesTable.selectAll().where { GroupProductPricesTable.activeOnly { condition } }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun delete(entity: GroupProductPriceModel) {
    val id =
      requireNotNull(entity.id) {
        "The given entity must not be transient"
      }
    transaction(database) {
      // activeOnly:对已软删行二次 delete 为 no-op(双删不重写 deleted_at)
      GroupProductPricesTable.update({ GroupProductPricesTable.activeOnly { GroupProductPricesTable.id eq id } }) {
        it[isDeleted] = true
        it[deletedAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
      }
    }
  }

  override fun save(entity: GroupProductPriceModel): GroupProductPriceModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        GroupProductPricesTable
          .selectAll()
          .where { GroupProductPricesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        GroupProductPricesTable.update({ GroupProductPricesTable.id eq id }) {
          it[customerGroupId] = entity.customerGroupId
          it[productId] = entity.productId
          it[priceCents] = entity.priceCents
          it[updatedAt] = now
        }
      } else {
        GroupProductPricesTable.insert {
          it[GroupProductPricesTable.id] = id
          it[customerGroupId] = entity.customerGroupId
          it[productId] = entity.productId
          it[priceCents] = entity.priceCents
          it[GroupProductPricesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
