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

package dev.yidafu.aqua.product.domain.repository

import dev.yidafu.aqua.common.domain.model.ProductOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import dev.yidafu.aqua.common.domain.table.ProductOptionsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 商品属性选项字典仓库实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted=false;delete 软删(语义对齐 @SoftDelete)。
 */
@Repository
class ProductOptionRepositoryImpl(
  private val database: Database,
) : ProductOptionRepository {
  private fun toModel(row: ResultRow): ProductOptionModel =
    ProductOptionModel(
      id = row[ProductOptionsTable.id],
      optionType = ProductOptionType.fromValue(row[ProductOptionsTable.optionType]),
      value = row[ProductOptionsTable.optionValue],
      sortOrder = row[ProductOptionsTable.sortOrder],
      enabled = row[ProductOptionsTable.enabled],
      deletedAt = row[ProductOptionsTable.deletedAt],
      deletedBy = row[ProductOptionsTable.deletedBy],
      createdAt = row[ProductOptionsTable.createdAt],
      updatedAt = row[ProductOptionsTable.updatedAt],
    )

  override fun findById(id: Long): Optional<ProductOptionModel> =
    Optional.ofNullable(
      transaction(database) {
        ProductOptionsTable
          .selectAll()
          .andWhere { ProductOptionsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun delete(entity: ProductOptionModel) {
    val id = requireNotNull(entity.id) { "ProductOptionModel.id 不能为空(软删必须携带主键)" }
    transaction(database) {
      val now = LocalDateTime.now()
      // activeOnly:对已软删行二次 delete 为 no-op(对齐旧 JPA load→软删过滤→不落库)
      ProductOptionsTable.update({ ProductOptionsTable.activeOnly { ProductOptionsTable.id eq id } }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun save(entity: ProductOptionModel): ProductOptionModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        ProductOptionsTable
          .selectAllIncludingDeleted()
          .where { ProductOptionsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        ProductOptionsTable.update({ ProductOptionsTable.id eq id }) {
          it[optionType] = entity.optionType.name
          it[optionValue] = entity.value
          it[sortOrder] = entity.sortOrder
          it[enabled] = entity.enabled
          it[updatedAt] = now
        }
      } else {
        ProductOptionsTable.insert {
          it[ProductOptionsTable.id] = id
          it[optionType] = entity.optionType.name
          it[optionValue] = entity.value
          it[sortOrder] = entity.sortOrder
          it[enabled] = entity.enabled
          it[createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findByOptionTypeOrderBySortOrderAscCreatedAtAsc(type: ProductOptionType): List<ProductOptionModel> =
    transaction(database) {
      ProductOptionsTable
        .selectAll()
        .andWhere { ProductOptionsTable.optionType eq type.name }
        .orderBy(
          ProductOptionsTable.sortOrder to SortOrder.ASC,
          ProductOptionsTable.createdAt to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByOptionTypeAndEnabledTrueOrderBySortOrderAscCreatedAtAsc(type: ProductOptionType): List<ProductOptionModel> =
    transaction(database) {
      ProductOptionsTable
        .selectAll()
        .andWhere { (ProductOptionsTable.optionType eq type.name) and (ProductOptionsTable.enabled eq true) }
        .orderBy(
          ProductOptionsTable.sortOrder to SortOrder.ASC,
          ProductOptionsTable.createdAt to SortOrder.ASC,
        ).toList()
    }.map(::toModel)

  override fun findByOptionTypeAndValue(
    type: ProductOptionType,
    value: String,
  ): ProductOptionModel? =
    transaction(database) {
      ProductOptionsTable
        .selectAll()
        .andWhere { (ProductOptionsTable.optionType eq type.name) and (ProductOptionsTable.optionValue eq value) }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findMaxSortOrder(type: ProductOptionType): Int? =
    transaction(database) {
      val maxExpr = ProductOptionsTable.sortOrder.max()
      ProductOptionsTable
        .select(maxExpr)
        .andWhere { ProductOptionsTable.optionType eq type.name }
        .singleOrNull()
        ?.get(maxExpr)
    }
}
