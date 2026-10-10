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

import dev.yidafu.aqua.analytics.common.exposed.dateTrunc
import dev.yidafu.aqua.analytics.common.exposed.longOf
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.ProductsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 商品销量统计仓储实现(JPA→Exposed 迁移 P3)。
 * 口径与原 QueryDSL 版一致:销量/营收聚合仅计 COMPLETED 订单明细(P2-7/P1-4),
 * 商品统计全状态口径(OFFLINE/OUT_OF_STOCK 计入总数,R-PRO-02)。
 * ResultRow 取值全部在事务闭包内(聚合表达式 hashCode 依赖事务上下文)。
 */
@Repository
class ProductSalesStatisticsRepositoryImpl(
  private val database: Database,
) : ProductSalesStatisticsRepositoryCustom {
  override fun getProductDailySales(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    productId: Long?,
  ): List<ProductDailySalesResult> {
    // DATE_TRUNC('day') 按日分组(H2/PostgreSQL 兼容,语义同原 CAST(... AS DATE))
    val dateExpr = dateTrunc("day", OrderItemsTable.createdAt)
    val qtySum = OrderItemsTable.quantity.sum()
    val revenueSum = OrderItemsTable.totalPriceCents.sum()

    return transaction(database) {
      // 软删豁免:ProductsTable 走 LEFT JOIN 只为取名称(商品已软删时回退"未知商品"),故意不附加 activeOnly
      OrderItemsTable
        .join(OrdersTable, JoinType.INNER, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
        .join(ProductsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.productId eq ProductsTable.id })
        .select(
          dateExpr,
          OrderItemsTable.productId,
          ProductsTable.name,
          qtySum,
          revenueSum,
        ).where {
          OrdersTable
            .activeOnly {
              (OrdersTable.status eq OrderModelStatus.COMPLETED.name) and
                (OrderItemsTable.createdAt greaterEq startDateTime) and
                (OrderItemsTable.createdAt lessEq endDateTime) and
                (if (productId != null) OrderItemsTable.productId eq productId else org.jetbrains.exposed.v1.core.Op.TRUE)
            }.and(OrderItemsTable.activeOnly())
        }.groupBy(dateExpr, OrderItemsTable.productId, ProductsTable.name)
        .toList()
        .map { row ->
          ProductDailySalesResult(
            date = row[dateExpr]?.toLocalDate()?.toString() ?: "",
            productId = row[OrderItemsTable.productId],
            // 商品被软删时名称回退(与原 coalesce(name, '未知商品') 语义一致)
            productName = row[ProductsTable.name] ?: "未知商品",
            salesVolume = row.longOf(qtySum),
            revenue = row.longOf(revenueSum),
          )
        }
    }
  }

  override fun getAllProductIdsAndNames(): List<ProductInfo> =
    transaction(database) {
      ProductsTable
        .select(ProductsTable.id, ProductsTable.name)
        .toList()
        .map { row -> ProductInfo(productId = row[ProductsTable.id], productName = row[ProductsTable.name]) }
    }

  override fun countProductsByStatus(): Map<ProductModelStatus, Long> {
    val productCount = ProductsTable.id.count()
    return transaction(database) {
      ProductsTable
        .select(ProductsTable.status, productCount)
        .groupBy(ProductsTable.status)
        .toList()
        .associate { row -> ProductModelStatus.fromValue(row[ProductsTable.status]) to row[productCount] }
    }
  }

  override fun aggregateProductValue(): ProductStatisticsAggregate {
    val productCount = ProductsTable.id.count()
    val priceSum = ProductsTable.priceCents.sum()
    val salesSum = ProductsTable.salesVolume.sum()
    return transaction(database) {
      val row =
        ProductsTable
          .select(productCount, priceSum, salesSum)
          .first()
      ProductStatisticsAggregate(
        totalProducts = row[productCount],
        totalValueCents = row.longOf(priceSum),
        totalSales = row.longOf(salesSum),
      )
    }
  }

  override fun countLowStockProducts(threshold: Long): Long =
    transaction(database) {
      ProductsTable
        .select(ProductsTable.id)
        .andWhere { ProductsTable.stockQuantity lessEq threshold.toInt() }
        .count()
    }

  override fun findLowStockProducts(
    threshold: Long,
    limit: Int,
  ): List<ProductStockInfo> =
    transaction(database) {
      ProductsTable
        .select(
          ProductsTable.id,
          ProductsTable.name,
          ProductsTable.specification,
          ProductsTable.stockQuantity,
        ).andWhere { ProductsTable.stockQuantity lessEq threshold.toInt() }
        .orderBy(ProductsTable.stockQuantity to SortOrder.ASC, ProductsTable.id to SortOrder.ASC)
        .limit(limit)
        .toList()
        .map { row ->
          ProductStockInfo(
            productId = row[ProductsTable.id],
            productName = row[ProductsTable.name],
            specification = row[ProductsTable.specification],
            stock = row[ProductsTable.stockQuantity],
          )
        }
    }

  override fun getProductSalesRanking(limit: Int): List<ProductSalesRankingResult> {
    // 已完成订单项聚合:销量 = SUM(quantity)、营收 = SUM(total_price_cents),与「营收=已完成订单金额合计」口径同源(P2-7);
    // 商品当前上下架/售罄状态不影响历史已完成销量(P1-4 排行口径)。
    val volumeExpr = OrderItemsTable.quantity.sum()
    val revenueExpr = OrderItemsTable.totalPriceCents.sum()
    return transaction(database) {
      // 软删豁免:ProductsTable 走 LEFT JOIN 只为取名称(商品已软删时回退"未知商品"),故意不附加 activeOnly
      OrderItemsTable
        .join(OrdersTable, JoinType.INNER, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
        .join(ProductsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.productId eq ProductsTable.id })
        .select(
          OrderItemsTable.productId,
          ProductsTable.name,
          volumeExpr,
          revenueExpr,
        ).where {
          OrdersTable
            .activeOnly { OrdersTable.status eq OrderModelStatus.COMPLETED.name }
            .and(OrderItemsTable.activeOnly())
        }.groupBy(OrderItemsTable.productId, ProductsTable.name)
        .orderBy(volumeExpr to SortOrder.DESC, revenueExpr to SortOrder.DESC)
        .limit(limit)
        .toList()
        .map { row ->
          ProductSalesRankingResult(
            productId = row[OrderItemsTable.productId],
            productName = row[ProductsTable.name] ?: "未知商品",
            salesVolume = row.longOf(volumeExpr),
            revenueCents = row.longOf(revenueExpr),
          )
        }
    }
  }
}
