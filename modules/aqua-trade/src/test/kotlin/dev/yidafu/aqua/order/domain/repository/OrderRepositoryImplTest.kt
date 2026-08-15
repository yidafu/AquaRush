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
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 订单仓储 QueryDSL 实现单元测试:构造器 mock 拦截查询工厂,覆盖过滤/分页/统计/批量更新/分析。
 */
class OrderRepositoryImplTest {
  private lateinit var selectQuery: JPAQuery<OrderModel>
  private lateinit var countQuery: JPAQuery<Void>
  private lateinit var exprQuery: JPAQuery<Long>
  private lateinit var analyticsQuery: JPAQuery<Tuple>
  private lateinit var updateClause: JPAUpdateClause
  private lateinit var repo: OrderRepositoryImpl

  private val start = LocalDateTime.of(2025, 1, 1, 0, 0)
  private val end = LocalDateTime.of(2025, 1, 2, 0, 0)

  private fun order(
    id: Long,
    status: OrderModelStatus = OrderModelStatus.PENDING_DISPATCH,
    amountCents: Long = 1000,
  ) = OrderModel(id = id, orderNo = "ORD-$id", userId = 100L, amountCents = amountCents, status = status)

  @BeforeEach
  fun setUp() {
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

  @AfterEach
  fun tearDown() {
    unmockkConstructor(JPAQueryFactory::class)
  }

  // ================================================================
  // findOrdersWithFilters
  // ================================================================

  @Test
  @DisplayName("findOrdersWithFilters 全过滤条件")
  fun `findOrdersWithFilters applies all filters`() {
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

    assertEquals(orders, result)
  }

  @Test
  @DisplayName("findOrdersWithFilters 无过滤条件返回全部")
  fun `findOrdersWithFilters handles null filters`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders

    assertEquals(orders, repo.findOrdersWithFilters(null, null, null, null, null, null, null))
  }

  // ================================================================
  // findDeliveryWorkerOrdersWithFilters
  // ================================================================

  @Test
  @DisplayName("findDeliveryWorkerOrdersWithFilters 基础条件")
  fun `findDeliveryWorkerOrdersWithFilters fetches base`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders

