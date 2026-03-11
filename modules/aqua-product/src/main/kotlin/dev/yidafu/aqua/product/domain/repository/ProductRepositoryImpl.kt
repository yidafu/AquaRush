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

package dev.yidafu.aqua.product.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.QProductModel.Companion.productModel
import dev.yidafu.aqua.common.graphql.generated.ProductStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * QueryDSL implementation for ProductRepositoryCustom
 */
@Repository
class ProductRepositoryImpl : ProductRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun searchProducts(
    keyword: String?,
    status: ProductStatus?,
    pageable: Pageable,
  ): Page<ProductModel> {
    val builder = BooleanBuilder()

    // Add keyword filter (LIKE query)
    if (keyword.isNullOrBlank()) {
      builder.and(productModel.name.like("%$keyword%"))
    }
    // Add status filter
    status?.let {
      builder.and(productModel.status.eq(it))
    }

    // Get total count
    val total =
      queryFactory
        .query()
        .from(productModel)
        .where(builder)
        .fetchCount()

    // Get paginated results
    val results =
      queryFactory
        .selectFrom(productModel)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .fetch()

    return PageImpl(results, pageable, total)
  }
}
