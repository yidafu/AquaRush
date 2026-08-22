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

import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.DateTemplate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import dev.yidafu.aqua.common.domain.model.QOrderGiftModel.Companion.orderGiftModel
import dev.yidafu.aqua.common.domain.model.QOrderItemModel.Companion.orderItemModel
import dev.yidafu.aqua.common.domain.model.QOrderModel.Companion.orderModel
import dev.yidafu.aqua.common.domain.model.QPaymentRefundModel.Companion.paymentRefundModel
import dev.yidafu.aqua.common.domain.model.QTicketPurchaseModel.Companion.ticketPurchaseModel
import dev.yidafu.aqua.common.domain.model.QUserActionLogModel.Companion.userActionLogModel
import dev.yidafu.aqua.common.domain.model.QUserCouponModel.Companion.userCouponModel
import dev.yidafu.aqua.common.domain.model.QUserModel.Companion.userModel
import dev.yidafu.aqua.common.domain.model.QUserTicketModel.Companion.userTicketModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 统计快照按日聚合仓储实现
 * 所有聚合均按单日窗口 [date, date+1) 过滤源表,金额按订单只计一次(防 P1-3 虚增)。
 */
@Repository
class StatisticsSnapshotAggregationRepositoryImpl : StatisticsSnapshotAggregationRepository {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun aggregateOrderFacts(date: LocalDate): List<DailyOrderFactModel> {
    val window = dateWindow(date)

    // 查询 1:仅 orders 聚合 —— 订单数与金额按订单只计一次(不 join order_items,防多商品订单金额重复求和,P1-3)
    val orderRows =
      queryFactory
        .select(
          orderModel.status,
          orderModel.deliveryWorkerId,
          orderModel.id.count(),
          orderModel.amountCents.sumLong().coalesce(0L),
        ).from(orderModel)
        .where(orderModel.createdAt.goe(window.start), orderModel.createdAt.lt(window.end))
        .groupBy(orderModel.status, orderModel.deliveryWorkerId)
        .fetch()
        .map { row ->
          OrderGroupKey(status = row.get(orderModel.status)!!.name, deliveryWorkerId = row.get(orderModel.deliveryWorkerId)) to
            OrderCounts(
              orderCount = row.get(orderModel.id.count())!!,
              amountCents = row.get(orderModel.amountCents.sumLong().coalesce(0L))!!,
              itemQuantity = 0L,
            )
        }.toMap()

    // 查询 2:订单 ⋈ 明细 —— 商品件数按明细汇总(仅取件数,不与金额混算)
    val quantityRows =
      queryFactory
        .select(
          orderModel.status,
          orderModel.deliveryWorkerId,
          orderItemModel.quantity.sumLong().coalesce(0L),
        ).from(orderModel)
        .leftJoin(orderItemModel)
        .on(orderItemModel.orderId.eq(orderModel.id))
        .where(orderModel.createdAt.goe(window.start), orderModel.createdAt.lt(window.end))
        .groupBy(orderModel.status, orderModel.deliveryWorkerId)
        .fetch()

    val merged = orderRows.toMutableMap()
    quantityRows.forEach { row ->
      val key =
        OrderGroupKey(
          status = row.get(orderModel.status)!!.name,
          deliveryWorkerId = row.get(orderModel.deliveryWorkerId),
        )
      val counts = merged[key]
      if (counts != null) {
        merged[key] = counts.copy(itemQuantity = row.get(orderItemModel.quantity.sumLong().coalesce(0L))!!)
      } else {
        // 有明细但订单聚合缺失(理论不发生:明细必属订单);仍补零行保证一致性
        merged[key] =
          OrderCounts(
            orderCount = 0L,
            amountCents = 0L,
            itemQuantity = row.get(orderItemModel.quantity.sumLong().coalesce(0L))!!,
          )
      }
    }

    return merged.map { (key, counts) ->
      DailyOrderFactModel(
        statDate = date,
        status = key.status,
        deliveryWorkerId = key.deliveryWorkerId,
        orderCount = counts.orderCount,
        amountCents = counts.amountCents,
        itemQuantity = counts.itemQuantity,
      )
    }
  }

