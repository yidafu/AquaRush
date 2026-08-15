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

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * 支付查询规格工厂单元测试:每种规格构建正确的谓词。
 */
class PaymentSpecificationsTest {
  private lateinit var root: Root<PaymentModel>
  private lateinit var query: CriteriaQuery<PaymentModel>
  private lateinit var cb: CriteriaBuilder

  @BeforeEach
  fun setUp() {
    root = mockk(relaxed = true)
    query = mockk(relaxed = true)
    cb = mockk(relaxed = true)
  }

  @Test
  @DisplayName("byOrderId 构建订单号相等谓词")
  fun `byOrderId builds equal predicate`() {
    val orderIdPath = mockk<Path<Long>>()
    val predicate = mockk<Predicate>()
    every { root.get<Long>("orderId") } returns orderIdPath
    every { cb.equal(orderIdPath, 1L) } returns predicate

    val spec = PaymentSpecifications.byOrderId(1L)

    assertSame(predicate, spec.toPredicate(root, query, cb))
    verify { cb.equal(orderIdPath, 1L) }
  }

  @Test
  @DisplayName("byTransactionId 构建交易号相等谓词")
  fun `byTransactionId builds equal predicate`() {
    val spec = PaymentSpecifications.byTransactionId("WX-1")

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
    verify { root.get<String>("transactionId") }
    verify { cb.equal(any(), eq("WX-1")) }
  }

  @Test
  @DisplayName("byPrepayId 构建预支付号相等谓词")
  fun `byPrepayId builds equal predicate`() {
    val spec = PaymentSpecifications.byPrepayId("prepay-1")

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
    verify { root.get<String>("prepayId") }
    verify { cb.equal(any(), eq("prepay-1")) }
  }

  @Test
  @DisplayName("byUserIdAndStatus 构建用户与状态 AND 谓词")
  fun `byUserIdAndStatus builds and predicate`() {
    val userIdPath = mockk<Path<Long>>()
    val statusPath = mockk<Path<Enum<*>>>()
    val userIdPred = mockk<Predicate>()
    val statusPred = mockk<Predicate>()
    val andPred = mockk<Predicate>()
    every { root.get<Long>("userId") } returns userIdPath
    every { root.get<Enum<*>>("status") } returns statusPath
    every { cb.equal(userIdPath, 100L) } returns userIdPred
    every { cb.equal(statusPath, PaymentStatus.PENDING) } returns statusPred
    every { cb.and(userIdPred, statusPred) } returns andPred

    val spec = PaymentSpecifications.byUserIdAndStatus(100L, PaymentStatus.PENDING)

    assertSame(andPred, spec.toPredicate(root, query, cb))
  }

  @Test
  @DisplayName("byStatus 构建状态相等谓词")
  fun `byStatus builds equal predicate`() {
    val spec = PaymentSpecifications.byStatus(PaymentStatus.SUCCESS)

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
    verify { root.get<Enum<*>>("status") }
    verify { cb.equal(any(), eq(PaymentStatus.SUCCESS)) }
  }

  @Test
  @DisplayName("byStatuses 构建状态 IN 谓词")
  fun `byStatuses builds in predicate`() {
    val spec = PaymentSpecifications.byStatuses(listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING))

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
  }

  @Test
  @DisplayName("expiredBefore 构建待支付且过期谓词")
  fun `expiredBefore builds pending and expired predicate`() {
    val now = LocalDateTime.of(2025, 1, 1, 12, 0)
    val spec = PaymentSpecifications.expiredBefore(now)

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
    verify { cb.equal(any(), eq(PaymentStatus.PENDING)) }
    verify { cb.lessThan(any(), eq(now)) }
  }

  @Test
  @DisplayName("createdAtBetween 构建创建时间区间谓词")
  fun `createdAtBetween builds range predicate`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)
    val spec = PaymentSpecifications.createdAtBetween(start, end)

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
    verify { cb.greaterThanOrEqualTo(any(), eq(start)) }
    verify { cb.lessThanOrEqualTo(any(), eq(end)) }
  }

  @Test
  @DisplayName("byStatusAndCreatedAtBetween 构建状态与时间区间谓词")
  fun `byStatusAndCreatedAtBetween builds combined predicate`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)
    val spec = PaymentSpecifications.byStatusAndCreatedAtBetween(PaymentStatus.SUCCESS, start, end)

    val predicate = spec.toPredicate(root, query, cb)

    assertNotNull(predicate)
    verify { cb.and(any(), any(), any()) }
  }
}
