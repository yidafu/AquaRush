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

package dev.yidafu.aqua.analytics.statistics.model.repository

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QOrderItemModel
import dev.yidafu.aqua.common.domain.model.QOrderModel
import dev.yidafu.aqua.common.domain.model.QProductModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class ProductSalesStatisticsRepositoryImpl : ProductSalesStatisticsRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  private val orderItemModel = QOrderItemModel.orderItemModel
  private val orderModel = QOrderModel.orderModel
  private val productModel = QProductModel.productModel

  override fun getProductDailySales(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    productId: Long?,
  ): List<ProductDailySalesResult> {
    // Use DATE(created_at) to group by date
    val dateExpr = Expressions.dateTemplate(LocalDate::class.java, "DATE({0})", orderItemModel.createdAt)

    val whereClause =
      com.querydsl.core
        .BooleanBuilder()
        .and(orderModel.status.eq(OrderModelStatus.COMPLETED))
        .and(orderItemModel.createdAt.goe(startDateTime))
        .and(orderItemModel.createdAt.loe(endDateTime))

    if (productId != null) {
      whereClause.and(orderItemModel.productId.eq(productId))
    }

    val results =
      queryFactory
        .select(
          dateExpr,
          orderItemModel.productId,
          productModel.name.coalesce("未知商品"),
          orderItemModel.quantity.sumLong(),
          orderItemModel.totalPriceCents.sumLong(),
        ).from(orderItemModel)
        .join(orderModel)
        .on(orderItemModel.orderId.eq(orderModel.id))
        .leftJoin(productModel)
        .on(orderItemModel.productId.eq(productModel.id))
        .where(whereClause)
        .groupBy(dateExpr, orderItemModel.productId, productModel.name)
        .fetch()

    return results.map { row ->
      val dateValue = row.get(dateExpr)
      ProductDailySalesResult(
        date = dateValue?.toString() ?: "",
        productId = row.get(orderItemModel.productId)!!,
        productName = row.get(productModel.name.coalesce("未知商品"))!!,
        salesVolume = row.get(orderItemModel.quantity.sumLong()) ?: 0L,
        revenue = row.get(orderItemModel.totalPriceCents.sumLong()) ?: 0L,
      )
    }
  }

  override fun getAllProductIdsAndNames(): List<ProductInfo> {
    val results =
      queryFactory
        .select(
          productModel.id,
          productModel.name,
        ).from(productModel)
        .fetch()

    return results.map { row ->
      ProductInfo(
        productId = row.get(productModel.id)!!,
        productName = row.get(productModel.name)!!,
      )
    }
  }
}
