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

package dev.yidafu.aqua.marketing.customer.domain.repository

import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.domain.table.CustomerGroupsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
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
 * 客户分组仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class CustomerGroupRepositoryImpl(
  private val database: Database,
) : CustomerGroupRepository {
  private fun toModel(row: ResultRow): CustomerGroupModel =
    CustomerGroupModel(
      id = row[CustomerGroupsTable.id],
      name = row[CustomerGroupsTable.name],
      description = row[CustomerGroupsTable.description],
      createdAt = row[CustomerGroupsTable.createdAt],
      updatedAt = row[CustomerGroupsTable.updatedAt],
      deletedAt = row[CustomerGroupsTable.deletedAt],
      deletedBy = row[CustomerGroupsTable.deletedBy],
    )

  override fun findByName(name: String): CustomerGroupModel? =
    transaction(database) {
      CustomerGroupsTable
        .selectAll()
        .andWhere { CustomerGroupsTable.name eq name }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsByName(name: String): Boolean =
    transaction(database) {
      CustomerGroupsTable
        .selectAll()
        .andWhere { CustomerGroupsTable.name eq name }
        .limit(1)
        .any()
    }

  override fun findById(id: Long): Optional<CustomerGroupModel> =
    Optional.ofNullable(
      transaction(database) {
        CustomerGroupsTable
          .selectAll()
          .andWhere { CustomerGroupsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun existsById(id: Long): Boolean =
    transaction(database) {
      CustomerGroupsTable
        .selectAll()
        .andWhere { CustomerGroupsTable.id eq id }
        .limit(1)
        .any()
    }

  override fun findAllById(ids: List<Long>): List<CustomerGroupModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      CustomerGroupsTable
        .selectAll()
        .andWhere { CustomerGroupsTable.id inList ids }
        .orderBy(CustomerGroupsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun searchGroups(
    keyword: String?,
    pageable: Pageable,
  ): Page<CustomerGroupModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (CustomerGroupsTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    val (content, total) =
      transaction(database) {
        val content =
          CustomerGroupsTable
            .selectAll()
            .andWhere { condition }
            // 关键词分支对齐原 OrderByCreatedAtDesc;id 破平防跨页重复
            .orderBy(CustomerGroupsTable.createdAt to SortOrder.DESC, CustomerGroupsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to CustomerGroupsTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun delete(entity: CustomerGroupModel) {
    val id =
      requireNotNull(entity.id) {
        "The given entity must not be transient"
      }
    transaction(database) {
      // activeOnly:对已软删行二次 delete 为 no-op(双删不重写 deleted_at)
      CustomerGroupsTable.update({ CustomerGroupsTable.activeOnly { CustomerGroupsTable.id eq id } }) {
        it[isDeleted] = true
        it[deletedAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
      }
    }
  }

  override fun save(entity: CustomerGroupModel): CustomerGroupModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        CustomerGroupsTable
          .selectAllIncludingDeleted()
          .where { CustomerGroupsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        CustomerGroupsTable.update({ CustomerGroupsTable.id eq id }) {
          it[name] = entity.name
          it[description] = entity.description
          it[updatedAt] = now
        }
      } else {
        CustomerGroupsTable.insert {
          it[CustomerGroupsTable.id] = id
          it[name] = entity.name
          it[description] = entity.description
          it[CustomerGroupsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
