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

package dev.yidafu.aqua.marketing.coupon.domain.repository

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.table.CouponTemplatesTable
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
 * 优惠券模板仓储实现(JPA→Exposed 迁移 P4)。
 * 查询过滤 is_deleted;delete 软删三列写入(语义对齐 @SoftDelete + P4 约定)。
 */
@Repository
class CouponTemplateRepositoryImpl(
  private val database: Database,
) : CouponTemplateRepository {
  private fun toModel(row: ResultRow): CouponTemplateModel =
    CouponTemplateModel(
      id = row[CouponTemplatesTable.id],
      name = row[CouponTemplatesTable.name],
      couponType =
        dev.yidafu.aqua.common.domain.model.enums.CouponType
          .fromValue(row[CouponTemplatesTable.couponType]),
      thresholdCents = row[CouponTemplatesTable.thresholdCents],
      discountAmountCents = row[CouponTemplatesTable.discountAmountCents],
      discountRatePermille = row[CouponTemplatesTable.discountRatePermille],
      scope =
        dev.yidafu.aqua.common.domain.model.enums.CouponScope
          .fromValue(row[CouponTemplatesTable.scope]),
      productId = row[CouponTemplatesTable.productId],
      validityType =
        dev.yidafu.aqua.common.domain.model.enums.ValidityType
          .fromValue(row[CouponTemplatesTable.validityType]),
      validFrom = row[CouponTemplatesTable.validFrom],
      validTo = row[CouponTemplatesTable.validTo],
      validDays = row[CouponTemplatesTable.validDays],
      issuanceType =
        dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
          .fromValue(row[CouponTemplatesTable.issuanceType]),
      perUserLimit = row[CouponTemplatesTable.perUserLimit],
      status = CouponTemplateStatus.fromValue(row[CouponTemplatesTable.status]),
      createdAt = row[CouponTemplatesTable.createdAt],
      updatedAt = row[CouponTemplatesTable.updatedAt],
      deletedAt = row[CouponTemplatesTable.deletedAt],
      deletedBy = row[CouponTemplatesTable.deletedBy],
    )

  override fun findByName(name: String): CouponTemplateModel? =
    transaction(database) {
      CouponTemplatesTable
        .selectAll()
        .andWhere { CouponTemplatesTable.name eq name }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByStatus(status: CouponTemplateStatus): List<CouponTemplateModel> =
    transaction(database) {
      CouponTemplatesTable
        .selectAll()
        .andWhere { CouponTemplatesTable.status eq status.name }
        .orderBy(CouponTemplatesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findById(id: Long): Optional<CouponTemplateModel> =
    Optional.ofNullable(
      transaction(database) {
        CouponTemplatesTable
          .selectAll()
          .andWhere { CouponTemplatesTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findAllById(ids: List<Long>): List<CouponTemplateModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      CouponTemplatesTable
        .selectAll()
        .andWhere { CouponTemplatesTable.id inList ids }
        .orderBy(CouponTemplatesTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun searchTemplates(
    keyword: String?,
    status: CouponTemplateStatus?,
    pageable: Pageable,
  ): Page<CouponTemplateModel> {
    var condition: Op<Boolean> = Op.TRUE
    keyword?.takeIf { it.isNotBlank() }?.let {
      condition = condition and (CouponTemplatesTable.name.lowerCase() like "%${it.lowercase()}%")
    }
    status?.let { condition = condition and (CouponTemplatesTable.status eq it.name) }
    val (content, total) =
      transaction(database) {
        val content =
          CouponTemplatesTable
            .selectAll()
            .andWhere { condition }
            // 固定 id 升序破平(原 JPA 无序,取稳定插入序)
            .orderBy(CouponTemplatesTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to CouponTemplatesTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun save(entity: CouponTemplateModel): CouponTemplateModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        CouponTemplatesTable
          .selectAllIncludingDeleted()
          .where { CouponTemplatesTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        CouponTemplatesTable.update({ CouponTemplatesTable.id eq id }) {
          it[name] = entity.name
          it[couponType] = entity.couponType.name
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[validityType] = entity.validityType.name
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[validDays] = entity.validDays
          it[issuanceType] = entity.issuanceType.name
          it[perUserLimit] = entity.perUserLimit
          it[status] = entity.status.name
          it[updatedAt] = now
        }
      } else {
        CouponTemplatesTable.insert {
          it[CouponTemplatesTable.id] = id
          it[name] = entity.name
          it[couponType] = entity.couponType.name
          it[thresholdCents] = entity.thresholdCents
          it[discountAmountCents] = entity.discountAmountCents
          it[discountRatePermille] = entity.discountRatePermille
          it[scope] = entity.scope.name
          it[productId] = entity.productId
          it[validityType] = entity.validityType.name
          it[validFrom] = entity.validFrom
          it[validTo] = entity.validTo
          it[validDays] = entity.validDays
          it[issuanceType] = entity.issuanceType.name
          it[perUserLimit] = entity.perUserLimit
          it[status] = entity.status.name
          it[CouponTemplatesTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
