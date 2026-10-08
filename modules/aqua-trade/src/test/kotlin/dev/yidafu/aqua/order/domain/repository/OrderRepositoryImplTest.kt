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

import com.querydsl.core.Tuple
import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import com.querydsl.jpa.impl.JPAUpdateClause
import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 订单仓储 QueryDSL 实现单元测试:构造器 mock 拦截查询工厂,覆盖过滤/分页/统计/批量更新/分析。
 */
class OrderRepositoryImplTest :
  FunSpec({
    var selectQuery: JPAQuery<OrderModel> = mockk()
    var countQuery: JPAQuery<Void> = mockk()
    var exprQuery: JPAQuery<Long> = mockk()
    var analyticsQuery: JPAQuery<Tuple> = mockk()
    var updateClause: JPAUpdateClause = mockk()
    var repo: OrderRepositoryImpl = mockk()
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)

    fun order(
      id: Long,
      status: OrderModelStatus = OrderModelStatus.PENDING_DISPATCH,
      amountCents: Long = 1000,
    ) = OrderModel(id = id, orderNo = "ORD-$id", userId = 100L, amountCents = amountCents, status = status)

    // ================================================================
    // findOrdersWithFilters
    // ================================================================

    // ================================================================
    // findDeliveryWorkerOrdersWithFilters
    // ================================================================

    // ================================================================
    // countOrdersWithFilters
    // ================================================================

    // ================================================================
    // findOrdersPaginated
    // ================================================================

    // ================================================================
    // findByDeliveryWorkerIdAndStatusIn
    // ================================================================

    // ================================================================
    // findUserOrdersPaginated
    // ================================================================

    // ================================================================
    // bulkUpdateOrderStatus
    // ================================================================

    // ================================================================
    // getOrderAnalytics
    // ================================================================

    // ================================================================
    // countOrdersByDateRange / sumAmountCentsByStatusAndDateRange
    // ================================================================

    // ================================================================
    // countAndSumByPaymentType
    // ================================================================

    beforeTest {
      mockkConstructor(JPAQueryFactory::class)
      selectQuery = mockk(relaxed = true)
      countQuery = mockk(relaxed = true)
      exprQuery = mockk(relaxed = true)
      analyticsQuery = mockk(relaxed = true)
      updateClause = mockk(relaxed = true)
      repo = OrderRepositoryImpl()
      val em = mockk<EntityManager>(relaxed = true)
      val field = OrderRepositoryImpl::class.java.getDeclaredField("entityManager")
      field.isAccessible = true
      field.set(repo, em)

      every { anyConstructed<JPAQueryFactory>().selectFrom(any<EntityPath<OrderModel>>()) } returns selectQuery
      every { anyConstructed<JPAQueryFactory>().query() } returns countQuery
      every { anyConstructed<JPAQueryFactory>().select(any<Expression<Long>>()) } returns exprQuery
      every { anyConstructed<JPAQueryFactory>().update(any<EntityPath<OrderModel>>()) } returns updateClause
      every { selectQuery.where(any<Predicate>()) } returns selectQuery
      every { selectQuery.orderBy(any<OrderSpecifier<*>>()) } returns selectQuery
      every { selectQuery.orderBy(any<OrderSpecifier<*>>(), any<OrderSpecifier<*>>()) } returns selectQuery
      every { selectQuery.offset(any()) } returns selectQuery
      every { selectQuery.limit(any()) } returns selectQuery
      every { countQuery.from(any()) } returns countQuery
      every { countQuery.where(any<Predicate>()) } returns countQuery
      every { countQuery.select(any<Expression<Long>>()) } returns exprQuery
      every { exprQuery.from(any()) } returns exprQuery
      every { exprQuery.where(any<Predicate>()) } returns exprQuery
      every { updateClause.set(any<Path<Any>>(), any<Any>()) } returns updateClause
      every { updateClause.where(any<Predicate>()) } returns updateClause
    }
    afterTest {
      unmockkConstructor(JPAQueryFactory::class)
    }
    test("findOrdersWithFilters applies all filters") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders

      val result =
        repo.findOrdersWithFilters(
          userId = 100L,
          status = OrderModelStatus.PENDING_DISPATCH,
          deliveryWorkerId = 5L,
          startDate = start,
          endDate = end,
          orderNumber = "ORD-1",
          statuses = listOf(OrderModelStatus.PENDING_DISPATCH),
        )

      result shouldBe orders
    }
    test("findOrdersWithFilters handles null filters") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders

      repo.findOrdersWithFilters(null, null, null, null, null, null, null) shouldBe orders
    }
    test("findDeliveryWorkerOrdersWithFilters fetches base") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders

      repo.findDeliveryWorkerOrdersWithFilters(5L, OrderModelStatus.DELIVERING, null, null, null) shouldBe orders
    }
    test("findDeliveryWorkerOrdersWithFilters applies date and limit") {
      val orders = listOf(order(1L), order(2L))
      every { selectQuery.fetch() } returns orders

      repo.findDeliveryWorkerOrdersWithFilters(5L, OrderModelStatus.DELIVERING, start, end, 10) shouldBe orders
    }
    test("countOrdersWithFilters counts") {
      every { countQuery.fetchCount() } returnsMany listOf(7L, 0L)

      repo.countOrdersWithFilters(
        100L,
        OrderModelStatus.PENDING_DISPATCH,
        5L,
        start,
        end,
        listOf(OrderModelStatus.PENDING_DISPATCH),
      ) shouldBe
        7L
      repo.countOrdersWithFilters(null, null, null, null, null, null) shouldBe 0L
    }
    test("findOrdersPaginated default sort") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      // total 需 ≥ offset+pageSize,否则 Spring Data 4 PageImpl 会按 offset+content.size 重算
      every { countQuery.fetchCount() } returns 15L

      val page = repo.findOrdersPaginated("kw", OrderModelStatus.COMPLETED, 100L, 5L, start, end, 100L, 10000L, 0, 10, "createdAt", "desc")

      page.content shouldBe orders
      page.totalElements shouldBe 15L
      page.size shouldBe 10
    }
    test("findOrdersPaginated sorts by total amount asc") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L

      val page = repo.findOrdersPaginated(null, null, null, null, null, null, null, null, 1, 5, "totalAmount", "asc")

      page.content shouldBe orders
      page.sort.isSorted shouldBe true
    }
    test("findOrdersPaginated sorts by updated at") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L

      val page = repo.findOrdersPaginated(null, null, null, null, null, null, null, null, 0, 20, "updated_at", "asc")

      page.content shouldBe orders
      page.totalElements shouldBe 1L
    }
    test("findByDeliveryWorkerIdAndStatusIn with worker") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L

      val page = repo.findByDeliveryWorkerIdAndStatusIn(5L, listOf(OrderModelStatus.COMPLETED), "kw", 0, 10)

      page.content shouldBe orders
      page.totalElements shouldBe 1L
    }
    test("findByDeliveryWorkerIdAndStatusIn without worker") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L

      val page = repo.findByDeliveryWorkerIdAndStatusIn(null, listOf(OrderModelStatus.CANCELLED), null, 0, 10)

      page.content shouldBe orders
    }
    test("findUserOrdersPaginated with statuses") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L

      val page = repo.findUserOrdersPaginated(100L, listOf(OrderModelStatus.COMPLETED), 0, 10)

      page.content shouldBe orders
    }
    test("findUserOrdersPaginated with null statuses") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L

      val page = repo.findUserOrdersPaginated(100L, null, 0, 10)
      val page2 = repo.findUserOrdersPaginated(100L, emptyList(), 0, 10)

      page.content shouldBe orders
      page2.content shouldBe orders
    }
    test("bulkUpdateOrderStatus updates status") {
      every { updateClause.execute() } returns 3L

      repo.bulkUpdateOrderStatus(listOf(1L, 2L, 3L), OrderModelStatus.CANCELLED, null) shouldBe 3
    }
    test("bulkUpdateOrderStatus with worker") {
      every { updateClause.execute() } returns 2L

      repo.bulkUpdateOrderStatus(listOf(1L, 2L), OrderModelStatus.DELIVERING, 5L) shouldBe 2
    }
    test("getOrderAnalytics aggregates rows") {
      val tuple = mockk<Tuple>()
      every {
        tuple.get(any<Expression<*>>())
      } answers {
        when (firstArg<Expression<*>>().type) {
          LocalDate::class.java -> LocalDate.of(2025, 1, 1)
          OrderModelStatus::class.java -> OrderModelStatus.COMPLETED
          Double::class.javaObjectType -> 4.5
          else -> 10L
        }
      }
      every { analyticsQuery.fetch() } returns listOf(tuple)
      every { anyConstructed<JPAQueryFactory>().select(any(), any(), any(), any(), any(), any(), any()) } returns analyticsQuery
      every { analyticsQuery.from(any()) } returns analyticsQuery
      every { analyticsQuery.where(any<Predicate>()) } returns analyticsQuery
      every { analyticsQuery.groupBy(any(), any()) } returns analyticsQuery
      every { analyticsQuery.orderBy(any<OrderSpecifier<*>>(), any<OrderSpecifier<*>>()) } returns analyticsQuery

      val rows = repo.getOrderAnalytics(start, end)

      rows.size shouldBe 1
      rows[0].orderDate shouldBe LocalDate.of(2025, 1, 1)
      rows[0].status shouldBe OrderModelStatus.COMPLETED
      rows[0].orderCount shouldBe 10L
      rows[0].totalRevenueCents shouldBe 10L
      rows[0].uniqueCustomers shouldBe 10L
      rows[0].activeWorkers shouldBe 10L
    }
    test("countOrdersByDateRange counts with filters") {
      every { exprQuery.fetchOne() } returnsMany listOf(4L, 4L, null)
      repo.countOrdersByDateRange(start, end, 5L, listOf(OrderModelStatus.COMPLETED)) shouldBe 4L
      repo.countOrdersByDateRange(start, end, null, null) shouldBe 4L

      repo.countOrdersByDateRange(start, end, 5L, null) shouldBe 0L
    }
    test("sumAmountCentsByStatusAndDateRange sums") {
      every { exprQuery.fetchOne() } returnsMany listOf(9000L, 9000L, null)
      repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 9000L
      repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, null) shouldBe 9000L

      repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 0L
    }
    test("countByStatusAndCreatedAtBetween counts with filters") {
      // 后台首页 orderStatistics:严格开区间 (startDate, endDate),状态/配送员可空过滤
      every { exprQuery.fetchOne() } returnsMany listOf(4L, 2L, null)
      repo.countByStatusAndCreatedAtBetween(start, end, 5L, listOf(OrderModelStatus.COMPLETED)) shouldBe 4L
      repo.countByStatusAndCreatedAtBetween(start, end, null, null) shouldBe 2L

      repo.countByStatusAndCreatedAtBetween(start, end, 5L, listOf(OrderModelStatus.CANCELLED)) shouldBe 0L
    }
    test("sumAmountCentsByStatusAndCreatedAtBetween sums") {
      every { exprQuery.fetchOne() } returnsMany listOf(9499L, 3500L, null)
      repo.sumAmountCentsByStatusAndCreatedAtBetween(
        listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED),
        start,
        end,
        null,
      ) shouldBe 9499L
      repo.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 3500L

      repo.sumAmountCentsByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 0L
    }
    test("countByStatusAndCompletedAtBetween counts with filters (窗口按完成时间)") {
      // 已完成口径:窗口按 completed_at(完成时间),跨日完成订单计入完成日
      every { exprQuery.fetchOne() } returnsMany listOf(4L, 2L, null)
      repo.countByStatusAndCompletedAtBetween(start, end, 5L, listOf(OrderModelStatus.COMPLETED)) shouldBe 4L
      repo.countByStatusAndCompletedAtBetween(start, end, null, null) shouldBe 2L

      repo.countByStatusAndCompletedAtBetween(start, end, 5L, listOf(OrderModelStatus.CANCELLED)) shouldBe 0L
    }
    test("sumAmountCentsByStatusAndCompletedAtBetween sums (窗口按完成时间)") {
      every { exprQuery.fetchOne() } returnsMany listOf(9499L, 3500L, null)
      repo.sumAmountCentsByStatusAndCompletedAtBetween(
        listOf(OrderModelStatus.COMPLETED, OrderModelStatus.CANCELLED),
        start,
        end,
        null,
      ) shouldBe 9499L
      repo.sumAmountCentsByStatusAndCompletedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 3500L

      repo.sumAmountCentsByStatusAndCompletedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end, 5L) shouldBe 0L
    }
    test("findByCreatedAtBetween fetches in window") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders

      repo.findByCreatedAtBetween(start, end) shouldBe orders
    }
    test("findByStatusAndCreatedAtBetween fetches in window by status") {
      val orders = listOf(order(1L, status = OrderModelStatus.COMPLETED))
      every { selectQuery.fetch() } returns orders

      repo.findByStatusAndCreatedAtBetween(listOf(OrderModelStatus.COMPLETED), start, end) shouldBe orders
    }
    test("countAndSumByPaymentType returns count and sum") {
      every { exprQuery.fetchOne() } returnsMany listOf(2L, 2500L)

      val result = repo.countAndSumByPaymentType(5L, OrderModelStatus.COMPLETED, PaymentType.QR_CODE, start, end)

      result[0] shouldBe 2L
      result[1] shouldBe 2500L
    }
    test("countAndSumByPaymentType handles null fetch") {
      every { exprQuery.fetchOne() } returnsMany listOf(null, null)

      val result = repo.countAndSumByPaymentType(5L, OrderModelStatus.COMPLETED, PaymentType.CASH, start, end)

      result[0] shouldBe 0L
      result[1] shouldBe 0L
    }

    // ================================================================
    // 「已送达」口径聚合(R-ORD-11:送水工业绩/收款对账,含 待确认收货 + 已完成,按送达时间)
    // ================================================================

    test("countDeliveredByDateRange counts delivered orders") {
      every { exprQuery.fetchOne() } returns 4L

      repo.countDeliveredByDateRange(start, end, 5L) shouldBe 4L
      repo.countDeliveredByDateRange(start, end, null) shouldBe 4L
    }
    test("countDeliveredByDateRange handles null fetch") {
      every { exprQuery.fetchOne() } returns null

      repo.countDeliveredByDateRange(start, end, 5L) shouldBe 0L
    }
    test("sumDeliveredAmountCentsByDateRange sums delivered orders") {
      every { exprQuery.fetchOne() } returns 9000L

      repo.sumDeliveredAmountCentsByDateRange(start, end, 5L) shouldBe 9000L
    }
    test("countAndSumDeliveredByPaymentType returns count and sum") {
      every { exprQuery.fetchOne() } returnsMany listOf(2L, 2500L)

      val result = repo.countAndSumDeliveredByPaymentType(5L, PaymentType.CASH, start, end)

      result[0] shouldBe 2L
      result[1] shouldBe 2500L
    }
    test("countOrdersByUserIdGroupByStatus groups by status") {
      // G31: 我的页订单角标 —— select(status, count()) group by status → Map
      val tuple = mockk<Tuple>()
      val tupleQuery = mockk<JPAQuery<Tuple>>(relaxed = true)
      every { tuple.get(any<Expression<*>>()) } returnsMany listOf(OrderModelStatus.PENDING_PAYMENT, 2L)
      every { tupleQuery.fetch() } returns listOf(tuple)
      every { anyConstructed<JPAQueryFactory>().select(any(), any()) } returns tupleQuery
      every { tupleQuery.from(any()) } returns tupleQuery
      every { tupleQuery.where(any<Predicate>()) } returns tupleQuery
      every { tupleQuery.groupBy(any()) } returns tupleQuery

      val result = repo.countOrdersByUserIdGroupByStatus(100L)

      result shouldBe mapOf(OrderModelStatus.PENDING_PAYMENT to 2L)
    }

    // ================================================================
    // 营销使用情况(PRD 10-marketing.md 营销方式详情)
    // ================================================================

    test("findOrdersByMarketingUsage by promotion activity") {
      val orders = listOf(order(1L), order(2L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 2L
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, 5L)

      val page = repo.findOrdersByMarketingUsage(criteria, 0, 20)

      page.content shouldBe orders
      page.totalElements shouldBe 2L
    }
    test("findOrdersByMarketingUsage by flash sale item match") {
      val orders = listOf(order(1L))
      every { selectQuery.fetch() } returns orders
      every { countQuery.fetchCount() } returns 1L
      val criteria =
        MarketingUsageOrderCriteria(
          kind = MarketingUsageKind.FLASH_SALE_ITEM,
          refId = 3L,
          productId = 9L,
          unitPriceCents = 1500L,
          validFrom = start,
          validTo = end,
        )

      val page = repo.findOrdersByMarketingUsage(criteria, 0, 20)

      page.content shouldBe orders
      page.totalElements shouldBe 1L
    }
    test("countOrdersByMarketingUsage counts") {
      every { countQuery.fetchCount() } returns 6L
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.COUPON_TEMPLATE, 1L)

      repo.countOrdersByMarketingUsage(criteria) shouldBe 6L
    }
    test("sumMarketingDiscountCents sums per kind") {
      every { exprQuery.fetchOne() } returnsMany listOf(1500L, 0L)

      repo.sumMarketingDiscountCents(MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, 1L)) shouldBe 1500L
      // 限时特价/新人价/赠品无订单列优惠快照,返回 0
      repo.sumMarketingDiscountCents(MarketingUsageOrderCriteria(MarketingUsageKind.FLASH_SALE_ITEM, 2L)) shouldBe 0L
    }
    test("aggregateMarketingUsageItems for gift activity") {
      val tuple = mockk<Tuple>()
      val tupleQuery = mockk<JPAQuery<Tuple>>(relaxed = true)
      every { tuple.get(any<Expression<*>>()) } returnsMany listOf(8L, 5L)
      every { tupleQuery.fetch() } returns listOf(tuple)
      every { anyConstructed<JPAQueryFactory>().select(any(), any()) } returns tupleQuery
      every { tupleQuery.from(any()) } returns tupleQuery
      every { tupleQuery.where(any<Predicate>()) } returns tupleQuery
      every { tupleQuery.groupBy(any()) } returns tupleQuery

      val result = repo.aggregateMarketingUsageItems(MarketingUsageOrderCriteria(MarketingUsageKind.GIFT_ACTIVITY, 4L))

      result.size shouldBe 1
      result[0].productId shouldBe 8L
      result[0].unitPriceCents shouldBe null
      result[0].quantitySum shouldBe 5L
    }
    test("aggregateMarketingUsageItems for flash sale item") {
      val tuple = mockk<Tuple>()
      val tupleQuery = mockk<JPAQuery<Tuple>>(relaxed = true)
      every { tuple.get(any<Expression<*>>()) } returnsMany listOf(9L, 1500L, 3L)
      every { tupleQuery.fetch() } returns listOf(tuple)
      every { anyConstructed<JPAQueryFactory>().select(any(), any(), any()) } returns tupleQuery
      every { tupleQuery.from(any()) } returns tupleQuery
      every { tupleQuery.where(any<Predicate>()) } returns tupleQuery
      every { tupleQuery.where(any(), any(), any()) } returns tupleQuery
      every { tupleQuery.groupBy(any(), any()) } returns tupleQuery
      val criteria =
        MarketingUsageOrderCriteria(
          kind = MarketingUsageKind.FLASH_SALE_ITEM,
          refId = 3L,
          productId = 9L,
          unitPriceCents = 1500L,
          validFrom = start,
          validTo = end,
        )

      val result = repo.aggregateMarketingUsageItems(criteria)

      result.size shouldBe 1
      result[0].productId shouldBe 9L
      result[0].unitPriceCents shouldBe 1500L
      result[0].quantitySum shouldBe 3L
    }
  })