  override fun aggregateProductFacts(date: LocalDate): List<DailyProductFactModel> {
    val window = dateWindow(date)
    return queryFactory
      .select(
        orderModel.status,
        orderItemModel.productId,
        orderItemModel.quantity.sumLong().coalesce(0L),
        orderItemModel.totalPriceCents.sumLong().coalesce(0L),
        orderModel.id.countDistinct(),
      ).from(orderModel)
      .leftJoin(orderItemModel)
      .on(orderItemModel.orderId.eq(orderModel.id))
      .where(orderModel.createdAt.goe(window.start), orderModel.createdAt.lt(window.end))
      .groupBy(orderModel.status, orderItemModel.productId)
      .fetch()
      .map { row ->
        DailyProductFactModel(
          statDate = date,
          status = row.get(orderModel.status)!!.name,
          productId = row.get(orderItemModel.productId)!!,
          quantity = row.get(orderItemModel.quantity.sumLong().coalesce(0L))!!,
          revenueCents = row.get(orderItemModel.totalPriceCents.sumLong().coalesce(0L))!!,
          orderCount = row.get(orderModel.id.countDistinct())!!,
        )
      }
  }

  override fun aggregateUserFact(date: LocalDate): DailyUserFactModel {
    val window = dateWindow(date)
    val newUserCount =
      queryFactory
        .select(userModel.id.count())
        .from(userModel)
        .where(userModel.createdAt.goe(window.start), userModel.createdAt.lt(window.end))
        .fetchOne() ?: 0L

    val loginUserCount =
      queryFactory
        .select(userActionLogModel.userId.countDistinct())
        .from(userActionLogModel)
        .where(
          userActionLogModel.actionType.eq("USER_LOGIN"),
          userActionLogModel.createdAt.goe(window.start),
          userActionLogModel.createdAt.lt(window.end),
        ).fetchOne() ?: 0L

    return DailyUserFactModel(
      statDate = date,
      newUserCount = newUserCount,
      loginUserCount = loginUserCount,
    )
  }

  override fun aggregateRefundFacts(date: LocalDate): List<DailyRefundFactModel> {
    val window = dateWindow(date)
    return queryFactory
      .select(
        paymentRefundModel.status,
        paymentRefundModel.refundType,
        paymentRefundModel.id.count(),
        paymentRefundModel.refundAmount.sumLong().coalesce(0L),
      ).from(paymentRefundModel)
      .where(paymentRefundModel.refundedAt.goe(window.start), paymentRefundModel.refundedAt.lt(window.end))
      .groupBy(paymentRefundModel.status, paymentRefundModel.refundType)
      .fetch()
      .map { row ->
        DailyRefundFactModel(
          statDate = date,
          status = row.get(paymentRefundModel.status)!!.name,
          refundType = row.get(paymentRefundModel.refundType)!!.name,
          refundCount = row.get(paymentRefundModel.id.count())!!,
          refundAmountCents = row.get(paymentRefundModel.refundAmount.sumLong().coalesce(0L))!!,
        )
      }
  }

  /**
   * 聚合单日营销事实(F-STA-007):单日窗口 [date, date+1),空数据日返回全 0 行(快照覆盖连续)。
   * 由范围版本 [aggregateMarketingFactsRange] 取单日,避免两套聚合逻辑。
   */
  override fun aggregateMarketingFacts(date: LocalDate): DailyMarketingFactModel = aggregateMarketingFactsRange(date, date).first()

