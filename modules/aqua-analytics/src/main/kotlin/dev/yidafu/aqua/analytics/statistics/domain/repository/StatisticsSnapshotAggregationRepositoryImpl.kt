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
import dev.yidafu.aqua.analytics.statistics.dto.MarketingBreakdownKind
import dev.yidafu.aqua.common.domain.model.DailyMarketingFactModel
import dev.yidafu.aqua.common.domain.model.DailyMarketingUsageFactModel
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyRevenueFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.table.OrderGiftsTable
import dev.yidafu.aqua.common.domain.table.OrderItemsTable
import dev.yidafu.aqua.common.domain.table.OrdersTable
import dev.yidafu.aqua.common.domain.table.PaymentRefundsTable
import dev.yidafu.aqua.common.domain.table.TicketPurchasesTable
import dev.yidafu.aqua.common.domain.table.UserActionLogsTable
import dev.yidafu.aqua.common.domain.table.UserCouponsTable
import dev.yidafu.aqua.common.domain.table.UserTicketsTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.coalesce
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 统计快照按日聚合仓储实现(JPA→Exposed 迁移 P3)。
 * 所有聚合均按单日窗口 [date, date+1) 过滤源表,金额按订单只计一次(防 P1-3 虚增)。
 * 源表均为 @SoftDelete 表 —— 每处查询显式过滤 is_deleted=false,与 Hibernate 行为一致。
 * ResultRow 取值全部在事务闭包内(聚合表达式 hashCode 依赖事务上下文)。
 */
