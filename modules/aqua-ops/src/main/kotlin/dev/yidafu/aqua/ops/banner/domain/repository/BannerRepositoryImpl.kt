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

package dev.yidafu.aqua.ops.banner.domain.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.QBannerModel.Companion.bannerModel
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 轮播图仓库 QueryDSL 实现。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行,QueryDSL 查询同样生效。
 */
@Repository
class BannerRepositoryImpl : BannerRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findMaxSortOrder(): Int? =
    queryFactory
      .select(bannerModel.sortOrder.max())
      .from(bannerModel)
      .fetchOne()

  /**
   * 悲观写锁需活动事务;REQUIRED 并入调用方事务(原 @Lock 查询语义一致)。
   */
  @Transactional
  override fun findAllOrderedForUpdate(): List<BannerModel> =
    queryFactory
      .selectFrom(bannerModel)
      .orderBy(bannerModel.sortOrder.asc())
      .setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .fetch()

  override fun findActiveBanners(now: LocalDateTime): List<BannerModel> =
    queryFactory
      .selectFrom(bannerModel)
      .where(
        bannerModel.enabled.isTrue,
        bannerModel.startAt.isNull().or(bannerModel.startAt.loe(now)),
        bannerModel.endAt.isNull().or(bannerModel.endAt.goe(now)),
      ).orderBy(bannerModel.sortOrder.asc())
      .fetch()
}
