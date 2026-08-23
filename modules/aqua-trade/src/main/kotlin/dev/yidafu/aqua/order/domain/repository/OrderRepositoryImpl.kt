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

package dev.yidafu.aqua.order.domain.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.api.dto.MarketingUsageItemRow
import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.api.dto.OrderAnalyticsRow
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.QOrderGiftModel.Companion.orderGiftModel
import dev.yidafu.aqua.common.domain.model.QOrderItemModel.Companion.orderItemModel
import dev.yidafu.aqua.common.domain.model.QOrderModel.Companion.orderModel
import dev.yidafu.aqua.common.domain.model.QUserCouponModel.Companion.userCouponModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Enhanced OrderRepository implementation using QueryDSL
 * Provides type-safe complex queries for order management
 */
@Repository
class OrderRepositoryImpl : OrderRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findOrdersWithFilters(
    userId: Long?,
    status: OrderModelStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    orderNumber: String?,
    statuses: List<OrderModelStatus>?,
  ): List<OrderModel> {
    val builder = BooleanBuilder()

    userId?.let { builder.and(orderModel.userId.eq(it)) }
    status?.let { builder.and(orderModel.status.eq(it)) }
    deliveryWorkerId?.let { builder.and(orderModel.deliveryWorkerId.eq(it)) }
    orderNumber?.let { builder.and(orderModel.orderNo.eq(it)) }
    statuses?.let { builder.and(orderModel.status.`in`(it)) }
    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(orderModel.createdAt.between(start, end))
      }
    }

    return queryFactory
      .selectFrom(orderModel)
      .where(builder)
      .orderBy(orderModel.createdAt.desc())
      .fetch()
  }

  override fun findDeliveryWorkerOrdersWithFilters(
    deliveryWorkerId: Long,
    status: OrderModelStatus,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    limit: Int?,
  ): List<OrderModel> {
    var query =
      queryFactory
        .selectFrom(orderModel)
        .where(
          orderModel.deliveryWorkerId
            .eq(deliveryWorkerId)
            .and(orderModel.status.eq(status)),
        )

    startDate?.let { start ->
      endDate?.let { end ->
        query = query.where(orderModel.createdAt.between(start, end))
      }
    }

    var result = query.orderBy(orderModel.createdAt.desc())

    limit?.let {
      result = result.limit(it.toLong())
    }

    return result.fetch()
  }

  override fun countOrdersWithFilters(
    userId: Long?,
    status: OrderModelStatus?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    statuses: List<OrderModelStatus>?,
  ): Long {
    val builder = BooleanBuilder()

    userId?.let { builder.and(orderModel.userId.eq(it)) }
    status?.let { builder.and(orderModel.status.eq(it)) }
    deliveryWorkerId?.let { builder.and(orderModel.deliveryWorkerId.eq(it)) }
    statuses?.let { builder.and(orderModel.status.`in`(it)) }
    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(orderModel.createdAt.between(start, end))
      }
    }

    return queryFactory
      .query()
      .from(orderModel)
      .where(builder)
      .fetchCount()
  }

  override fun findOrdersPaginated(
    keyword: String?,
    status: OrderModelStatus?,
    userId: Long?,
    deliveryWorkerId: Long?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    minAmount: Long?,
    maxAmount: Long?,
    page: Int,
    size: Int,
    sortField: String,
    sortDirection: String,
  ): Page<OrderModel> {
    val builder = BooleanBuilder()

    keyword?.let { keywordVal ->
      // Search in order number (using contains)
      builder.and(orderModel.orderNo.containsIgnoreCase(keywordVal))
    }
    status?.let { builder.and(orderModel.status.eq(it)) }
    userId?.let { builder.and(orderModel.userId.eq(it)) }
    deliveryWorkerId?.let { builder.and(orderModel.deliveryWorkerId.eq(it)) }

    startDate?.let { start ->
      endDate?.let { end ->
        builder.and(orderModel.createdAt.between(start, end))
      }
    }

    minAmount?.let { builder.and(orderModel.amountCents.goe(it)) }
    maxAmount?.let { builder.and(orderModel.amountCents.loe(it)) }

    // Build sorting - use Spring Data Sort
    val sortFieldName =
      when (sortField.lowercase()) {
        "totalamount", "total_amount" -> "amountCents"
        "updatedat", "updated_at" -> "updatedAt"
        else -> "createdAt"
      }

    val springSort =
      if (sortDirection.equals("asc", ignoreCase = true)) {
        Sort.by(Sort.Order.asc(sortFieldName))
      } else {
        Sort.by(Sort.Order.desc(sortFieldName))
      }

    val pageable = PageRequest.of(page, size, springSort)

    val query =
      queryFactory
        .selectFrom(orderModel)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())

    // Apply ordering using QueryDSL
    when (sortField.lowercase()) {
      "totalamount", "total_amount" -> {
        if (sortDirection.equals("asc", ignoreCase = true)) {
          query.orderBy(orderModel.amountCents.asc())
        } else {
          query.orderBy(orderModel.amountCents.desc())
        }
      }

      "updatedat", "updated_at" -> {
        if (sortDirection.equals("asc", ignoreCase = true)) {
          query.orderBy(orderModel.updatedAt.asc())
        } else {
          query.orderBy(orderModel.updatedAt.desc())
        }
      }

      else -> {
        if (sortDirection.equals("asc", ignoreCase = true)) {
          query.orderBy(orderModel.createdAt.asc())
        } else {
          query.orderBy(orderModel.createdAt.desc())
        }
      }
    }

    @Suppress("UNCHECKED_CAST")
    val content = query.fetch() as List<OrderModel>
    val total =
      queryFactory
        .query()
        .from(orderModel)
        .where(builder)
        .fetchCount()

    return PageImpl(content, pageable, total)
  }

  override fun findByDeliveryWorkerIdAndStatusIn(
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>,
    keyword: String?,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    val builder =
      BooleanBuilder()
        .and(orderModel.status.`in`(statuses))

    if (deliveryWorkerId != null) {
      builder.and(orderModel.deliveryWorkerId.eq(deliveryWorkerId))
    }

    // 添加关键字搜索条件
    if (!keyword.isNullOrBlank()) {
      // 使用 ilike 进行模糊匹配搜索内容
//      builder.and(orderModel.searchContent.eq("%$keyword%"))
    }

    val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))

    val query =
      queryFactory
        .selectFrom(orderModel)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .orderBy(orderModel.createdAt.desc())

    @Suppress("UNCHECKED_CAST")
    val content = query.fetch() as List<OrderModel>
    val total =
      queryFactory
        .query()
        .from(orderModel)
        .where(builder)
        .fetchCount()

    return PageImpl(content, pageable, total)
  }

  override fun findUserOrdersPaginated(
    userId: Long,
    statuses: List<OrderModelStatus>?,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    val builder =
      BooleanBuilder().and(orderModel.userId.eq(userId))
    statuses?.takeIf { it.isNotEmpty() }?.let {
      builder.and(orderModel.status.`in`(it))
    }

    val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))

    val query =
      queryFactory
        .selectFrom(orderModel)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .orderBy(orderModel.createdAt.desc())

    @Suppress("UNCHECKED_CAST")
    val content = query.fetch() as List<OrderModel>
    val total =
      queryFactory
        .query()
        .from(orderModel)
        .where(builder)
        .fetchCount()

    return PageImpl(content, pageable, total)
  }

  @Transactional
  override fun bulkUpdateOrderStatus(
    orderIds: List<Long>,
    newStatus: OrderModelStatus,
    deliveryWorkerId: Long?,
  ): Int {
    var update =
      queryFactory
        .update(orderModel)
        .set(orderModel.status, newStatus)
        .where(orderModel.id.`in`(orderIds))

    deliveryWorkerId?.let {
      update = update.set(orderModel.deliveryWorkerId, it)
    }

    return update.execute().toInt()
  }

  override fun getOrderAnalytics(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderAnalyticsRow> {
    // Create date expression for PostgreSQL DATE() function
    val dateExpr =
      Expressions.dateTemplate(
        LocalDate::class.java,
        "DATE({0})",
        orderModel.createdAt,
      )

    val sumAmount: NumberExpression<Long> = orderModel.amountCents.sumLong()
    val avgAmount = orderModel.amountCents.avg()

    val results =
      queryFactory
        .select(
          dateExpr,
          orderModel.status,
          orderModel.count(),
          sumAmount,
          avgAmount,
          orderModel.userId.countDistinct(),
          orderModel.deliveryWorkerId.countDistinct(),
        ).from(orderModel)
        .where(orderModel.createdAt.between(startDate, endDate))
        .groupBy(dateExpr, orderModel.status)
        .orderBy(dateExpr.desc(), orderModel.status.asc())
        .fetch()

    @Suppress("UNCHECKED_CAST")
    return results.map { tuple ->
      OrderAnalyticsRow(
        orderDate = tuple.get(dateExpr) ?: LocalDate.now(),
        status = tuple.get(orderModel.status) ?: OrderModelStatus.PENDING_PAYMENT,
        orderCount = tuple.get(orderModel.count()) ?: 0L,
        totalRevenueCents = tuple.get(sumAmount) ?: 0L,
        averageOrderValueCents = Math.round(tuple.get(avgAmount) ?: 0.0).toLong(),
        uniqueCustomers = tuple.get(orderModel.userId.countDistinct()) ?: 0L,
        activeWorkers = tuple.get(orderModel.deliveryWorkerId.countDistinct()) ?: 0L,
      )
    }
  }

  override fun countOrdersByDateRange(
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long {
    val baseCondition = orderModel.createdAt.goe(startOfDay).and(orderModel.createdAt.lt(endOfDay))

    val whereClauseBuilder = BooleanBuilder(baseCondition)

    if (deliveryWorkerId != null) {
      whereClauseBuilder.and(orderModel.deliveryWorkerId.eq(deliveryWorkerId))
    }

    statuses?.let {
      whereClauseBuilder.and(orderModel.status.`in`(it))
    }

    return queryFactory
      .select(orderModel.count())
      .from(orderModel)
      .where(whereClauseBuilder)
      .fetchOne() ?: 0L
  }

  override fun sumAmountCentsByStatusAndDateRange(
    statuses: List<OrderModelStatus>,
    startOfDay: LocalDateTime,
    endOfDay: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long {
    val baseCondition = orderModel.createdAt.goe(startOfDay).and(orderModel.createdAt.lt(endOfDay))

    val whereClause =
      if (deliveryWorkerId != null) {
        baseCondition
          .and(orderModel.deliveryWorkerId.eq(deliveryWorkerId))
          .and(orderModel.status.`in`(statuses))
      } else {
        baseCondition.and(orderModel.status.`in`(statuses))
      }

    return queryFactory
      .select(orderModel.amountCents.sumLong())
      .from(orderModel)
      .where(whereClause)
      .fetchOne() ?: 0L
  }

  override fun countAndSumByPaymentType(
    deliveryWorkerId: Long,
    status: OrderModelStatus,
    paymentType: PaymentType,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Array<Long> {
    val whereClause =
      orderModel.deliveryWorkerId
        .eq(deliveryWorkerId)
        .and(orderModel.status.eq(status))
        .and(orderModel.paymentType.eq(paymentType))
        .and(orderModel.completedAt.goe(startDate))
        .and(orderModel.completedAt.lt(endDate))

    val count =
      queryFactory
        .select(orderModel.count())
        .from(orderModel)
        .where(whereClause)
        .fetchOne() ?: 0L

    val sum =
      queryFactory
        .select(orderModel.amountCents.sumLong())
        .from(orderModel)
        .where(whereClause)
        .fetchOne() ?: 0L

    return arrayOf(count, sum)
  }

  override fun countOrdersByUserIdGroupByStatus(userId: Long): Map<OrderModelStatus, Long> =
    queryFactory
      .select(orderModel.status, orderModel.count())
      .from(orderModel)
      .where(orderModel.userId.eq(userId))
      .groupBy(orderModel.status)
      .fetch()
      .associate { row ->
        row.get(orderModel.status)!! to (row.get(orderModel.count()) ?: 0L)
      }

  // ========================================================================
  // 后台首页 orderStatistics 聚合(与 StatisticsOrderRepositoryImpl 同款 QueryDSL 写法)
  // createdAt 一律用 gt/lt 严格开区间,与原内存过滤 isAfter/isBefore 逐位等价,不改统计口径
  // ========================================================================

  override fun countByStatusAndCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long {
    val whereClause = buildWindowWhereClause(startDate, endDate, deliveryWorkerId, statuses)
    return queryFactory
      .select(orderModel.count())
      .from(orderModel)
      .where(whereClause)
      .fetchOne() ?: 0L
  }

  override fun sumAmountCentsByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long {
    val whereClause = buildWindowWhereClause(startDate, endDate, deliveryWorkerId, statuses)
    return queryFactory
      .select(orderModel.amountCents.sumLong())
      .from(orderModel)
      .where(whereClause)
      .fetchOne() ?: 0L
  }

  override fun countByStatusAndCompletedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): Long {
    val whereClause = buildCompletedAtWindowWhereClause(startDate, endDate, deliveryWorkerId, statuses)
    return queryFactory
      .select(orderModel.count())
      .from(orderModel)
      .where(whereClause)
      .fetchOne() ?: 0L
  }

  override fun sumAmountCentsByStatusAndCompletedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
  ): Long {
    val whereClause = buildCompletedAtWindowWhereClause(startDate, endDate, deliveryWorkerId, statuses)
    return queryFactory
      .select(orderModel.amountCents.sumLong())
      .from(orderModel)
      .where(whereClause)
      .fetchOne() ?: 0L
  }

  override fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel> =
    queryFactory
      .selectFrom(orderModel)
      .where(orderModel.createdAt.gt(startDate).and(orderModel.createdAt.lt(endDate)))
      .fetch()

  override fun findByStatusAndCreatedAtBetween(
    statuses: List<OrderModelStatus>,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<OrderModel> =
    queryFactory
      .selectFrom(orderModel)
      .where(
        orderModel.status
          .`in`(statuses)
          .and(orderModel.createdAt.gt(startDate))
          .and(orderModel.createdAt.lt(endDate)),
      ).fetch()

  /**
   * 构建后台首页 orderStatistics 的窗口谓词(严格开区间 + 可选配送员/状态过滤)
   */
  private fun buildWindowWhereClause(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): BooleanBuilder {
    val whereClause = BooleanBuilder(orderModel.createdAt.gt(startDate).and(orderModel.createdAt.lt(endDate)))
    deliveryWorkerId?.let {
      whereClause.and(orderModel.deliveryWorkerId.eq(it))
    }
    statuses?.let {
      whereClause.and(orderModel.status.`in`(it))
    }
    return whereClause
  }

  /**
   * 构建已完成口径的窗口谓词:窗口按 completed_at(完成时间)严格开区间,其余过滤与
   * [buildWindowWhereClause] 一致。营收/已完成订单数按完成日归属(PRD F-STA-002)。
   */
  private fun buildCompletedAtWindowWhereClause(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    deliveryWorkerId: Long?,
    statuses: List<OrderModelStatus>?,
  ): BooleanBuilder {
    val whereClause = BooleanBuilder(orderModel.completedAt.gt(startDate).and(orderModel.completedAt.lt(endDate)))
    deliveryWorkerId?.let {
      whereClause.and(orderModel.deliveryWorkerId.eq(it))
    }
    statuses?.let {
      whereClause.and(orderModel.status.`in`(it))
    }
    return whereClause
  }

  // ========================================================================
  // 营销使用情况(PRD 10-marketing.md 营销方式详情:相关订单记录 + 营销使用汇总)
  // ========================================================================

  override fun findOrdersByMarketingUsage(
    criteria: MarketingUsageOrderCriteria,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))
    val builder = marketingUsagePredicate(criteria)
    val content =
      queryFactory
        .selectFrom(orderModel)
        .where(builder)
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .orderBy(orderModel.createdAt.desc())
        .fetch()
    val total =
      queryFactory
        .query()
        .from(orderModel)
        .where(builder)
        .fetchCount()
    return PageImpl(content, pageable, total)
  }

  override fun countOrdersByMarketingUsage(criteria: MarketingUsageOrderCriteria): Long =
    queryFactory
      .query()
      .from(orderModel)
      .where(marketingUsagePredicate(criteria))
      .fetchCount()

  override fun sumMarketingDiscountCents(criteria: MarketingUsageOrderCriteria): Long =
    when (criteria.kind) {
      MarketingUsageKind.PROMOTION_ACTIVITY -> {
        sumDiscountCents(orderModel.promotionDiscountCents, orderModel.promotionActivityId.eq(criteria.refId))
      }

      MarketingUsageKind.FIRST_ORDER_REDUCE -> {
        sumDiscountCents(orderModel.firstOrderDiscountCents, orderModel.firstOrderActivityId.eq(criteria.refId))
      }

      MarketingUsageKind.COUPON_TEMPLATE -> {
        sumDiscountCents(orderModel.couponDiscountCents, marketingUsagePredicate(criteria))
      }

      // 限时特价/新人专享价/买赠满赠无订单列优惠快照,让利金额由 aggregateMarketingUsageItems 计算
      else -> {
        0L
      }
    }

  override fun aggregateMarketingUsageItems(criteria: MarketingUsageOrderCriteria): List<MarketingUsageItemRow> =
    when (criteria.kind) {
      MarketingUsageKind.GIFT_ACTIVITY -> {
        queryFactory
          .select(orderGiftModel.productId, orderGiftModel.quantity.sumLong())
          .from(orderGiftModel)
          .where(orderGiftModel.giftActivityId.eq(criteria.refId))
          .groupBy(orderGiftModel.productId)
          .fetch()
          .map { row ->
            MarketingUsageItemRow(
              productId = row.get(orderGiftModel.productId) ?: 0L,
              unitPriceCents = null,
              quantitySum = row.get(orderGiftModel.quantity.sumLong()) ?: 0L,
            )
          }
      }

      MarketingUsageKind.FLASH_SALE_ITEM, MarketingUsageKind.NEWCOMER_PRICE_ITEM -> {
        // 时间窗匹配经 orders 子查询实现(与 marketingUsagePredicate 同口径,避免 join 重复行)
        queryFactory
          .select(orderItemModel.productId, orderItemModel.unitPriceCents, orderItemModel.quantity.sumLong())
          .from(orderItemModel)
          .where(
            orderItemModel.productId.eq(criteria.productId),
            orderItemModel.unitPriceCents.eq(criteria.unitPriceCents),
            orderItemModel.orderId.`in`(
              JPAExpressions
                .select(orderModel.id)
                .from(orderModel)
                .where(
                  orderModel.createdAt.goe(criteria.validFrom),
                  orderModel.createdAt.lt(criteria.validTo),
                ),
            ),
          ).groupBy(orderItemModel.productId, orderItemModel.unitPriceCents)
          .fetch()
          .map { row ->
            MarketingUsageItemRow(
              productId = row.get(orderItemModel.productId) ?: 0L,
              unitPriceCents = row.get(orderItemModel.unitPriceCents),
              quantitySum = row.get(orderItemModel.quantity.sumLong()) ?: 0L,
            )
          }
      }

      else -> {
        emptyList()
      }
    }

  /**
   * 营销使用订单匹配谓词(join 语义见 MarketingUsageKind):
   * 关联表匹配用子查询(避免 join 造成的订单重复行)。
   */
  private fun marketingUsagePredicate(criteria: MarketingUsageOrderCriteria): BooleanBuilder {
    val builder = BooleanBuilder()
    when (criteria.kind) {
      MarketingUsageKind.PROMOTION_ACTIVITY -> {
        builder.and(orderModel.promotionActivityId.eq(criteria.refId))
      }

      MarketingUsageKind.FIRST_ORDER_REDUCE -> {
        builder.and(orderModel.firstOrderActivityId.eq(criteria.refId))
      }

      MarketingUsageKind.COUPON_TEMPLATE -> {
        builder.and(
          orderModel.couponId.`in`(
            JPAExpressions
              .select(userCouponModel.id)
              .from(userCouponModel)
              .where(userCouponModel.couponTemplateId.eq(criteria.refId)),
          ),
        )
      }

      MarketingUsageKind.GIFT_ACTIVITY -> {
        builder.and(
          orderModel.id.`in`(
            JPAExpressions
              .select(orderGiftModel.orderId)
              .from(orderGiftModel)
              .where(orderGiftModel.giftActivityId.eq(criteria.refId)),
          ),
        )
      }

      MarketingUsageKind.FLASH_SALE_ITEM, MarketingUsageKind.NEWCOMER_PRICE_ITEM -> {
        requireNotNull(criteria.productId) { "productId is required for ${criteria.kind}" }
        requireNotNull(criteria.unitPriceCents) { "unitPriceCents is required for ${criteria.kind}" }
        requireNotNull(criteria.validFrom) { "validFrom is required for ${criteria.kind}" }
        requireNotNull(criteria.validTo) { "validTo is required for ${criteria.kind}" }
        // 活动窗口径与引擎一致:validFrom ≤ 下单时间 < validTo
        builder.and(orderModel.createdAt.goe(criteria.validFrom))
        builder.and(orderModel.createdAt.lt(criteria.validTo))
        builder.and(
          orderModel.id.`in`(
            JPAExpressions
              .select(orderItemModel.orderId)
              .from(orderItemModel)
              .where(
                orderItemModel.productId.eq(criteria.productId),
                orderItemModel.unitPriceCents.eq(criteria.unitPriceCents),
              ),
          ),
        )
      }
    }
    return builder
  }

  private fun sumDiscountCents(
    expr: NumberPath<Long>,
    predicate: Predicate,
  ): Long =
    queryFactory
      .select(expr.sumLong())
      .from(orderModel)
      .where(predicate)
      .fetchOne() ?: 0L
}
