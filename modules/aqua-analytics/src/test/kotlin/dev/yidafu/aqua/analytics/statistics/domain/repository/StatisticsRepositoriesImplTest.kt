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

import dev.yidafu.aqua.analytics.common.exposed.AnalyticsPgTestSupport
import dev.yidafu.aqua.analytics.common.exposed.Seed
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.table.PaymentRefundsTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 统计快照聚合与各统计仓储实现的 真实 PG 集成测试(JPA→Exposed 迁移 P3)。
 * 重点验证:聚合口径与原 QueryDSL 版一致 + @SoftDelete 过滤语义(is_deleted=false)。
 */
class StatisticsSnapshotAggregationRepositoryImplTest :
  FunSpec({
    val repo = StatisticsSnapshotAggregationRepositoryImpl(AnalyticsPgTestSupport.database)
    val date = LocalDate.of(2026, 10, 1)

    beforeTest { AnalyticsPgTestSupport.reset() }

    test("aggregateOrderFacts 按 status/worker 分组,金额按订单计一次,软删订单不计入") {
      val base = date.atStartOfDay()
      val o1 = Seed.order(status = "COMPLETED", amountCents = 100L, createdAt = base.plusHours(1))
      Seed.orderItem(o1, quantity = 3, totalPriceCents = 90L)
      Seed.order(status = "COMPLETED", amountCents = 200L, createdAt = base.plusHours(2))
      // 软删订单不应计入
      Seed.order(status = "COMPLETED", amountCents = 999L, createdAt = base.plusHours(3), deleted = true)

      val facts = repo.aggregateOrderFacts(date)
      facts shouldHaveSize 1
      val fact = facts.first()
      fact.status shouldBe "COMPLETED"
      fact.orderCount shouldBe 2L
      fact.amountCents shouldBe 300L
      fact.itemQuantity shouldBe 3L
    }

    test("aggregateProductFacts 按 (status, product) 分组") {
      val base = date.atStartOfDay()
      val o1 = Seed.order(createdAt = base.plusHours(1))
      Seed.orderItem(o1, productId = 5L, quantity = 2, totalPriceCents = 200L)
      Seed.orderItem(o1, productId = 6L, quantity = 1, totalPriceCents = 100L)

      val facts = repo.aggregateProductFacts(date)
      facts.sumOf { it.quantity } shouldBe 3L
      facts.sumOf { it.revenueCents } shouldBe 300L
      facts.map { it.productId }.toSet() shouldBe setOf(5L, 6L)
    }

    test("aggregateUserFact 新增用户数与登录数") {
      val base = date.atStartOfDay()
      Seed.user(createdAt = base.plusHours(1))
      Seed.user(createdAt = base.plusHours(2))
      Seed.user(createdAt = base.minusDays(1))
      Seed.userActionLog(userId = "7", actionType = "USER_LOGIN", createdAt = base.plusHours(1))
      Seed.userActionLog(userId = "7", actionType = "USER_LOGIN", createdAt = base.plusHours(5))

      val fact = repo.aggregateUserFact(date)
      fact.newUserCount shouldBe 2L
      fact.loginUserCount shouldBe 1L // 同一用户去重
    }

    test("aggregateRefundFacts 按 (status, refundType) 分组") {
      val base = date.atStartOfDay()
      PaymentRefundSeed.refund(amount = 100L, status = "SUCCESS", refundType = "FULL", refundedAt = base.plusHours(1))
      PaymentRefundSeed.refund(amount = 50L, status = "SUCCESS", refundType = "PARTIAL", refundedAt = base.plusHours(2))

      val facts = repo.aggregateRefundFacts(date)
      facts shouldHaveSize 2
      facts.sumOf { it.refundAmountCents } shouldBe 150L
    }

    test("aggregateRevenueFacts 仅计 COMPLETED 且按 completed_at 归属,空日返回空列表") {
      val base = date.atStartOfDay()
      Seed.order(status = "COMPLETED", amountCents = 300L, completedAt = base.plusHours(2))
      Seed.order(status = "DELIVERING", amountCents = 400L, createdAt = base.plusHours(1))

      repo.aggregateRevenueFacts(date).let { facts ->
        facts shouldHaveSize 1
        facts.first().amountCents shouldBe 300L
        facts.first().orderCount shouldBe 1L
      }
      // 空日
      repo.aggregateRevenueFacts(date.plusDays(5)) shouldHaveSize 0
    }

    test("aggregateMarketingFactsRange 按日补零,金额按日归属") {
      val base = date.atStartOfDay()
      Seed.order(
        createdAt = base.plusHours(1),
        couponId = Seed.id(),
        couponDiscountCents = 100L,
      )
      val start = LocalDate.of(2026, 9, 30)

      val facts = repo.aggregateMarketingFactsRange(start, date)
      facts shouldHaveSize 2
      facts.first().couponBenefitCents shouldBe 0L // 9/30 补零
      facts.last().couponBenefitCents shouldBe 100L
      facts.last().marketingOrderCount shouldBe 1L
    }
  })