@Repository
class StatisticsSnapshotAggregationRepositoryImpl(
  private val database: Database,
) : StatisticsSnapshotAggregationRepository {
  override fun aggregateOrderFacts(date: LocalDate): List<DailyOrderFactModel> {
    val window = dateWindow(date)
    val orderCount = OrdersTable.id.count()
    val amountSum = OrdersTable.totalAmountCents.sum()
    val quantitySum = OrderItemsTable.quantity.sum()

    // 查询 1:仅 orders 聚合 —— 订单数与金额按订单只计一次(不 join order_items,防多商品订单金额重复求和,P1-3)
    val orderRows: MutableMap<OrderGroupKey, OrderCounts> =
      transaction(database) {
        OrdersTable
          .select(OrdersTable.status, OrdersTable.deliveryWorkerId, orderCount, amountSum)
          .andWhere { OrdersTable.createdAt greaterEq window.start and (OrdersTable.createdAt less window.end) }
          .groupBy(OrdersTable.status, OrdersTable.deliveryWorkerId)
          .toList()
          .associate { row ->
            OrderGroupKey(row[OrdersTable.status], row[OrdersTable.deliveryWorkerId]) to
              OrderCounts(row[orderCount], row.longOf(amountSum), 0L)
          }.toMutableMap()
      }

    // 查询 2:订单 ⋈ 明细 —— 商品件数按明细汇总(仅取件数,不与金额混算)
    transaction(database) {
      OrdersTable
        .join(OrderItemsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
        .select(OrdersTable.status, OrdersTable.deliveryWorkerId, quantitySum)
        .where {
          OrdersTable
            .activeOnly { OrdersTable.createdAt greaterEq window.start and (OrdersTable.createdAt less window.end) }
            .and(OrderItemsTable.activeOnly())
        }.groupBy(OrdersTable.status, OrdersTable.deliveryWorkerId)
        .toList()
        .forEach { row ->
          val key = OrderGroupKey(row[OrdersTable.status], row[OrdersTable.deliveryWorkerId])
          val qty = row.longOf(quantitySum)
          val existing = orderRows[key]
          orderRows[key] =
            if (existing != null) {
              existing.copy(itemQuantity = qty)
            } else {
              OrderCounts(0L, 0L, qty) // 有明细但订单聚合缺失(理论不发生:明细必属订单);补零行保证一致性
            }
        }
    }

    return orderRows.map { (key, counts) ->
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
    val quantitySum = OrderItemsTable.quantity.sum()
    val revenueSum = OrderItemsTable.totalPriceCents.sum()
    val distinctOrders = OrdersTable.id.countDistinct()
    return transaction(database) {
      OrdersTable
        .join(OrderItemsTable, JoinType.LEFT, additionalConstraint = { OrderItemsTable.orderId eq OrdersTable.id })
        .select(OrdersTable.status, OrderItemsTable.productId, quantitySum, revenueSum, distinctOrders)
        .where {
          OrdersTable
            .activeOnly { OrdersTable.createdAt greaterEq window.start and (OrdersTable.createdAt less window.end) }
            .and(OrderItemsTable.activeOnly())
        }.groupBy(OrdersTable.status, OrderItemsTable.productId)
        .toList()
        .map { row ->
          DailyProductFactModel(
            statDate = date,
            status = row[OrdersTable.status],
            productId = row[OrderItemsTable.productId],
            quantity = row.longOf(quantitySum),
            revenueCents = row.longOf(revenueSum),
            orderCount = row[distinctOrders],
          )
        }
    }
  }

  override fun aggregateUserFact(date: LocalDate): DailyUserFactModel {
    val window = dateWindow(date)
    val newUserCount =
      transaction(database) {
        UsersTable
          .select(UsersTable.id.count())
          .andWhere { UsersTable.createdAt greaterEq window.start and (UsersTable.createdAt less window.end) }
          .first()[UsersTable.id.count()]
      }
    val loginUserCount =
      transaction(database) {
        UserActionLogsTable
          .select(UserActionLogsTable.userId.countDistinct())
          .where {
            (UserActionLogsTable.actionType eq "USER_LOGIN") and
              (UserActionLogsTable.createdAt greaterEq window.start) and
              (UserActionLogsTable.createdAt less window.end)
          }.first()[UserActionLogsTable.userId.countDistinct()]
      }
    return DailyUserFactModel(statDate = date, newUserCount = newUserCount, loginUserCount = loginUserCount)
  }

  override fun aggregateRefundFacts(date: LocalDate): List<DailyRefundFactModel> {
    val window = dateWindow(date)
    val refundCount = PaymentRefundsTable.id.count()
    val amountSum = PaymentRefundsTable.refundAmountCents.sum()
    return transaction(database) {
      PaymentRefundsTable
        .select(PaymentRefundsTable.status, PaymentRefundsTable.refundType, refundCount, amountSum)
        .andWhere {
          PaymentRefundsTable.processedAt greaterEq window.start and (PaymentRefundsTable.processedAt less window.end)
        }.groupBy(PaymentRefundsTable.status, PaymentRefundsTable.refundType)
        .toList()
        .map { row ->
          DailyRefundFactModel(
            statDate = date,
            status = row[PaymentRefundsTable.status],
            refundType = row[PaymentRefundsTable.refundType],
            refundCount = row[refundCount],
            refundAmountCents = row.longOf(amountSum),
          )
        }
    }
  }

  /**
   * 已完成营收事实:仅 status=COMPLETED 且 completed_at 落在单日窗口 [date, date+1) 的订单。
   * 归属日为完成时间 —— 跨日完成(下单日 ≠ 完成日)的订单营收计入完成日,避免既不在下单日
   * (当时未完成)也不在完成日(created_at 不在窗口)而永久丢失(仪表盘营收趋势为空)。
   * 金额按订单只计一次(orders 单表聚合,P1-3);无完成订单的日期返回空列表(查询侧补零)。
   */
  override fun aggregateRevenueFacts(date: LocalDate): List<DailyRevenueFactModel> {
    val window = dateWindow(date)
    val orderCount = OrdersTable.id.count()
    val amountSum = OrdersTable.totalAmountCents.sum()
    return transaction(database) {
      OrdersTable
        .select(orderCount, amountSum)
        .andWhere {
          (OrdersTable.status eq "COMPLETED") and
            (OrdersTable.completedAt greaterEq window.start) and
            (OrdersTable.completedAt less window.end)
        }.toList()
        .map { row ->
          DailyRevenueFactModel(statDate = date, orderCount = row[orderCount], amountCents = row.longOf(amountSum))
        }
    }.filter { it.orderCount > 0L }
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
   * 空数据日补零(DATE_TRUNC 按日分组,Postgres/H2 兼容)。
   */
  override fun aggregateMarketingFactsRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyMarketingFactModel> {
    require(!startDate.isAfter(endDate)) { "营销事实聚合起止日期非法: $startDate > $endDate" }
    val window = dateWindow(startDate, endDate)
    val orderDate = dateTrunc("day", OrdersTable.createdAt)
    val couponDate = dateTrunc("day", UserCouponsTable.usedAt)
    val paidDate = dateTrunc("day", coalesce(TicketPurchasesTable.paidAt, TicketPurchasesTable.createdAt))
    val ticketUsedDate = dateTrunc("day", UserTicketsTable.usedAt)
    val giftDate = dateTrunc("day", OrderGiftsTable.createdAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val couponSum = OrdersTable.couponDiscountCents.sum()
    val promotionSum = OrdersTable.promotionDiscountCents.sum()
    val firstOrderSum = OrdersTable.firstOrderDiscountCents.sum()
    val couponCount = UserCouponsTable.id.count()
    val salesCount = TicketPurchasesTable.totalCount.sum()
    val salesRevenue = TicketPurchasesTable.packagePriceCents.sum()
    val redeemedCount = UserTicketsTable.id.count()
    val giftQty = OrderGiftsTable.quantity.sum()

    data class OrderAgg(
      val orders: Long,
      val coupon: Long,
      val promotion: Long,
      val firstOrder: Long,
    )

    data class SalesAgg(
      val count: Long,
      val revenue: Long,
    )

    // 订单侧:按日 营销订单数 + 三列优惠金额合计(券/满减/首单可同单,订单数按订单去重)
    val orderByDay: Map<LocalDate, OrderAgg> =
      transaction(database) {
        OrdersTable
          .select(orderDate, distinctOrders, couponSum, promotionSum, firstOrderSum)
          .andWhere {
            (OrdersTable.createdAt greaterEq window.start) and
              (OrdersTable.createdAt less window.end) and
              (
                OrdersTable.couponDiscountCents greater 0L or
                  (OrdersTable.promotionDiscountCents greater 0L) or
                  (OrdersTable.firstOrderDiscountCents greater 0L)
              )
          }.groupBy(orderDate)
          .toList()
          .associate { row ->
            row[orderDate]!!.toLocalDate() to
              OrderAgg(row[distinctOrders], row.longOf(couponSum), row.longOf(promotionSum), row.longOf(firstOrderSum))
          }
      }

    // 用券数:USED 且 used_at 在窗口,按日
    val couponByDay: Map<LocalDate, Long> =
      transaction(database) {
        UserCouponsTable
          .select(couponDate, couponCount)
          .andWhere {
            (UserCouponsTable.status eq UserCouponStatus.USED.name) and
              (UserCouponsTable.usedAt greaterEq window.start) and
              (UserCouponsTable.usedAt less window.end)
          }.groupBy(couponDate)
          .toList()
          .associate { row -> row[couponDate]!!.toLocalDate() to row[couponCount] }
      }

    // 水票销售:PAID 购票按 paidAt(回退 createdAt)归属,按日;张数含赠送(N+M)
    val ticketSalesByDay: Map<LocalDate, SalesAgg> =
      transaction(database) {
        TicketPurchasesTable
          .select(paidDate, salesCount, salesRevenue)
          .andWhere {
            (TicketPurchasesTable.paymentStatus eq TicketPurchaseStatus.PAID.name) and
              (coalesce(TicketPurchasesTable.paidAt, TicketPurchasesTable.createdAt) greaterEq window.start) and
              (coalesce(TicketPurchasesTable.paidAt, TicketPurchasesTable.createdAt) less window.end)
          }.groupBy(paidDate)
          .toList()
          .associate { row -> row[paidDate]!!.toLocalDate() to SalesAgg(row.longOf(salesCount), row.longOf(salesRevenue)) }
      }

    // 水票核销:USED 按 usedAt,按日
    val ticketRedeemedByDay: Map<LocalDate, Long> =
      transaction(database) {
        UserTicketsTable
          .select(ticketUsedDate, redeemedCount)
          .andWhere {
            (UserTicketsTable.status eq UserTicketStatus.USED.name) and
              (UserTicketsTable.usedAt greaterEq window.start) and
              (UserTicketsTable.usedAt less window.end)
          }.groupBy(ticketUsedDate)
          .toList()
          .associate { row -> row[ticketUsedDate]!!.toLocalDate() to row[redeemedCount] }
      }

    // 赠品件数:order_gifts 按 created_at,按日(无软删列)
    val giftByDay: Map<LocalDate, Long> =
      transaction(database) {
        OrderGiftsTable
          .select(giftDate, giftQty)
          .where {
            (OrderGiftsTable.createdAt greaterEq window.start) and
              (OrderGiftsTable.createdAt less window.end)
          }.groupBy(giftDate)
          .toList()
          .associate { row -> row[giftDate]!!.toLocalDate() to row.longOf(giftQty) }
      }

    // 按日补零(与用户事实一致,快照覆盖连续)
    return dateRange(startDate, endDate).map { date ->
      val orderRow = orderByDay[date]
      val salesRow = ticketSalesByDay[date]
      DailyMarketingFactModel(
        statDate = date,
        marketingOrderCount = orderRow?.orders ?: 0L,
        couponBenefitCents = orderRow?.coupon ?: 0L,
        promotionBenefitCents = orderRow?.promotion ?: 0L,
        firstOrderBenefitCents = orderRow?.firstOrder ?: 0L,
        couponRedeemedCount = couponByDay[date] ?: 0L,
        ticketSalesCount = salesRow?.count ?: 0L,
        ticketSalesRevenueCents = salesRow?.revenue ?: 0L,
        ticketRedeemedCount = ticketRedeemedByDay[date] ?: 0L,
        giftQuantity = giftByDay[date] ?: 0L,
      )
    }
  }

  /** 聚合单日营销使用事实(F-STA-007):由范围版本 [aggregateMarketingUsageFactsRange] 取单日。 */
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

    val orderDate = dateTrunc("day", OrdersTable.createdAt)
    val couponUsedDate = dateTrunc("day", UserCouponsTable.usedAt)
    val paidDate = dateTrunc("day", coalesce(TicketPurchasesTable.paidAt, TicketPurchasesTable.createdAt))
    val ticketUsedDate = dateTrunc("day", UserTicketsTable.usedAt)
    val giftDate = dateTrunc("day", OrderGiftsTable.createdAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val couponSum = OrdersTable.couponDiscountCents.sum()
    val couponCount = UserCouponsTable.id.count()
    val salesCount = TicketPurchasesTable.totalCount.sum()
    val salesRevenue = TicketPurchasesTable.packagePriceCents.sum()
    val redeemedCount = UserTicketsTable.id.count()
    val giftOrders = OrderGiftsTable.orderId.countDistinct()
    val giftQty = OrderGiftsTable.quantity.sum()

    // 券行:orders ⋈ user_coupons(orders.coupon_id = user_coupons.id)→ (日, coupon_template_id) 分组
    val couponOrderRows: Map<Pair<LocalDate, Long>, Pair<Long, Long>> =
      transaction(database) {
        OrdersTable
          .join(UserCouponsTable, JoinType.INNER, additionalConstraint = { UserCouponsTable.id eq OrdersTable.couponId })
          .select(orderDate, UserCouponsTable.couponTemplateId, distinctOrders, couponSum)
          .where {
            OrdersTable
              .activeOnly {
                (OrdersTable.createdAt greaterEq window.start) and
                  (OrdersTable.createdAt less window.end) and
                  (OrdersTable.couponDiscountCents greater 0L)
              }.and(UserCouponsTable.activeOnly())
          }.groupBy(orderDate, UserCouponsTable.couponTemplateId)
          .toList()
          .mapNotNull { row ->
            val templateId = row[UserCouponsTable.couponTemplateId] ?: return@mapNotNull null
            (row[orderDate]!!.toLocalDate() to templateId) to Pair(row[distinctOrders], row.longOf(couponSum))
          }.toMap()
      }

    val couponRedeemedByDateTemplate: Map<Pair<LocalDate, Long>, Long> =
      transaction(database) {
        UserCouponsTable
          .select(couponUsedDate, UserCouponsTable.couponTemplateId, couponCount)
          .andWhere {
            (UserCouponsTable.status eq UserCouponStatus.USED.name) and
              (UserCouponsTable.usedAt greaterEq window.start) and
              (UserCouponsTable.usedAt less window.end)
          }.groupBy(couponUsedDate, UserCouponsTable.couponTemplateId)
          .toList()
          .mapNotNull { row ->
            val templateId = row[UserCouponsTable.couponTemplateId] ?: return@mapNotNull null
            (row[couponUsedDate]!!.toLocalDate() to templateId) to row[couponCount]
          }.toMap()
      }

    couponOrderRows.forEach { (dateTemplate, agg) ->
      val (date, templateId) = dateTemplate
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = MarketingBreakdownKind.COUPON_TEMPLATE.name,
          refId = templateId,
          orderCount = agg.first,
          benefitCents = agg.second,
          usageCount = couponRedeemedByDateTemplate[dateTemplate] ?: 0L,
        )
    }

    // 满减/首单立减行:订单快照列按 (日, 活动 ID) 分组
    appendSnapshotActivityRows(
      rows,
      window,
      OrdersTable.promotionActivityId,
      OrdersTable.promotionDiscountCents,
      MarketingBreakdownKind.PROMOTION_ACTIVITY,
    )
    appendSnapshotActivityRows(
      rows,
      window,
      OrdersTable.firstOrderActivityId,
      OrdersTable.firstOrderDiscountCents,
      MarketingBreakdownKind.FIRST_ORDER_ACTIVITY,
    )

    // 赠品行:order_gifts 按 (日, gift_activity_id) 分组(让利估算在查询侧按当前目录价计算;无软删列)
    val giftRows: Map<Pair<LocalDate, Long>, Pair<Long, Long>> =
      transaction(database) {
        OrderGiftsTable
          .select(giftDate, OrderGiftsTable.giftActivityId, giftOrders, giftQty)
          .where {
            (OrderGiftsTable.createdAt greaterEq window.start) and
              (OrderGiftsTable.createdAt less window.end) and
              OrderGiftsTable.giftActivityId.isNotNull()
          }.groupBy(giftDate, OrderGiftsTable.giftActivityId)
          .toList()
          .mapNotNull { row ->
            val activityId = row[OrderGiftsTable.giftActivityId] ?: return@mapNotNull null
            (row[giftDate]!!.toLocalDate() to activityId) to Pair(row[giftOrders], row.longOf(giftQty))
          }.toMap()
      }
    giftRows.forEach { (dateActivity, agg) ->
      val (date, activityId) = dateActivity
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = MarketingBreakdownKind.GIFT_ACTIVITY.name,
          refId = activityId,
          orderCount = agg.first,
          benefitCents = 0L,
          usageCount = agg.second,
        )
    }

    // 票种行:销售(PAID 按 paidAt 回退 createdAt,张数含赠送)+ 核销(USED 按 usedAt),按 (日, ticket_type_id)
    val ticketSalesRows: Map<Pair<LocalDate, Long>, Pair<Long, Long>> =
      transaction(database) {
        TicketPurchasesTable
          .select(paidDate, TicketPurchasesTable.ticketTypeId, salesCount, salesRevenue)
          .andWhere {
            (TicketPurchasesTable.paymentStatus eq TicketPurchaseStatus.PAID.name) and
              (coalesce(TicketPurchasesTable.paidAt, TicketPurchasesTable.createdAt) greaterEq window.start) and
              (coalesce(TicketPurchasesTable.paidAt, TicketPurchasesTable.createdAt) less window.end)
          }.groupBy(paidDate, TicketPurchasesTable.ticketTypeId)
          .toList()
          .mapNotNull { row ->
            val typeId = row[TicketPurchasesTable.ticketTypeId] ?: return@mapNotNull null
            (row[paidDate]!!.toLocalDate() to typeId) to Pair(row.longOf(salesCount), row.longOf(salesRevenue))
          }.toMap()
      }

    val ticketRedeemedByDateType: Map<Pair<LocalDate, Long>, Long> =
      transaction(database) {
        UserTicketsTable
          .select(ticketUsedDate, UserTicketsTable.ticketTypeId, redeemedCount)
          .andWhere {
            (UserTicketsTable.status eq UserTicketStatus.USED.name) and
              (UserTicketsTable.usedAt greaterEq window.start) and
              (UserTicketsTable.usedAt less window.end)
          }.groupBy(ticketUsedDate, UserTicketsTable.ticketTypeId)
          .toList()
          .mapNotNull { row ->
            val typeId = row[UserTicketsTable.ticketTypeId] ?: return@mapNotNull null
            (row[ticketUsedDate]!!.toLocalDate() to typeId) to row[redeemedCount]
          }.toMap()
      }

    ticketSalesRows.forEach { (dateType, agg) ->
      val (date, typeId) = dateType
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = MarketingBreakdownKind.TICKET_TYPE.name,
          refId = typeId,
          orderCount = 0L,
          benefitCents = 0L,
          usageCount = 0L,
          ticketSalesCount = agg.first,
          ticketSalesRevenueCents = agg.second,
          ticketRedeemedCount = ticketRedeemedByDateType[dateType] ?: 0L,
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
    activityIdColumn: org.jetbrains.exposed.v1.core.Column<Long?>,
    discountColumn: org.jetbrains.exposed.v1.core.Column<Long>,
    kind: MarketingBreakdownKind,
  ) {
    val orderDate = dateTrunc("day", OrdersTable.createdAt)
    val distinctOrders = OrdersTable.id.countDistinct()
    val discountSum = discountColumn.sum()
    val activityRows: Map<Pair<LocalDate, Long>, Pair<Long, Long>> =
      transaction(database) {
        OrdersTable
          .select(orderDate, activityIdColumn, distinctOrders, discountSum)
          .andWhere {
            (OrdersTable.createdAt greaterEq window.start) and
              (OrdersTable.createdAt less window.end) and
              activityIdColumn.isNotNull() and
              (discountColumn greater 0L)
          }.groupBy(orderDate, activityIdColumn)
          .toList()
          .mapNotNull { row ->
            val activityId = row[activityIdColumn] ?: return@mapNotNull null
            (row[orderDate]!!.toLocalDate() to activityId) to Pair(row[distinctOrders], row.longOf(discountSum))
          }.toMap()
      }
    activityRows.forEach { (dateActivity, agg) ->
      val (date, refId) = dateActivity
      rows +=
        DailyMarketingUsageFactModel(
          statDate = date,
          kind = kind.name,
          refId = refId,
          orderCount = agg.first,
          benefitCents = agg.second,
          usageCount = agg.first,
        )
    }
  }

  /** 最早源数据日期(orders/users/user_action_logs/payment_refunds 四源最小创建/处理日期);无任何源数据时返回 null。 */
  override fun findEarliestSourceDate(): LocalDate? {
    val orderEarliest =
      transaction(database) {
        OrdersTable.select(OrdersTable.createdAt.min()).first()[OrdersTable.createdAt.min()]
      }
    val userEarliest =
      transaction(database) {
        UsersTable.select(UsersTable.createdAt.min()).first()[UsersTable.createdAt.min()]
      }
    val loginEarliest =
      transaction(database) {
        UserActionLogsTable.select(UserActionLogsTable.createdAt.min()).first()[UserActionLogsTable.createdAt.min()]
      }
    val refundEarliest =
      transaction(database) {
        PaymentRefundsTable
          .select(PaymentRefundsTable.processedAt.min())
          .first()[PaymentRefundsTable.processedAt.min()]
      }

    val candidates: List<LocalDateTime> =
      listOf(orderEarliest, userEarliest, loginEarliest, refundEarliest).filterIsInstance<LocalDateTime>()
    return candidates.minOrNull()?.toLocalDate()
  }

  private fun dateWindow(date: LocalDate): DateWindow = DateWindow(start = date.atStartOfDay(), end = date.plusDays(1).atStartOfDay())

  private fun dateWindow(
    startDate: LocalDate,
    endDate: LocalDate,
  ): DateWindow = DateWindow(start = startDate.atStartOfDay(), end = endDate.plusDays(1).atStartOfDay())

  private fun dateRange(
    start: LocalDate,
    end: LocalDate,
  ): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }
      .takeWhile { !it.isAfter(end) }
      .toList()

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
