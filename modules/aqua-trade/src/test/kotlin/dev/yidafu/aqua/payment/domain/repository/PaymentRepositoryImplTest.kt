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

package dev.yidafu.aqua.payment.domain.repository

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 支付仓储 QueryDSL 实现单元测试:构造器 mock 拦截查询工厂,覆盖各类过滤查询与汇总。
 */
class PaymentRepositoryImplTest {
  private lateinit var selectQuery: JPAQuery<PaymentModel>
  private lateinit var countQuery: JPAQuery<Void>
  private lateinit var exprQuery: JPAQuery<Long>
  private lateinit var repo: PaymentRepositoryImpl

  private val now = LocalDateTime.of(2025, 1, 1, 12, 0)

  private fun payment(
    id: Long,
    userId: Long = 100L,
    status: PaymentStatus = PaymentStatus.SUCCESS,
    amount: Long = 1200,
  ) = PaymentModel(id = id, orderId = id, userId = userId, amount = amount, status = status)

  @BeforeEach
  fun setUp() {
    mockkConstructor(JPAQueryFactory::class)
    selectQuery = mockk(relaxed = true)
    countQuery = mockk(relaxed = true)
    exprQuery = mockk(relaxed = true)
    repo = PaymentRepositoryImpl()
    val em = mockk<EntityManager>(relaxed = true)
    val field = PaymentRepositoryImpl::class.java.getDeclaredField("entityManager")
    field.isAccessible = true
    field.set(repo, em)

    every { anyConstructed<JPAQueryFactory>().selectFrom(any<EntityPath<PaymentModel>>()) } returns selectQuery
    every { anyConstructed<JPAQueryFactory>().query() } returns countQuery
    every { anyConstructed<JPAQueryFactory>().select(any<Expression<Long>>()) } returns exprQuery
    every { selectQuery.where(any<Predicate>()) } returns selectQuery
    every { selectQuery.orderBy(any<OrderSpecifier<*>>()) } returns selectQuery
    every { countQuery.from(any()) } returns countQuery
    every { countQuery.where(any<Predicate>()) } returns countQuery
    every { countQuery.select(any<Expression<Long>>()) } returns exprQuery
  }

  @AfterEach
  fun tearDown() {
    unmockkConstructor(JPAQueryFactory::class)
  }

  @Test
  @DisplayName("findByUserIdAndStatusEnhanced 返回匹配支付记录")
  fun `findByUserIdAndStatusEnhanced fetches matches`() {
    val payments = listOf(payment(1L))
    every { selectQuery.fetch() } returns payments

    assertEquals(payments, repo.findByUserIdAndStatusEnhanced(100L, PaymentStatus.SUCCESS))
  }

  @Test
  @DisplayName("findExpiredPaymentsEnhanced 返回过期待支付记录")
  fun `findExpiredPaymentsEnhanced fetches expired`() {
    val payments = listOf(payment(1L, status = PaymentStatus.PENDING))
    every { selectQuery.fetch() } returns payments

    assertEquals(payments, repo.findExpiredPaymentsEnhanced(now))
  }

  @Test
  @DisplayName("findByCreatedAtBetweenEnhanced 返回区间记录")
  fun `findByCreatedAtBetweenEnhanced fetches by range`() {
    val payments = listOf(payment(1L))
    every { selectQuery.fetch() } returns payments

    assertEquals(payments, repo.findByCreatedAtBetweenEnhanced(now.minusDays(1), now))
  }

  @Test
  @DisplayName("countByStatusAndCreatedAtBetweenEnhanced 返回计数")
  fun `countByStatusAndCreatedAtBetweenEnhanced counts`() {
    every { countQuery.fetchCount() } returns 3L

    assertEquals(3L, repo.countByStatusAndCreatedAtBetweenEnhanced(PaymentStatus.SUCCESS, now.minusDays(1), now))
  }

  @Test
  @DisplayName("sumAmountByStatusAndCreatedAtBetweenEnhanced 返回金额,空结果兜底 0")
  fun `sumAmountByStatusAndCreatedAtBetweenEnhanced sums`() {
    every { exprQuery.fetchOne() } returns 5000L
    assertEquals(5000L, repo.sumAmountByStatusAndCreatedAtBetweenEnhanced(PaymentStatus.SUCCESS, now.minusDays(1), now))

    every { exprQuery.fetchOne() } returns null
    assertEquals(0L, repo.sumAmountByStatusAndCreatedAtBetweenEnhanced(PaymentStatus.SUCCESS, now.minusDays(1), now))
  }

  @Test
  @DisplayName("findPaymentsWithFilters 全过滤条件")
  fun `findPaymentsWithFilters applies all filters`() {
    val payments = listOf(payment(1L))
    every { selectQuery.fetch() } returns payments

    val result =
      repo.findPaymentsWithFilters(
        userId = 100L,
        status = PaymentStatus.SUCCESS,
        transactionId = "WX-1",
        startDate = now.minusDays(1),
        endDate = now,
        minAmount = 100L,
        maxAmount = 5000L,
      )

    assertEquals(payments, result)
  }

  @Test
  @DisplayName("findPaymentsWithFilters 无过滤条件返回全部")
  fun `findPaymentsWithFilters handles null filters`() {
    val payments = listOf(payment(1L), payment(2L))
    every { selectQuery.fetch() } returns payments

    val result = repo.findPaymentsWithFilters(null, null, null, null, null, null, null)

    assertEquals(payments, result)
  }

  @Test
  @DisplayName("findPaymentsWithFilters 仅起点无终点时跳过时间过滤")
  fun `findPaymentsWithFilters skips partial date range`() {
    val payments = listOf(payment(1L))
    every { selectQuery.fetch() } returns payments

    val result = repo.findPaymentsWithFilters(null, null, null, now.minusDays(1), null, null, null)

    assertEquals(payments, result)
  }
}