    assertEquals(orders, repo.findDeliveryWorkerOrdersWithFilters(5L, OrderModelStatus.DELIVERING, null, null, null))
  }

  @Test
  @DisplayName("findDeliveryWorkerOrdersWithFilters 带时间与条数限制")
  fun `findDeliveryWorkerOrdersWithFilters applies date and limit`() {
    val orders = listOf(order(1L), order(2L))
    every { selectQuery.fetch() } returns orders

    assertEquals(orders, repo.findDeliveryWorkerOrdersWithFilters(5L, OrderModelStatus.DELIVERING, start, end, 10))
  }

  // ================================================================
  // countOrdersWithFilters
  // ================================================================

  @Test
  @DisplayName("countOrdersWithFilters 计数")
  fun `countOrdersWithFilters counts`() {
    every { countQuery.fetchCount() } returnsMany listOf(7L, 0L)

    assertEquals(
      7L,
      repo.countOrdersWithFilters(100L, OrderModelStatus.PENDING_DISPATCH, 5L, start, end, listOf(OrderModelStatus.PENDING_DISPATCH)),
    )
    assertEquals(0L, repo.countOrdersWithFilters(null, null, null, null, null, null))
  }

  // ================================================================
  // findOrdersPaginated
  // ================================================================

  @Test
  @DisplayName("findOrdersPaginated 默认按创建时间倒序")
  fun `findOrdersPaginated default sort`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    // total 需 ≥ offset+pageSize,否则 Spring Data 4 PageImpl 会按 offset+content.size 重算
    every { countQuery.fetchCount() } returns 15L

    val page = repo.findOrdersPaginated("kw", OrderModelStatus.COMPLETED, 100L, 5L, start, end, 100L, 10000L, 0, 10, "createdAt", "desc")

    assertEquals(orders, page.content)
    assertEquals(15L, page.totalElements)
    assertEquals(10, page.size)
  }

  @Test
  @DisplayName("findOrdersPaginated 按金额升序")
  fun `findOrdersPaginated sorts by total amount asc`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    every { countQuery.fetchCount() } returns 1L

    val page = repo.findOrdersPaginated(null, null, null, null, null, null, null, null, 1, 5, "totalAmount", "asc")

    assertEquals(orders, page.content)
    assertTrue(page.sort.isSorted)
  }

  @Test
  @DisplayName("findOrdersPaginated 按更新时间排序")
  fun `findOrdersPaginated sorts by updated at`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    every { countQuery.fetchCount() } returns 1L

    val page = repo.findOrdersPaginated(null, null, null, null, null, null, null, null, 0, 20, "updated_at", "asc")

    assertEquals(orders, page.content)
    assertEquals(1L, page.totalElements)
  }

  // ================================================================
  // findByDeliveryWorkerIdAndStatusIn
  // ================================================================

  @Test
  @DisplayName("findByDeliveryWorkerIdAndStatusIn 带配送员过滤")
  fun `findByDeliveryWorkerIdAndStatusIn with worker`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    every { countQuery.fetchCount() } returns 1L

    val page = repo.findByDeliveryWorkerIdAndStatusIn(5L, listOf(OrderModelStatus.COMPLETED), "kw", 0, 10)

    assertEquals(orders, page.content)
    assertEquals(1L, page.totalElements)
  }

  @Test
  @DisplayName("findByDeliveryWorkerIdAndStatusIn 配送员为空")
  fun `findByDeliveryWorkerIdAndStatusIn without worker`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    every { countQuery.fetchCount() } returns 1L

    val page = repo.findByDeliveryWorkerIdAndStatusIn(null, listOf(OrderModelStatus.CANCELLED), null, 0, 10)

    assertEquals(orders, page.content)
  }

  // ================================================================
  // findUserOrdersPaginated
  // ================================================================

  @Test
  @DisplayName("findUserOrdersPaginated 带状态过滤")
  fun `findUserOrdersPaginated with statuses`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    every { countQuery.fetchCount() } returns 1L

    val page = repo.findUserOrdersPaginated(100L, listOf(OrderModelStatus.COMPLETED), 0, 10)

    assertEquals(orders, page.content)
  }

  @Test
  @DisplayName("findUserOrdersPaginated 状态为空")
  fun `findUserOrdersPaginated with null statuses`() {
    val orders = listOf(order(1L))
    every { selectQuery.fetch() } returns orders
    every { countQuery.fetchCount() } returns 1L

    val page = repo.findUserOrdersPaginated(100L, null, 0, 10)
    val page2 = repo.findUserOrdersPaginated(100L, emptyList(), 0, 10)

    assertEquals(orders, page.content)
    assertEquals(orders, page2.content)
  }

  // ================================================================
  // bulkUpdateOrderStatus
  // ================================================================

  @Test
  @DisplayName("bulkUpdateOrderStatus 批量更新状态")
  fun `bulkUpdateOrderStatus updates status`() {
    every { updateClause.execute() } returns 3L

    assertEquals(3, repo.bulkUpdateOrderStatus(listOf(1L, 2L, 3L), OrderModelStatus.CANCELLED, null))
  }

  @Test
  @DisplayName("bulkUpdateOrderStatus 附带配送员更新")
  fun `bulkUpdateOrderStatus with worker`() {
    every { updateClause.execute() } returns 2L

    assertEquals(2, repo.bulkUpdateOrderStatus(listOf(1L, 2L), OrderModelStatus.DELIVERING, 5L))
  }

  // ================================================================
  // getOrderAnalytics
  // ================================================================

  @Test
  @DisplayName("getOrderAnalytics 聚合日维度指标")
  fun `getOrderAnalytics aggregates rows`() {
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

    assertEquals(1, rows.size)
    assertEquals(LocalDate.of(2025, 1, 1), rows[0].orderDate)
    assertEquals(OrderModelStatus.COMPLETED, rows[0].status)
    assertEquals(10L, rows[0].orderCount)
    assertEquals(10L, rows[0].totalRevenueCents)
    assertEquals(10L, rows[0].uniqueCustomers)
    assertEquals(10L, rows[0].activeWorkers)
  }

  // ================================================================
  // countOrdersByDateRange / sumAmountCentsByStatusAndDateRange
  // ================================================================

  @Test
  @DisplayName("countOrdersByDateRange 支持配送员与状态过滤,空结果兜底")
  fun `countOrdersByDateRange counts with filters`() {
    every { exprQuery.fetchOne() } returnsMany listOf(4L, 4L, null)
    assertEquals(4L, repo.countOrdersByDateRange(start, end, 5L, listOf(OrderModelStatus.COMPLETED)))
    assertEquals(4L, repo.countOrdersByDateRange(start, end, null, null))

    assertEquals(0L, repo.countOrdersByDateRange(start, end, 5L, null))
  }

  @Test
  @DisplayName("sumAmountCentsByStatusAndDateRange 汇总金额,空结果兜底")
  fun `sumAmountCentsByStatusAndDateRange sums`() {
    every { exprQuery.fetchOne() } returnsMany listOf(9000L, 9000L, null)
    assertEquals(9000L, repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 5L))
    assertEquals(9000L, repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, null))

    assertEquals(0L, repo.sumAmountCentsByStatusAndDateRange(listOf(OrderModelStatus.COMPLETED), start, end, 5L))
  }

  // ================================================================
  // countAndSumByPaymentType
  // ================================================================

  @Test
  @DisplayName("countAndSumByPaymentType 返回计数与金额")
  fun `countAndSumByPaymentType returns count and sum`() {
    every { exprQuery.fetchOne() } returnsMany listOf(2L, 2500L)

    val result = repo.countAndSumByPaymentType(5L, OrderModelStatus.COMPLETED, PaymentType.QR_CODE, start, end)

    assertEquals(2L, result[0])
    assertEquals(2500L, result[1])
  }

  @Test
  @DisplayName("countAndSumByPaymentType 空结果兜底 0")
  fun `countAndSumByPaymentType handles null fetch`() {
    every { exprQuery.fetchOne() } returnsMany listOf(null, null)

    val result = repo.countAndSumByPaymentType(5L, OrderModelStatus.COMPLETED, PaymentType.CASH, start, end)

    assertEquals(0L, result[0])
    assertEquals(0L, result[1])
  }
}