  /**
   * 聚合日期范围营销事实(按日分组,查询侧实时兜底段与快照写共用同一实现):
   * 营销订单数(任一快照优惠,按订单去重)+ 券/满减/首单立减让利 + 用券数 + 水票销售/核销 + 赠品件数;
   * 归属日:订单=createdAt,券=used_at,购票=paidAt(回退 createdAt),水票核销=usedAt,赠品=createdAt。
   * 空数据日补零(DATE_TRUNC 按日分组,Postgres/H2 兼容,与 getDailyOrderStatistics 同模式)。
   */
  override fun aggregateMarketingFactsRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyMarketingFactModel> {
    require(!startDate.isAfter(endDate)) { "营销事实聚合起止日期非法: $startDate > $endDate" }
    val window = dateWindow(startDate, endDate)
    val orderDate = dayExpr(orderModel.createdAt)
    val couponDate = dayExpr(userCouponModel.usedAt)
    val paidDate = dayExpr(ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt))
    val ticketUsedDate = dayExpr(userTicketModel.usedAt)
    val giftDate = dayExpr(orderGiftModel.createdAt)

    // 订单侧:按日 营销订单数 + 三列优惠金额合计(券/满减/首单可同单,订单数按订单去重)
    val orderByDay =
      queryFactory
        .select(
          orderDate,
          orderModel.id.countDistinct(),
          orderModel.couponDiscountCents.sumLong().coalesce(0L),
          orderModel.promotionDiscountCents.sumLong().coalesce(0L),
          orderModel.firstOrderDiscountCents.sumLong().coalesce(0L),
        ).from(orderModel)
        .where(
          orderModel.createdAt.goe(window.start),
          orderModel.createdAt.lt(window.end),
          orderModel.couponDiscountCents
            .gt(0L)
            .or(orderModel.promotionDiscountCents.gt(0L))
            .or(orderModel.firstOrderDiscountCents.gt(0L)),
        ).groupBy(orderDate)
        .fetch()
        .associate { row -> row.get(orderDate)!!.toLocalDate() to row }

    // 用券数:USED 且 used_at 在窗口,按日
    val couponByDay =
      queryFactory
        .select(couponDate, userCouponModel.id.count())
        .from(userCouponModel)
        .where(
          userCouponModel.status.eq(UserCouponStatus.USED),
          userCouponModel.usedAt.goe(window.start),
          userCouponModel.usedAt.lt(window.end),
        ).groupBy(couponDate)
        .fetch()
        .associate { row -> row.get(couponDate)!!.toLocalDate() to row.get(userCouponModel.id.count())!! }

