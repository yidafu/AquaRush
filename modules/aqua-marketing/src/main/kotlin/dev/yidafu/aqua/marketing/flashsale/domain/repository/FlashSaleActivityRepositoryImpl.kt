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

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.QFlashSaleActivityModel.Companion.flashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 限时特价活动仓库 QueryDSL 实现。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行。
 */
@Repository
class FlashSaleActivityRepositoryImpl : FlashSaleActivityRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findActiveByProductId(
    productId: Long,
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): FlashSaleActivityModel? =
    queryFactory
      .selectFrom(flashSaleActivityModel)
      .where(
        flashSaleActivityModel.status.eq(status),
        flashSaleActivityModel.validFrom.loe(now),
        flashSaleActivityModel.validTo.goe(now),
        flashSaleActivityModel.productId.eq(productId),
      ).orderBy(flashSaleActivityModel.validFrom.desc())
      .fetchFirst()

  override fun existsOverlap(
    productId: Long,
    status: PromotionActivityStatus,
    newValidFrom: LocalDateTime,
    newValidTo: LocalDateTime,
    excludeId: Long?,
  ): Boolean {
    val builder =
      BooleanBuilder()
        .and(flashSaleActivityModel.productId.eq(productId))
        .and(flashSaleActivityModel.status.eq(status))
        .and(flashSaleActivityModel.validFrom.lt(newValidTo))
        .and(flashSaleActivityModel.validTo.gt(newValidFrom))
    excludeId?.let { builder.and(flashSaleActivityModel.id.ne(it)) }
    return queryFactory
      .selectFrom(flashSaleActivityModel)
      .where(builder)
      .fetchCount() > 0
  }
}
