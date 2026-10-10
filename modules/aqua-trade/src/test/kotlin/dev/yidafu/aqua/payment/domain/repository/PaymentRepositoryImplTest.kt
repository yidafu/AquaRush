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

import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.table.PaymentsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

/**
 * 支付仓储 Exposed 实现级测试(**Testcontainers 真实 PostgreSQL**):种子直插 [PaymentsTable] 后断言查询结果。
 * 覆盖:save 双分支与 version 递增、软删可见性、创建时间闭区间边界、过期扫描分页(严格小于 + hasNext)、
 * claimExpired 条件 UPDATE 的原子/幂等/拒绝分支。金额断言一律 Long 分。
 */
class PaymentRepositoryImplTest :
  FunSpec({
    val db = TradePgTestSupport.database
    val repo = PaymentRepositoryImpl(db)
    val now = LocalDateTime.of(2025, 6, 1, 12, 0)

    fun seedPayment(
      id: Long = TradePgTestSupport.id(),
      orderId: Long = id + 10000,
      userId: Long = 100L,
      status: PaymentStatus = PaymentStatus.PENDING,
      amount: Long = 1200L,
      transactionId: String? = null,
      prepayId: String? = null,
      createdAt: LocalDateTime = LocalDateTime.of(2025, 6, 1, 10, 0),
      expiredAt: LocalDateTime? = null,
      paidAt: LocalDateTime? = null,
      isDeleted: Boolean = false,
    ): Long {
      transaction(db) {
        PaymentsTable.insert {
          it[PaymentsTable.id] = id
          it[PaymentsTable.orderId] = orderId
          it[PaymentsTable.userId] = userId
          it[PaymentsTable.transactionId] = transactionId
          it[PaymentsTable.prepayId] = prepayId
          it[PaymentsTable.amountCents] = amount
          it[PaymentsTable.currency] = "CNY"
          it[PaymentsTable.status] = status.name
          it[PaymentsTable.paymentMethod] = PaymentMethod.WECHAT_PAY.name
          it[PaymentsTable.description] = "桶装水订单支付"
          it[PaymentsTable.paidAt] = paidAt
          it[PaymentsTable.expiredAt] = expiredAt
          it[PaymentsTable.version] = 0L
          it[PaymentsTable.createdAt] = createdAt
          it[PaymentsTable.updatedAt] = createdAt
          if (isDeleted) {
            it[PaymentsTable.isDeleted] = true
            it[PaymentsTable.deletedAt] = LocalDateTime.of(2025, 6, 1, 11, 0)
          }
        }
      }
      return id
    }

    beforeTest {
      TradePgTestSupport.reset()
    }

    test("save 新增:雪花 id 回填 + findById 回读全字段") {
      val entity =
        PaymentModel(
          orderId = 88L,
          userId = 100L,
          transactionId = null,
          amount = 3300L,
          status = PaymentStatus.PENDING,
          paymentMethod = PaymentMethod.WECHAT_PAY,
          expiredAt = now.plusMinutes(30),
        )
      entity.id shouldBe null

      val saved = repo.save(entity)
      saved.id.shouldNotBeNull()
      val loaded = repo.findById(saved.id!!).get()
      loaded.orderId shouldBe 88L
      loaded.userId shouldBe 100L
      loaded.amount shouldBe 3300L
      loaded.currency shouldBe "CNY"
      loaded.status shouldBe PaymentStatus.PENDING
      loaded.paymentMethod shouldBe PaymentMethod.WECHAT_PAY
      loaded.expiredAt shouldBe now.plusMinutes(30)
      loaded.version shouldBe 0L
      loaded.deletedAt shouldBe null
    }

    test("save 更新:按 id upsert 更新业务列,version 数据库侧 +1,createdAt 不变") {
      val seedCreatedAt = LocalDateTime.of(2025, 6, 1, 9, 0)
      val id = seedPayment(id = 910001L, orderId = 7L, amount = 1000L, status = PaymentStatus.PENDING, createdAt = seedCreatedAt)
      val entity =
        PaymentModel(
          id = id,
          orderId = 7L,
          userId = 100L,
          transactionId = "WX-UPDATE-1",
          amount = 9999L,
          status = PaymentStatus.SUCCESS,
          paidAt = now,
          createdAt = seedCreatedAt,
        )

      repo.save(entity)

      val loaded = repo.findById(id).get()
      loaded.amount shouldBe 9999L
      loaded.status shouldBe PaymentStatus.SUCCESS
      loaded.transactionId shouldBe "WX-UPDATE-1"
      loaded.paidAt shouldBe now
      loaded.createdAt shouldBe seedCreatedAt
      loaded.version shouldBe 1L
    }

    test("findById:不存在与已软删均返回 empty") {
      val active = seedPayment(id = 910010L)
      val deleted = seedPayment(id = 910011L, isDeleted = true)

      repo.findById(active).isPresent shouldBe true
      repo.findById(deleted).isPresent shouldBe false
      repo.findById(999999999L).isPresent shouldBe false
    }

    test("findByOrderId:命中/未命中/软删不可见") {
      val hit = seedPayment(id = 910020L, orderId = 555L)
      seedPayment(id = 910021L, orderId = 666L, isDeleted = true)

      repo.findByOrderId(555L).get().id shouldBe hit
      repo.findByOrderId(666L).isPresent shouldBe false
      repo.findByOrderId(777L).isPresent shouldBe false
    }

    test("findByTransactionId:命中/未命中/软删不可见") {
      val hit = seedPayment(id = 910030L, transactionId = "WX-TX-1")
      seedPayment(id = 910031L, transactionId = "WX-TX-DEL", isDeleted = true)

      repo.findByTransactionId("WX-TX-1").get().id shouldBe hit
      repo.findByTransactionId("WX-TX-DEL").isPresent shouldBe false
      repo.findByTransactionId("WX-TX-NA").isPresent shouldBe false
    }

    test("findByCreatedAtBetween:闭区间边界(含 start/end)且软删不可见") {
      val start = LocalDateTime.of(2025, 6, 1, 0, 0)
      val end = LocalDateTime.of(2025, 6, 2, 0, 0)
      seedPayment(id = 910040L, createdAt = start.minusSeconds(1)) // start 前 1 秒 → 排除
      val atStart = seedPayment(id = 910041L, createdAt = start) // 恰等于 start → 计入
      val mid = seedPayment(id = 910042L, createdAt = LocalDateTime.of(2025, 6, 1, 12, 0))
      val atEnd = seedPayment(id = 910043L, createdAt = end) // 恰等于 end → 计入
      seedPayment(id = 910044L, createdAt = end.plusSeconds(1)) // end 后 1 秒 → 排除
      seedPayment(id = 910045L, createdAt = LocalDateTime.of(2025, 6, 1, 6, 0), isDeleted = true)

      val result = repo.findByCreatedAtBetween(start, end)

      result.size shouldBe 3
      result.map { it.id }.toSet() shouldBe setOf(atStart, mid, atEnd)
    }

    test("findByStatusAndExpiredAtBefore:过滤 + 严格小于边界 + 分页 hasNext + id 升序 + 软删不可见") {
      val a = seedPayment(id = 910050L, status = PaymentStatus.PENDING, expiredAt = now.minusHours(3))
      val b = seedPayment(id = 910051L, status = PaymentStatus.PENDING, expiredAt = now.minusHours(2))
      val c = seedPayment(id = 910052L, status = PaymentStatus.PENDING, expiredAt = now.minusHours(1))
      seedPayment(id = 910053L, status = PaymentStatus.PENDING, expiredAt = now) // 恰等于 now → 严格小于排除
      seedPayment(id = 910054L, status = PaymentStatus.PENDING, expiredAt = now.plusHours(1)) // 未到期
      seedPayment(id = 910055L, status = PaymentStatus.SUCCESS, expiredAt = now.minusHours(1)) // 非 PENDING
      seedPayment(id = 910056L, status = PaymentStatus.PENDING, expiredAt = now.minusHours(1), isDeleted = true)

      val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id"))
      val page0 = repo.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now, pageable)
      page0.content.map { it.id } shouldBe listOf(a, b)
      page0.totalElements shouldBe 3L
      page0.hasNext() shouldBe true

      val page1 =
        repo.findByStatusAndExpiredAtBefore(
          PaymentStatus.PENDING,
          now,
          PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "id")),
        )
      page1.content.map { it.id } shouldBe listOf(c)
      page1.hasNext() shouldBe false
    }

    test("claimExpired 成功:PENDING 且已到期 → EXPIRED,version +1,updatedAt = now") {
      val id = seedPayment(id = 910060L, status = PaymentStatus.PENDING, expiredAt = now.minusMinutes(1))

      repo.claimExpired(id, now) shouldBe 1

      transaction(db) {
        val row = PaymentsTable.selectAll().andWhere { PaymentsTable.id eq id }.first()
        row[PaymentsTable.status] shouldBe PaymentStatus.EXPIRED.name
        row[PaymentsTable.version] shouldBe 1L
        row[PaymentsTable.updatedAt] shouldBe now
      }
    }

    test("claimExpired 幂等:重复认领返回 0") {
      val id = seedPayment(id = 910070L, status = PaymentStatus.PENDING, expiredAt = now.minusMinutes(1))

      repo.claimExpired(id, now) shouldBe 1
      repo.claimExpired(id, now) shouldBe 0
    }

    test("claimExpired 拒绝:未到期/非 PENDING/已软删/不存在 均返回 0") {
      val notDue = seedPayment(id = 910080L, status = PaymentStatus.PENDING, expiredAt = now.plusMinutes(1))
      val nullExpiredAt = seedPayment(id = 910081L, status = PaymentStatus.PENDING, expiredAt = null)
      val notPending = seedPayment(id = 910082L, status = PaymentStatus.SUCCESS, expiredAt = now.minusMinutes(1))
      val deleted = seedPayment(id = 910083L, status = PaymentStatus.PENDING, expiredAt = now.minusMinutes(1), isDeleted = true)

      repo.claimExpired(notDue, now) shouldBe 0
      repo.claimExpired(nullExpiredAt, now) shouldBe 0
      repo.claimExpired(notPending, now) shouldBe 0
      repo.claimExpired(deleted, now) shouldBe 0
      repo.claimExpired(999999999L, now) shouldBe 0
    }
  })
