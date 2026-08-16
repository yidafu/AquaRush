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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.time.LocalDateTime

/**
 * 支付查询规格工厂单元测试:每种规格构建正确的谓词。
 */
class PaymentSpecificationsTest :
  FunSpec({
    var root: Root<PaymentModel> = mockk()
    var query: CriteriaQuery<PaymentModel> = mockk()
    var cb: CriteriaBuilder = mockk()

    beforeTest {
      root = mockk(relaxed = true)
      query = mockk(relaxed = true)
      cb = mockk(relaxed = true)
    }
    test("byOrderId builds equal predicate") {
      val orderIdPath = mockk<Path<Long>>()
      val predicate = mockk<Predicate>()
      every { root.get<Long>("orderId") } returns orderIdPath
      every { cb.equal(orderIdPath, 1L) } returns predicate

      val spec = PaymentSpecifications.byOrderId(1L)

      predicate.shouldBeSameInstanceAs(spec.toPredicate(root, query, cb))
      verify { cb.equal(orderIdPath, 1L) }
    }
    test("byTransactionId builds equal predicate") {
      val spec = PaymentSpecifications.byTransactionId("WX-1")

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
      verify { root.get<String>("transactionId") }
      verify { cb.equal(any(), eq("WX-1")) }
    }
    test("byPrepayId builds equal predicate") {
      val spec = PaymentSpecifications.byPrepayId("prepay-1")

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
      verify { root.get<String>("prepayId") }
      verify { cb.equal(any(), eq("prepay-1")) }
    }
    test("byUserIdAndStatus builds and predicate") {
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

      andPred.shouldBeSameInstanceAs(spec.toPredicate(root, query, cb))
    }
    test("byStatus builds equal predicate") {
      val spec = PaymentSpecifications.byStatus(PaymentStatus.SUCCESS)

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
      verify { root.get<Enum<*>>("status") }
      verify { cb.equal(any(), eq(PaymentStatus.SUCCESS)) }
    }
    test("byStatuses builds in predicate") {
      val spec = PaymentSpecifications.byStatuses(listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING))

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
    }
    test("expiredBefore builds pending and expired predicate") {
      val now = LocalDateTime.of(2025, 1, 1, 12, 0)
      val spec = PaymentSpecifications.expiredBefore(now)

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
      verify { cb.equal(any(), eq(PaymentStatus.PENDING)) }
      verify { cb.lessThan(any(), eq(now)) }
    }
    test("createdAtBetween builds range predicate") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      val spec = PaymentSpecifications.createdAtBetween(start, end)

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
      verify { cb.greaterThanOrEqualTo(any(), eq(start)) }
      verify { cb.lessThanOrEqualTo(any(), eq(end)) }
    }
    test("byStatusAndCreatedAtBetween builds combined predicate") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      val spec = PaymentSpecifications.byStatusAndCreatedAtBetween(PaymentStatus.SUCCESS, start, end)

      val predicate = spec.toPredicate(root, query, cb)

      predicate.shouldNotBeNull()
      verify { cb.and(any(), any(), any()) }
    }
  })
