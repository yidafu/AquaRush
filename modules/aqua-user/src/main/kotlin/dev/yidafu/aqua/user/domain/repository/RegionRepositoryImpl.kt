/*
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

package dev.yidafu.aqua.user.domain.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QRegionModel.regionModel
import dev.yidafu.aqua.common.domain.model.RegionModel
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

/**
 * Custom repository implementation for Region entity using QueryDSL
 */
@Repository
class RegionRepositoryImpl : RegionRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findRootRegions(level: Int): List<RegionModel> {
    return queryFactory.selectFrom(regionModel)
      .where(
        regionModel.parentCode.eq("0")
          .and(regionModel.level.eq(level))
      )
      .fetch()
  }

  override fun existsByNameAndLevelAndParentCode(
    name: String,
    level: Int,
    parentCode: String
  ): Boolean {
    return queryFactory.query()
      .from(regionModel)
      .where(
        regionModel.name.eq(name)
          .and(regionModel.level.eq(level))
          .and(regionModel.parentCode.eq(parentCode))
      )
      .fetchCount() > 0
  }
}