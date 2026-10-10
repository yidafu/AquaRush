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
        .andWhere {
          (GroupProductPricesTable.customerGroupId eq groupId) and
            (GroupProductPricesTable.productId eq productId)
        }.limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByCustomerGroupId(groupId: Long): List<GroupProductPriceModel> =
    transaction(database) {
      GroupProductPricesTable
        .selectAll()
        .andWhere { GroupProductPricesTable.customerGroupId eq groupId }
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
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(GroupProductPricesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to GroupProductPricesTable.selectAll().andWhere { condition }.count()
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

  /**
   * upsert 语义(对齐 JPA `save`)。自然键 (customer_group_id, product_id) 上有**非部分**
   * 唯一索引 `uk_gpp_group_product`:软删行仍占用该键,若直接 INSERT 新 id 会撞唯一索引
   * (管理后台「删除分组价后再重新配置同一商品」会 500)。因此按自然键查找时**包含软删行**,
   * 命中即复活该行(清 is_deleted/deleted_at/deleted_by 并覆盖价格),而非插入新行。
   */
  override fun save(entity: GroupProductPriceModel): GroupProductPriceModel =
    transaction(database) {
      val now = LocalDateTime.now()
      val existingRow =
        if (entity.id != null) {
          GroupProductPricesTable
            .selectAllIncludingDeleted()
            .where { GroupProductPricesTable.id eq entity.id!! }
            .limit(1)
            .firstOrNull()
        } else {
          // 业务键可能命中已软删行(命中即复活),存在性判定必须含已删行
          GroupProductPricesTable
            .selectAllIncludingDeleted()
            .where {
              (GroupProductPricesTable.customerGroupId eq entity.customerGroupId) and
                (GroupProductPricesTable.productId eq entity.productId)
            }.limit(1)
            .firstOrNull()
        }
      if (existingRow != null) {
        val id = existingRow[GroupProductPricesTable.id]
        entity.id = id
        GroupProductPricesTable.update({ GroupProductPricesTable.id eq id }) {
          it[customerGroupId] = entity.customerGroupId
          it[productId] = entity.productId
          it[priceCents] = entity.priceCents
          // 复活软删行:同键唯一索引下不允许并存第二条(见方法注释)
          it[isDeleted] = false
          it[deletedAt] = null
          it[deletedBy] = null
          it[updatedAt] = now
        }
      } else {
        val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
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
