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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QProductOptionModel.Companion.productOptionModel
import dev.yidafu.aqua.common.domain.model.enums.ProductOptionType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

/**
 * 商品属性选项字典仓库 QueryDSL 实现。
 * 注意:@SoftDelete 由 Hibernate 在 SQL 层自动过滤已删行。
 */
@Repository
class ProductOptionRepositoryImpl : ProductOptionRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findMaxSortOrder(type: ProductOptionType): Int? =
    queryFactory
      .select(productOptionModel.sortOrder.max())
      .from(productOptionModel)
      .where(productOptionModel.optionType.eq(type))
      .fetchOne()
}
