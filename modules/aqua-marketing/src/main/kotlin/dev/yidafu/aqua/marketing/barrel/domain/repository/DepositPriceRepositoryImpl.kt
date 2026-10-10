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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import dev.yidafu.aqua.common.domain.table.DepositPricesTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
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
 * 水桶费价目仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class DepositPriceRepositoryImpl(
  private val database: Database,
) : DepositPriceRepository {
  private fun toModel(row: ResultRow): DepositPriceModel =
    DepositPriceModel(
      id = row[DepositPricesTable.id],
      specification = row[DepositPricesTable.specification],
      priceCents = row[DepositPricesTable.priceCents],
      status = DepositPriceStatus.fromValue(row[DepositPricesTable.status]),
      sortOrder = row[DepositPricesTable.sortOrder],
      createdAt = row[DepositPricesTable.createdAt],
      updatedAt = row[DepositPricesTable.updatedAt],
      deletedAt = row[DepositPricesTable.deletedAt],
      deletedBy = row[DepositPricesTable.deletedBy],
    )

  override fun findBySpecification(specification: String): DepositPriceModel? =
    transaction(database) {
      DepositPricesTable
        .selectAll()
        .andWhere { DepositPricesTable.specification eq specification }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findById(id: Long): Optional<DepositPriceModel> =
    Optional.ofNullable(
      transaction(database) {
        DepositPricesTable
          .selectAll()
          .andWhere { DepositPricesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun searchPrices(
    status: DepositPriceStatus?,
    keyword: String?,
    pageable: Pageable,
  ): Page<DepositPriceModel> {
    var condition: Op<Boolean> = Op.TRUE
    status?.let { condition = condition and (DepositPricesTable.status eq it.name) }
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (DepositPricesTable.specification.lowerCase() like "%${it.lowercase()}%")
    }
    val (content, total) =
      transaction(database) {
        val content =
          DepositPricesTable
            .selectAll()
            .andWhere { condition }
            // 固定 sortOrder 升序(对齐原 PageRequest Sort),id 破平防跨页重复
            .orderBy(DepositPricesTable.sortOrder to SortOrder.ASC, DepositPricesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to DepositPricesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: DepositPriceModel): DepositPriceModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        DepositPricesTable
          .selectAllIncludingDeleted()
          .where { DepositPricesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        DepositPricesTable.update({ DepositPricesTable.id eq id }) {
          it[specification] = entity.specification
          it[priceCents] = entity.priceCents
          it[status] = entity.status.name
          it[sortOrder] = entity.sortOrder
          it[updatedAt] = now
        }
      } else {
        DepositPricesTable.insert {
          it[DepositPricesTable.id] = id
          it[specification] = entity.specification
          it[priceCents] = entity.priceCents
          it[status] = entity.status.name
          it[sortOrder] = entity.sortOrder
          it[DepositPricesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
