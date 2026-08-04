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

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.api.dto.ProductSearchRequest
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.QProductModel.Companion.productModel
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
    query: ProductSearchRequest,
    pageable: Pageable,
  ): Page<ProductModel> {
    val builder = BooleanBuilder()

    // Add keyword filter (LIKE query)
    if (!query.keyword.isNullOrBlank()) {
      builder.and(productModel.name.like("%${query.keyword}%"))
    }
    query.status?.let {
      builder.and(productModel.status.eq(it))
    }
    query.minPrice?.let {
      builder.and(productModel.price.gt(it))
    }
    query.maxPrice?.let {
      builder.and(productModel.price.lt(it))
    }
    query.minStock?.let {
      builder.and(productModel.stock.gt(it))
    }
    query.maxStock?.let {
      builder.and(productModel.stock.lt(it))
    }
    query.minSalesVolume?.let {
      builder.and(productModel.salesVolume.gt(it))
    }
    query.maxSalesVolume?.let {
      builder.and(productModel.salesVolume.lt(it))
    }
    val orderBy =
      when (query.sortBy) {
        "CREATED_AT_ASC" -> productModel.status.asc()
        "CREATED_AT_DESC" -> productModel.createdAt.desc()
        "PRICE_ASC" -> productModel.price.asc()
        "PRICE_DESC" -> productModel.price.desc()
        "SALES_VOLUME_ASC" -> productModel.salesVolume.asc()
        "SALES_VOLUME_DESC" -> productModel.salesVolume.desc()
        "SORT_ORDER_ASC" -> productModel.sortOrder.asc()
        "SORT_ORDER_DESC" -> productModel.sortOrder.desc()
        else -> productModel.createdAt.desc()
      }

    // Get total count
    val total =
      queryFactory
        .query()
        .from(productModel)
        .where(builder)
        .orderBy(orderBy)
        .fetchCount()

    // Get paginated results
    val results =
      queryFactory
        .selectFrom(productModel)
        .where(builder)
        .orderBy(orderBy)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .fetch()

    return PageImpl(results, pageable, total)
  }
}