/** 退款种子(独立 object 避免与 Seed.order 混用)。 */
private object PaymentRefundSeed {
  fun refund(
    amount: Long,
    status: String,
    refundType: String,
    refundedAt: LocalDateTime?,
  ): Long {
    val id = Seed.id()
    transaction(AnalyticsPgTestSupport.database) {
      dev.yidafu.aqua.common.domain.table.PaymentRefundsTable.insert {
        it[PaymentRefundsTable.id] = id
        it[paymentId] = 1L
        it[orderId] = 1L
        it[userId] = 1L
        it[refundAmountCents] = amount
        it[PaymentRefundsTable.status] = status
        it[PaymentRefundsTable.refundType] = refundType
        it[PaymentRefundsTable.processedAt] = refundedAt
        it[isDeleted] = false
        it[createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
      }
    }
    return id
  }
}

class StatisticsOrderRepositoryImplTest :
  FunSpec({
    val repo = StatisticsOrderRepositoryImpl(AnalyticsPgTestSupport.database)
    val base = LocalDateTime.of(2026, 10, 1, 0, 0)

    beforeTest { AnalyticsPgTestSupport.reset() }

    test("getDailyOrderStatistics 金额按订单一次、件数按明细,软删不计入") {
      val o1 = Seed.order(status = "COMPLETED", amountCents = 100L, createdAt = base.plusHours(5))
      Seed.orderItem(o1, quantity = 4)
      Seed.order(
        status = "COMPLETED",
        amountCents = 50L,
        createdAt = base.plusHours(7),
        deleted = true,
      )

      val rows = repo.getDailyOrderStatistics(base, base.plusDays(1))
      rows shouldHaveSize 1
      rows.first().orderCount shouldBe 1L
      rows.first().totalAmountCents shouldBe 100L
      rows.first().orderProductCount shouldBe 4L
    }

    test("getDailyRevenueStatistics 按 completed_at 归属完成日") {
      Seed.order(
        status = "COMPLETED",
        amountCents = 300L,
        createdAt = base.minusDays(2),
        completedAt = base.plusHours(3),
      )

      val rows = repo.getDailyRevenueStatistics(base, base.plusDays(1))
      rows shouldHaveSize 1
      rows.first().date shouldBe base.toLocalDate()
      rows.first().totalAmountCents shouldBe 300L
    }

    test("countByStatusIn 按状态计数") {
      Seed.order(status = "COMPLETED")
      Seed.order(status = "COMPLETED")
      Seed.order(status = "DELIVERING")

      val map = repo.countByStatusIn(listOf(OrderModelStatus.COMPLETED, OrderModelStatus.DELIVERING))
      map[OrderModelStatus.COMPLETED] shouldBe 2L
      map[OrderModelStatus.DELIVERING] shouldBe 1L
    }

    test("countOrdersGroupByHour 与 countOrdersPerUserInRange") {
      Seed.order(userId = 1L, createdAt = base.plusHours(9))
      Seed.order(userId = 1L, createdAt = base.plusHours(9))
      Seed.order(userId = 2L, createdAt = base.plusHours(10))

      val byHour = repo.countOrdersGroupByHour(base, base.plusDays(1))
      byHour[9] shouldBe 2L
      byHour[10] shouldBe 1L

      val byUser = repo.countOrdersPerUserInRange(base, base.plusDays(1))
      byUser[1L] shouldBe 2L
      byUser[2L] shouldBe 1L
    }

    test("countOrdersByAmountBucket 按金额分桶") {
      Seed.order(amountCents = 1500L, createdAt = base.plusHours(1)) // 0-2000
      Seed.order(amountCents = 3000L, createdAt = base.plusHours(2)) // 2000-5000
      Seed.order(amountCents = 8000L, createdAt = base.plusHours(3)) // 5000+

      val buckets =
        listOf(
          dev.yidafu.aqua.analytics.statistics.dto
            .OrderValueBucketDTO(label = "小单", fromCents = 0L, toCents = 2000L),
          dev.yidafu.aqua.analytics.statistics.dto
            .OrderValueBucketDTO(label = "中单", fromCents = 2000L, toCents = 5000L),
          dev.yidafu.aqua.analytics.statistics.dto
            .OrderValueBucketDTO(label = "大单", fromCents = 5000L, toCents = null),
        )
      val map = repo.countOrdersByAmountBucket(base, base.plusDays(1), buckets)
      map["小单"] shouldBe 1L
      map["中单"] shouldBe 1L
      map["大单"] shouldBe 1L
    }
  })

class ProductSalesStatisticsRepositoryImplTest :
  FunSpec({
    val repo = ProductSalesStatisticsRepositoryImpl(AnalyticsPgTestSupport.database)
    val base = LocalDateTime.of(2026, 10, 1, 0, 0)

    beforeTest { AnalyticsPgTestSupport.reset() }

    test("getProductDailySales 仅计 COMPLETED 订单明细") {
      val productId = Seed.product(name = "山泉12L")
      val o1 = Seed.order(status = "COMPLETED", createdAt = base.plusHours(5))
      Seed.orderItem(o1, productId = productId, quantity = 3, totalPriceCents = 3600L, createdAt = base.plusHours(5))
      val o2 = Seed.order(status = "DELIVERING", createdAt = base.plusHours(6))
      Seed.orderItem(o2, productId = productId, quantity = 9, totalPriceCents = 9000L)

      val rows = repo.getProductDailySales(base, base.plusDays(1))
      rows shouldHaveSize 1
      rows.first().salesVolume shouldBe 3L
      rows.first().revenue shouldBe 3600L
      rows.first().productName shouldBe "山泉12L"
    }

    test("countProductsByStatus / aggregateProductValue / countLowStockProducts") {
      Seed.product(status = "ONLINE", stock = 5, sales = 100, priceCents = 1000L)
      Seed.product(status = "OFFLINE", stock = 2, sales = 50, priceCents = 2000L)
      Seed.product(status = "ONLINE", stock = 90, sales = 10)

      repo.countProductsByStatus()[dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus.ONLINE] shouldBe 2L
      val value = repo.aggregateProductValue()
      value.totalProducts shouldBe 3L
      value.totalSales shouldBe 160L
      repo.countLowStockProducts(10) shouldBe 2L
      repo.findLowStockProducts(10, 1) shouldHaveSize 1
    }

    test("getProductSalesRanking 按销量降序") {
      val p1 = Seed.product(name = "A")
      val p2 = Seed.product(name = "B")
      val o1 = Seed.order(status = "COMPLETED")
      Seed.orderItem(o1, productId = p1, quantity = 5)
      Seed.orderItem(o1, productId = p2, quantity = 2)

      val ranking = repo.getProductSalesRanking(10)
      ranking.first().productId shouldBe p1
      ranking.first().salesVolume shouldBe 5L
    }
  })

class StatisticsDeliveryWorkerRepositoryImplTest :
  FunSpec({
    val repo = StatisticsDeliveryWorkerRepositoryImpl(AnalyticsPgTestSupport.database)
    val base = LocalDateTime.of(2026, 10, 1, 0, 0)

    beforeTest { AnalyticsPgTestSupport.reset() }

    test("countTodayActiveWorkers 只统计当日有单且角色具备配送能力的账号") {
      val workerId = Seed.admin(role = "DELIVERY_WORKER")
      Seed.admin(role = "USER_ADMIN")
      Seed.order(deliveryWorkerId = workerId, createdAt = base.plusHours(5))

      repo.countTodayActiveWorkers(base) shouldBe 1L
    }

    test("countTodayCompletedOrdersByWorker 按 worker 分组计数") {
      val w1 = Seed.admin()
      Seed.order(deliveryWorkerId = w1, status = "COMPLETED", createdAt = base.plusHours(5))
      Seed.order(deliveryWorkerId = w1, status = "COMPLETED", createdAt = base.plusHours(7), deleted = true)

      val map = repo.countTodayCompletedOrdersByWorker(base)
      map[w1] shouldBe 1L // 软删订单不计入;AWAITING_CONFIRMATION 超 orders.status DDL varchar(20)(存量限制),此处仅用 COMPLETED 验证分组
    }
  })

class StatisticsUserRepositoryImplTest :
  FunSpec({
    val repo = StatisticsUserRepositoryImpl(AnalyticsPgTestSupport.database)
    val base = LocalDateTime.of(2026, 10, 1, 0, 0)

    beforeTest { AnalyticsPgTestSupport.reset() }

    test("countDailyNewUsers 按日分组,软删用户不计入") {
      Seed.user(createdAt = base.plusHours(1))
      Seed.user(createdAt = base.plusHours(2))
      Seed.user(createdAt = base.plusHours(3), deleted = true)

      val map = repo.countDailyNewUsers(base, base.plusDays(1))
      map[base.toLocalDate()] shouldBe 2L
    }

    test("countActiveUsersSince 统计窗口内下过单的用户") {
      val u1 = Seed.user()
      Seed.user()
      Seed.order(userId = u1, createdAt = base.plusHours(5))

      repo.countActiveUsersSince(base) shouldBe 1L
      repo.countUsersCreatedAfter(base) shouldBe 2L
    }
  })
