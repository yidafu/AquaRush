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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.domain.table.TicketTypesTable
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
 * 桶水票票种仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class TicketTypeRepositoryImpl(
  private val database: Database,
) : TicketTypeRepository {
  private fun toModel(row: ResultRow): TicketTypeModel =
    TicketTypeModel(
      id = row[TicketTypesTable.id],
      name = row[TicketTypesTable.name],
      ticketType = TicketTypeKind.fromValue(row[TicketTypesTable.ticketType]),
      productId = row[TicketTypesTable.productId],
      specification = row[TicketTypesTable.specification],
      packageCount = row[TicketTypesTable.packageCount],
      packagePriceCents = row[TicketTypesTable.packagePriceCents],
      giftCount = row[TicketTypesTable.giftCount],
      validityType = ValidityType.fromValue(row[TicketTypesTable.validityType]),
      validFrom = row[TicketTypesTable.validFrom],
      validTo = row[TicketTypesTable.validTo],
      validDays = row[TicketTypesTable.validDays],
      perUserLimit = row[TicketTypesTable.perUserLimit],
      isLeadGeneration = row[TicketTypesTable.isLeadGeneration],
      activationOrderCount = row[TicketTypesTable.activationOrderCount],
      maxRedeemPerOrder = row[TicketTypesTable.maxRedeemPerOrder],
      status = TicketTypeStatus.fromValue(row[TicketTypesTable.status]),
      createdAt = row[TicketTypesTable.createdAt],
      updatedAt = row[TicketTypesTable.updatedAt],
      deletedAt = row[TicketTypesTable.deletedAt],
      deletedBy = row[TicketTypesTable.deletedBy],
    )

  override fun findByName(name: String): TicketTypeModel? =
    transaction(database) {
      TicketTypesTable
        .selectAll()
        .andWhere { TicketTypesTable.name eq name }
        .orderBy(TicketTypesTable.id to SortOrder.ASC)
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findAllById(ids: List<Long>): List<TicketTypeModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      TicketTypesTable
        .selectAll()
        .andWhere { TicketTypesTable.id inList ids }
        .orderBy(TicketTypesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun findById(id: Long): Optional<TicketTypeModel> =
    Optional.ofNullable(
      transaction(database) {
        TicketTypesTable
          .selectAll()
          .andWhere { TicketTypesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByStatus(status: TicketTypeStatus): List<TicketTypeModel> =
    transaction(database) {
      TicketTypesTable
        .selectAll()
        .andWhere { TicketTypesTable.status eq status.name }
        .orderBy(TicketTypesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun searchTicketTypes(
    keyword: String?,
    status: TicketTypeStatus?,
    pageable: Pageable,
  ): Page<TicketTypeModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (TicketTypesTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    status?.let { condition = condition and (TicketTypesTable.status eq it.name) }
    val (content, total) =
      transaction(database) {
        val content =
          TicketTypesTable
            .selectAll()
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(TicketTypesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to TicketTypesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: TicketTypeModel): TicketTypeModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        TicketTypesTable
          .selectAllIncludingDeleted()
          .where { TicketTypesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        TicketTypesTable.update({ TicketTypesTable.id eq id }) {
          it[name] = entity.name
          it[ticketType] = entity.ticketType.name
          it[productId] = entity.productId
          it[specification] = entity.specification
          it[packageCount] = entity.packageCount
          it[packagePriceCents] = entity.packagePriceCents
          it[giftCount] = entity.giftCount
          it[validityType] = entity.validityType.name
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[validDays] = entity.validDays
          it[perUserLimit] = entity.perUserLimit
          it[isLeadGeneration] = entity.isLeadGeneration
          it[activationOrderCount] = entity.activationOrderCount
          it[maxRedeemPerOrder] = entity.maxRedeemPerOrder
          it[status] = entity.status.name
          it[updatedAt] = now
        }
      } else {
        TicketTypesTable.insert {
          it[TicketTypesTable.id] = id
          it[name] = entity.name
          it[ticketType] = entity.ticketType.name
          it[productId] = entity.productId
          it[specification] = entity.specification
          it[packageCount] = entity.packageCount
          it[packagePriceCents] = entity.packagePriceCents
          it[giftCount] = entity.giftCount
          it[validityType] = entity.validityType.name
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[validDays] = entity.validDays
          it[perUserLimit] = entity.perUserLimit
          it[isLeadGeneration] = entity.isLeadGeneration
          it[activationOrderCount] = entity.activationOrderCount
          it[maxRedeemPerOrder] = entity.maxRedeemPerOrder
          it[status] = entity.status.name
          it[TicketTypesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
