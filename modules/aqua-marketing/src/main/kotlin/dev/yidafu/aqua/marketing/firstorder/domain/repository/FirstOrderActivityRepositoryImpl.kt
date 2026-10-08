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

package dev.yidafu.aqua.marketing.firstorder.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.QFirstOrderActivityModel.Companion.firstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动仓库 QueryDSL 实现。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行。
 */
@Repository
class FirstOrderActivityRepositoryImpl : FirstOrderActivityRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findActiveActivities(
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): List<FirstOrderActivityModel> =
    queryFactory
      .selectFrom(firstOrderActivityModel)
      .where(
        firstOrderActivityModel.status.eq(status),
        firstOrderActivityModel.validFrom.loe(now),
        firstOrderActivityModel.validTo.goe(now),
      ).fetch()

  override fun findActiveNewcomerPriceByProductId(
    productId: Long,
    activityType: FirstOrderActivityType,
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): FirstOrderActivityModel? =
    queryFactory
      .selectFrom(firstOrderActivityModel)
      .where(
        firstOrderActivityModel.status.eq(status),
        firstOrderActivityModel.validFrom.loe(now),
        firstOrderActivityModel.validTo.goe(now),
        firstOrderActivityModel.activityType.eq(activityType),
        firstOrderActivityModel.productId.eq(productId),
      ).orderBy(firstOrderActivityModel.validFrom.desc())
      .fetchFirst()

  override fun existsOverlap(
    productId: Long,
    activityType: FirstOrderActivityType,
    status: PromotionActivityStatus,
    newValidFrom: LocalDateTime,
    newValidTo: LocalDateTime,
    excludeId: Long?,
  ): Boolean {
    val builder =
      BooleanBuilder()
        .and(firstOrderActivityModel.productId.eq(productId))
        .and(firstOrderActivityModel.status.eq(status))
        .and(firstOrderActivityModel.activityType.eq(activityType))
        .and(firstOrderActivityModel.validFrom.lt(newValidTo))
        .and(firstOrderActivityModel.validTo.gt(newValidFrom))
    excludeId?.let { builder.and(firstOrderActivityModel.id.ne(it)) }
    return queryFactory
      .selectFrom(firstOrderActivityModel)
      .where(builder)
      .fetchCount() > 0
  }
}
