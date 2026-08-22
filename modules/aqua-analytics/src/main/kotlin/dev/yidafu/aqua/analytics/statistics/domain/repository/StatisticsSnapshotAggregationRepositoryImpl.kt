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

import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.DailyOrderFactModel
import dev.yidafu.aqua.common.domain.model.DailyProductFactModel
import dev.yidafu.aqua.common.domain.model.DailyRefundFactModel
import dev.yidafu.aqua.common.domain.model.DailyUserFactModel
import dev.yidafu.aqua.common.domain.model.QOrderItemModel.Companion.orderItemModel
import dev.yidafu.aqua.common.domain.model.QOrderModel.Companion.orderModel
import dev.yidafu.aqua.common.domain.model.QPaymentRefundModel.Companion.paymentRefundModel
import dev.yidafu.aqua.common.domain.model.QUserActionLogModel.Companion.userActionLogModel
import dev.yidafu.aqua.common.domain.model.QUserModel.Companion.userModel
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