    // 水票销售:PAID 购票按 paidAt(回退 createdAt)归属,按日;张数含赠送(N+M)
    val ticketSalesByDay =
      queryFactory
        .select(
          paidDate,
          ticketPurchaseModel.totalCount.sumLong().coalesce(0L),
          ticketPurchaseModel.packagePriceCents.sumLong().coalesce(0L),
        ).from(ticketPurchaseModel)
        .where(
          ticketPurchaseModel.paymentStatus.eq(TicketPurchaseStatus.PAID),
          ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt).goe(window.start),
          ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt).lt(window.end),
        ).groupBy(paidDate)
        .fetch()
        .associate { row -> row.get(paidDate)!!.toLocalDate() to row }

    // 水票核销:USED 按 usedAt,按日
    val ticketRedeemedByDay =
      queryFactory
        .select(ticketUsedDate, userTicketModel.id.count())
        .from(userTicketModel)
        .where(
          userTicketModel.status.eq(UserTicketStatus.USED),
          userTicketModel.usedAt.goe(window.start),
          userTicketModel.usedAt.lt(window.end),
        ).groupBy(ticketUsedDate)
        .fetch()
        .associate { row -> row.get(ticketUsedDate)!!.toLocalDate() to row.get(userTicketModel.id.count())!! }

    // 赠品件数:order_gifts 按 created_at,按日
    val giftByDay =
      queryFactory
        .select(giftDate, orderGiftModel.quantity.sumLong().coalesce(0L))
        .from(orderGiftModel)
        .where(orderGiftModel.createdAt.goe(window.start), orderGiftModel.createdAt.lt(window.end))
        .groupBy(giftDate)
        .fetch()
        .associate { row -> row.get(giftDate)!!.toLocalDate() to row.get(orderGiftModel.quantity.sumLong().coalesce(0L))!! }

    // 按日补零(与用户事实一致,快照覆盖连续)
    return dateRange(startDate, endDate).map { date ->
      val orderRow = orderByDay[date]
      val salesRow = ticketSalesByDay[date]
      DailyMarketingFactModel(
        statDate = date,
        marketingOrderCount = orderRow?.get(orderModel.id.countDistinct()) ?: 0L,
        couponBenefitCents = orderRow?.get(orderModel.couponDiscountCents.sumLong().coalesce(0L)) ?: 0L,
        promotionBenefitCents = orderRow?.get(orderModel.promotionDiscountCents.sumLong().coalesce(0L)) ?: 0L,
        firstOrderBenefitCents = orderRow?.get(orderModel.firstOrderDiscountCents.sumLong().coalesce(0L)) ?: 0L,
        couponRedeemedCount = couponByDay[date] ?: 0L,
        ticketSalesCount = salesRow?.get(ticketPurchaseModel.totalCount.sumLong().coalesce(0L)) ?: 0L,
        ticketSalesRevenueCents = salesRow?.get(ticketPurchaseModel.packagePriceCents.sumLong().coalesce(0L)) ?: 0L,
        ticketRedeemedCount = ticketRedeemedByDay[date] ?: 0L,
        giftQuantity = giftByDay[date] ?: 0L,
      )
    }
  }

  /**
   * 聚合单日营销使用事实(F-STA-007):由范围版本 [aggregateMarketingUsageFactsRange] 取单日。
   */
  override fun aggregateMarketingUsageFacts(date: LocalDate): List<DailyMarketingUsageFactModel> =
    aggregateMarketingUsageFactsRange(date, date)

  /**
   * 聚合日期范围营销使用事实(按 日 × kind × ref_id 分组,查询侧实时兜底段与快照写共用同一实现):
   * 券行按 orders.coupon_id → user_coupons.coupon_template_id 归并(含已退回券订单,订单快照保留 coupon_id);
   * 满减/首单立减按订单快照列;赠品按 order_gifts;票种按购票/核销。模板解析失败(被删)的行跳过。
   */
  override fun aggregateMarketingUsageFactsRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyMarketingUsageFactModel> {
    require(!startDate.isAfter(endDate)) { "营销使用事实聚合起止日期非法: $startDate > $endDate" }
    val window = dateWindow(startDate, endDate)
    val rows = mutableListOf<DailyMarketingUsageFactModel>()

    val orderDate = dayExpr(orderModel.createdAt)
    val couponUsedDate = dayExpr(userCouponModel.usedAt)
    val paidDate = dayExpr(ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt))
    val ticketUsedDate = dayExpr(userTicketModel.usedAt)
    val giftDate = dayExpr(orderGiftModel.createdAt)

    // 券行:orders ⋈ user_coupons(orders.coupon_id = user_coupons.id)→ (日, coupon_template_id) 分组
    val couponOrderRows =
      queryFactory
        .select(
          orderDate,
          userCouponModel.couponTemplateId,
          orderModel.id.countDistinct(),
          orderModel.couponDiscountCents.sumLong().coalesce(0L),
        ).from(orderModel)
        .join(userCouponModel)
        .on(userCouponModel.id.eq(orderModel.couponId))
        .where(
          orderModel.createdAt.goe(window.start),
          orderModel.createdAt.lt(window.end),
          orderModel.couponDiscountCents.gt(0L),
        ).groupBy(orderDate, userCouponModel.couponTemplateId)
        .fetch()
    val couponRedeemedByDateTemplate =
      queryFactory
        .select(couponUsedDate, userCouponModel.couponTemplateId, userCouponModel.id.count())
        .from(userCouponModel)
        .where(
          userCouponModel.status.eq(UserCouponStatus.USED),
          userCouponModel.usedAt.goe(window.start),
          userCouponModel.usedAt.lt(window.end),
        ).groupBy(couponUsedDate, userCouponModel.couponTemplateId)
        .fetch()
        .associate { row ->
          (row.get(couponUsedDate)!!.toLocalDate() to row.get(userCouponModel.couponTemplateId)!!) to
            row.get(userCouponModel.id.count())!!
        }
    couponOrderRows.forEach { row ->
      val date = row.get(orderDate)!!.toLocalDate()
      val templateId = row.get(userCouponModel.couponTemplateId) ?: return@forEach
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = MarketingBreakdownKind.COUPON_TEMPLATE.name,
          refId = templateId,
          orderCount = row.get(orderModel.id.countDistinct()) ?: 0L,
          benefitCents = row.get(orderModel.couponDiscountCents.sumLong().coalesce(0L)) ?: 0L,
          usageCount = couponRedeemedByDateTemplate[date to templateId] ?: 0L,
        )
    }

    // 满减/首单立减行:订单快照列按 (日, 活动 ID) 分组
    appendSnapshotActivityRows(
      rows = rows,
      window = window,
      orderDate = orderDate,
      activityIdPath = orderModel.promotionActivityId,
      discountPath = orderModel.promotionDiscountCents,
      kind = MarketingBreakdownKind.PROMOTION_ACTIVITY,
    )
    appendSnapshotActivityRows(
      rows = rows,
      window = window,
      orderDate = orderDate,
      activityIdPath = orderModel.firstOrderActivityId,
      discountPath = orderModel.firstOrderDiscountCents,
      kind = MarketingBreakdownKind.FIRST_ORDER_ACTIVITY,
    )

    // 赠品行:order_gifts 按 (日, gift_activity_id) 分组(让利估算在查询侧按当前目录价计算)
    val giftRows =
      queryFactory
        .select(
          giftDate,
          orderGiftModel.giftActivityId,
          orderGiftModel.orderId.countDistinct(),
          orderGiftModel.quantity.sumLong().coalesce(0L),
        ).from(orderGiftModel)
        .where(
          orderGiftModel.createdAt.goe(window.start),
          orderGiftModel.createdAt.lt(window.end),
          orderGiftModel.giftActivityId.isNotNull(),
        ).groupBy(giftDate, orderGiftModel.giftActivityId)
        .fetch()
    giftRows.forEach { row ->
      val date = row.get(giftDate)!!.toLocalDate()
      val refId = row.get(orderGiftModel.giftActivityId) ?: return@forEach
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = MarketingBreakdownKind.GIFT_ACTIVITY.name,
          refId = refId,
          orderCount = row.get(orderGiftModel.orderId.countDistinct()) ?: 0L,
          benefitCents = 0L,
          usageCount = row.get(orderGiftModel.quantity.sumLong().coalesce(0L)) ?: 0L,
        )
    }

    // 票种行:销售(PAID 按 paidAt 回退 createdAt,张数含赠送)+ 核销(USED 按 usedAt),按 (日, ticket_type_id)
    val ticketSalesRows =
      queryFactory
        .select(
          paidDate,
          ticketPurchaseModel.ticketTypeId,
          ticketPurchaseModel.totalCount.sumLong().coalesce(0L),
          ticketPurchaseModel.packagePriceCents.sumLong().coalesce(0L),
        ).from(ticketPurchaseModel)
        .where(
          ticketPurchaseModel.paymentStatus.eq(TicketPurchaseStatus.PAID),
          ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt).goe(window.start),
          ticketPurchaseModel.paidAt.coalesce(ticketPurchaseModel.createdAt).lt(window.end),
        ).groupBy(paidDate, ticketPurchaseModel.ticketTypeId)
        .fetch()
    val ticketRedeemedByDateType =
      queryFactory
        .select(ticketUsedDate, userTicketModel.ticketTypeId, userTicketModel.id.count())
        .from(userTicketModel)
        .where(
          userTicketModel.status.eq(UserTicketStatus.USED),
          userTicketModel.usedAt.goe(window.start),
          userTicketModel.usedAt.lt(window.end),
        ).groupBy(ticketUsedDate, userTicketModel.ticketTypeId)
        .fetch()
        .associate { row ->
          (row.get(ticketUsedDate)!!.toLocalDate() to row.get(userTicketModel.ticketTypeId)!!) to
            row.get(userTicketModel.id.count())!!
        }
    ticketSalesRows.forEach { row ->
      val date = row.get(paidDate)!!.toLocalDate()
      val typeId = row.get(ticketPurchaseModel.ticketTypeId) ?: return@forEach
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = MarketingBreakdownKind.TICKET_TYPE.name,
          refId = typeId,
          orderCount = 0L,
          benefitCents = 0L,
          usageCount = 0L,
          ticketSalesCount = row.get(ticketPurchaseModel.totalCount.sumLong().coalesce(0L)) ?: 0L,
          ticketSalesRevenueCents = row.get(ticketPurchaseModel.packagePriceCents.sumLong().coalesce(0L)) ?: 0L,
          ticketRedeemedCount = ticketRedeemedByDateType[date to typeId] ?: 0L,
        )
    }

    return rows
  }

  /**
   * 满减/首单立减共用:订单快照列(activityId/discount)按 (日, 活动 ID) 分组生成明细行,usage_count = 使用订单数。
   */
  private fun appendSnapshotActivityRows(
    rows: MutableList<DailyMarketingUsageFactModel>,
    window: DateWindow,
    orderDate: DateTemplate<LocalDateTime>,
    activityIdPath: NumberPath<Long>,
    discountPath: NumberPath<Long>,
    kind: MarketingBreakdownKind,
  ) {
    val activityRows =
      queryFactory
        .select(
          orderDate,
          activityIdPath,
          orderModel.id.countDistinct(),
          discountPath.sumLong().coalesce(0L),
        ).from(orderModel)
        .where(
          orderModel.createdAt.goe(window.start),
          orderModel.createdAt.lt(window.end),
          activityIdPath.isNotNull(),
          discountPath.gt(0L),
        ).groupBy(orderDate, activityIdPath)
        .fetch()
    activityRows.forEach { row ->
      val date = row.get(orderDate)!!.toLocalDate()
      val refId = row.get(activityIdPath) ?: return@forEach
      val orderCount = row.get(orderModel.id.countDistinct()) ?: 0L
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = kind.name,
          refId = refId,
          orderCount = orderCount,
          benefitCents = row.get(discountPath.sumLong().coalesce(0L)) ?: 0L,
          usageCount = orderCount,
        )
    }
  }

  /** 按日分组表达式(DATE_TRUNC('day'),Postgres/H2 兼容,与 getDailyOrderStatistics 同模式)。 */
  private fun dayExpr(expr: Expression<LocalDateTime>): DateTemplate<LocalDateTime> =
    Expressions.dateTemplate(LocalDateTime::class.java, "DATE_TRUNC('day', {0})", expr)

  private fun dateRange(
    start: LocalDate,
    end: LocalDate,
  ): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }
      .takeWhile { !it.isAfter(end) }
      .toList()

  override fun findEarliestSourceDate(): LocalDate? {
    val orderEarliest =
      queryFactory
        .select(orderModel.createdAt.min())
        .from(orderModel)
        .fetchOne()
    val userEarliest =
      queryFactory
        .select(userModel.createdAt.min())
        .from(userModel)
        .fetchOne()
    val loginEarliest =
      queryFactory
        .select(userActionLogModel.createdAt.min())
        .from(userActionLogModel)
        .fetchOne()
    val refundEarliest =
      queryFactory
        .select(paymentRefundModel.refundedAt.min())
        .from(paymentRefundModel)
        .fetchOne()

    return listOfNotNull(orderEarliest, userEarliest, loginEarliest, refundEarliest)
      .minOrNull()
      ?.toLocalDate()
  }

  private fun dateWindow(date: LocalDate): DateWindow = DateWindow(start = date.atStartOfDay(), end = date.plusDays(1).atStartOfDay())

  private fun dateWindow(
    startDate: LocalDate,
    endDate: LocalDate,
  ): DateWindow = DateWindow(start = startDate.atStartOfDay(), end = endDate.plusDays(1).atStartOfDay())

  private data class DateWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
  )

  private data class OrderGroupKey(
    val status: String,
    val deliveryWorkerId: Long?,
  )

  private data class OrderCounts(
    val orderCount: Long,
    val amountCents: Long,
    val itemQuantity: Long,
  )
}
