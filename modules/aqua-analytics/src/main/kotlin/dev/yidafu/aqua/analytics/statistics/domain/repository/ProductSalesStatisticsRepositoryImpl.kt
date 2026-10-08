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

package dev.yidafu.aqua.analytics.statistics.domain.repository

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.QOrderItemModel
import dev.yidafu.aqua.common.domain.model.QOrderModel
import dev.yidafu.aqua.common.domain.model.QProductModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
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
    // CAST(... AS DATE) 按日分组(H2/PostgreSQL 兼容;Postgres 的 DATE(timestamp) 写法 H2 不支持)
    val dateExpr = Expressions.dateTemplate(LocalDate::class.java, "CAST({0} AS DATE)", orderItemModel.createdAt)

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

  override fun countProductsByStatus(): Map<ProductModelStatus, Long> =
    queryFactory
      .select(productModel.status, productModel.count())
      .from(productModel)
      .groupBy(productModel.status)
      .fetch()
      .associate { row ->
        row.get(productModel.status)!! to (row.get(productModel.count()) ?: 0L)
      }

  override fun aggregateProductValue(): ProductStatisticsAggregate {
    val countExpr = productModel.count()
    val totalValueExpr = productModel.price.sumLong().coalesce(0L)
    val totalSalesExpr = productModel.salesVolume.sumLong().coalesce(0L)
    val row =
      queryFactory
        .select(countExpr, totalValueExpr, totalSalesExpr)
        .from(productModel)
        .fetchOne()
    return ProductStatisticsAggregate(
      totalProducts = row?.get(countExpr) ?: 0L,
      totalValueCents = row?.get(totalValueExpr) ?: 0L,
      totalSales = row?.get(totalSalesExpr) ?: 0L,
    )
  }

  override fun countLowStockProducts(threshold: Long): Long =
    queryFactory
      .select(productModel.count())
      .from(productModel)
      .where(productModel.stock.loe(threshold.toInt()))
      .fetchOne() ?: 0L

  override fun findLowStockProducts(
    threshold: Long,
    limit: Int,
  ): List<ProductStockInfo> {
    val idExpr = productModel.id
    val nameExpr = productModel.name
    val specExpr = productModel.specification
    val stockExpr = productModel.stock
    return queryFactory
      .select(idExpr, nameExpr, specExpr, stockExpr)
      .from(productModel)
      .where(stockExpr.loe(threshold.toInt()))
      .orderBy(stockExpr.asc(), idExpr.asc())
      .limit(limit.toLong())
      .fetch()
      .map { row ->
        ProductStockInfo(
          productId = row.get(idExpr)!!,
          productName = row.get(nameExpr)!!,
          specification = row.get(specExpr)!!,
          stock = row.get(stockExpr)!!,
        )
      }
  }

  override fun getProductSalesRanking(limit: Int): List<ProductSalesRankingResult> {
    // 已完成订单项聚合:销量 = SUM(quantity)、营收 = SUM(total_price_cents),与「营收=已完成订单金额合计」口径同源(P2-7);
    // 商品当前上下架/售罄状态不影响历史已完成销量(P1-4 排行口径)。
    val volumeExpr = orderItemModel.quantity.sumLong().coalesce(0L)
    val revenueExpr = orderItemModel.totalPriceCents.sumLong().coalesce(0L)
    val nameExpr = productModel.name.coalesce("未知商品")
    return queryFactory
      .select(orderItemModel.productId, nameExpr, volumeExpr, revenueExpr)
      .from(orderItemModel)
      .join(orderModel)
      .on(orderItemModel.orderId.eq(orderModel.id))
      .leftJoin(productModel)
      .on(orderItemModel.productId.eq(productModel.id))
      .where(orderModel.status.eq(OrderModelStatus.COMPLETED))
      .groupBy(orderItemModel.productId, nameExpr)
      .orderBy(volumeExpr.desc(), revenueExpr.desc())
      .limit(limit.toLong())
      .fetch()
      .map { row ->
        ProductSalesRankingResult(
          productId = row.get(orderItemModel.productId)!!,
          productName = row.get(nameExpr)!!,
          salesVolume = row.get(volumeExpr)!!,
          revenueCents = row.get(revenueExpr)!!,
        )
      }
  }
}
