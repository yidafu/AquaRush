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

import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.table.PaymentRefundsTable
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 退款仓储 Exposed 实现级测试(**Testcontainers 真实 PostgreSQL**):种子直插 [PaymentRefundsTable] 后断言查询结果。
 * 覆盖:save 双分支、软删可见性、findRefundRequests 过滤/排序/分页、findRefundedByDateRange
 * COMPLETED 闭区间窗口、sum 聚合(边界/口径/空窗口=0)、按日分组。金额断言一律 Long 分。
 */
class PaymentRefundRepositoryImplTest :
  FunSpec({
    val db = TradePgTestSupport.database
    val repo = PaymentRefundRepositoryImpl(db)

    fun seedRefund(
      id: Long = TradePgTestSupport.id(),
      paymentId: Long = 1L,
      orderId: Long = 10L,
      userId: Long = 100L,
      status: RefundStatus = RefundStatus.PENDING,
      refundAmount: Long = 1000L,
      refundType: RefundType = RefundType.ORDER_REFUND,
      processedAt: LocalDateTime? = null,
      createdAt: LocalDateTime = LocalDateTime.of(2025, 6, 1, 10, 0),
      isDeleted: Boolean = false,
    ): Long {
      transaction(db) {
        PaymentRefundsTable.insert {
          it[PaymentRefundsTable.id] = id
          it[PaymentRefundsTable.paymentId] = paymentId
          it[PaymentRefundsTable.orderId] = orderId
          it[PaymentRefundsTable.userId] = userId
          it[PaymentRefundsTable.refundId] = "RF-$id"
          it[PaymentRefundsTable.refundNo] = "OUT-$id"
          it[PaymentRefundsTable.refundAmountCents] = refundAmount
          it[PaymentRefundsTable.reason] = "用户申请退款"
          it[PaymentRefundsTable.status] = status.name
          it[PaymentRefundsTable.refundType] = refundType.name
          it[PaymentRefundsTable.refundAccount] = "wxpay"
          it[PaymentRefundsTable.processedAt] = processedAt
          it[PaymentRefundsTable.createdAt] = createdAt
          it[PaymentRefundsTable.updatedAt] = createdAt
          if (isDeleted) {
            it[PaymentRefundsTable.isDeleted] = true
            it[PaymentRefundsTable.deletedAt] = LocalDateTime.of(2025, 6, 1, 11, 0)
          }
        }
      }
      return id
    }

    beforeTest {
      TradePgTestSupport.reset()
    }

    test("save 新增:雪花 id 回填 + findById 回读全字段映射") {
      val entity =
        PaymentRefundModel(
          paymentId = 42L,
          orderId = 77L,
          userId = 100L,
          refundId = "RF-NEW-1",
          outRefundNo = "OUT-NEW-1",
          refundAmount = 6600L,
          refundReason = "送水延迟",
          status = RefundStatus.PENDING,
          refundType = RefundType.ORDER_REFUND,
        )
      entity.id shouldBe null

      val saved = repo.save(entity)
      saved.id.shouldNotBeNull()

      val loaded = repo.findById(saved.id!!).get()
      loaded.paymentId shouldBe 42L
      loaded.orderId shouldBe 77L
      loaded.refundId shouldBe "RF-NEW-1"
      loaded.outRefundNo shouldBe "OUT-NEW-1"
      loaded.refundAmount shouldBe 6600L
      loaded.refundReason shouldBe "送水延迟"
      loaded.status shouldBe RefundStatus.PENDING
      loaded.refundType shouldBe RefundType.ORDER_REFUND
      loaded.refundedAt shouldBe null
    }

    test("save 更新:按 id upsert 更新业务列(状态/金额/退款成功时间)") {
      val seedCreatedAt = LocalDateTime.of(2025, 6, 1, 9, 0)
      val id = seedRefund(id = 920001L, paymentId = 7L, refundAmount = 1000L, createdAt = seedCreatedAt)
      val entity =
        PaymentRefundModel(
          id = id,
          paymentId = 7L,
          orderId = 10L,
          userId = 100L,
          refundId = "RF-$id",
          outRefundNo = "OUT-$id",
          refundAmount = 4500L,
          refundReason = "用户申请退款",
          status = RefundStatus.COMPLETED,
          refundType = RefundType.ORDER_REFUND,
          refundedAt = LocalDateTime.of(2025, 6, 2, 15, 30),
          createdAt = seedCreatedAt,
        )

      repo.save(entity)

      val loaded = repo.findById(id).get()
      loaded.refundAmount shouldBe 4500L
      loaded.status shouldBe RefundStatus.COMPLETED
      loaded.refundedAt shouldBe LocalDateTime.of(2025, 6, 2, 15, 30)
      loaded.createdAt shouldBe seedCreatedAt
      loaded.updatedAt.isAfter(seedCreatedAt) shouldBe true
    }

    test("findById:不存在与已软删均返回 empty") {
      val active = seedRefund(id = 920010L)
      val deleted = seedRefund(id = 920011L, isDeleted = true)

      repo.findById(active).isPresent shouldBe true
      repo.findById(deleted).isPresent shouldBe false
      repo.findById(999999999L).isPresent shouldBe false
    }

    test("findRefundRequests:无过滤全量 + createdAt 倒序 + 软删不可见") {
      val t1 = LocalDateTime.of(2025, 6, 1, 9, 0)
      val t2 = LocalDateTime.of(2025, 6, 2, 9, 0)
      val t3 = LocalDateTime.of(2025, 6, 3, 9, 0)
      val r1 = seedRefund(id = 920020L, createdAt = t1)
      val r2 = seedRefund(id = 920021L, createdAt = t2)
      val r3 = seedRefund(id = 920022L, createdAt = t3)
      seedRefund(id = 920023L, createdAt = t3.plusDays(1), isDeleted = true)

      val page = repo.findRefundRequests(null, null, null, 0, 10)

      page.content.map { it.id } shouldBe listOf(r3, r2, r1)
      page.totalElements shouldBe 3L
    }

    test("findRefundRequests:status + 创建时间闭区间过滤 + 分页 total") {
      val r1 = seedRefund(id = 920030L, status = RefundStatus.COMPLETED, createdAt = LocalDateTime.of(2025, 6, 1, 9, 0))
      val r2 = seedRefund(id = 920031L, status = RefundStatus.PENDING, createdAt = LocalDateTime.of(2025, 6, 2, 9, 0))
      val r3 = seedRefund(id = 920032L, status = RefundStatus.COMPLETED, createdAt = LocalDateTime.of(2025, 6, 3, 9, 0))
      val r4 = seedRefund(id = 920033L, status = RefundStatus.COMPLETED, createdAt = LocalDateTime.of(2025, 6, 4, 9, 0))

      // status 过滤:COMPLETED 按 createdAt 倒序分页(页大小 2)
      val completedPage0 = repo.findRefundRequests(RefundStatus.COMPLETED, null, null, 0, 2)
      completedPage0.content.map { it.id } shouldBe listOf(r4, r3)
      completedPage0.totalElements shouldBe 3L
      completedPage0.hasNext() shouldBe true
      val completedPage1 = repo.findRefundRequests(RefundStatus.COMPLETED, null, null, 1, 2)
      completedPage1.content.map { it.id } shouldBe listOf(r1)
      completedPage1.hasNext() shouldBe false

      // 创建时间闭区间(6/2 00:00 ~ 6/3 09:00,恰等于 r3.createdAt 含边界)+ 不限状态
      val windowed =
        repo.findRefundRequests(
          null,
          LocalDateTime.of(2025, 6, 2, 0, 0),
          LocalDateTime.of(2025, 6, 3, 9, 0),
          0,
          10,
        )
      windowed.content.map { it.id } shouldBe listOf(r3, r2)
      windowed.totalElements shouldBe 2L
    }

    test("findRefundedByDateRange:COMPLETED 闭区间窗口 + processedAt 倒序 + 分页 + 口径过滤") {
      val start = LocalDateTime.of(2025, 6, 1, 0, 0)
      val end = LocalDateTime.of(2025, 6, 2, 0, 0)
      val w1 = seedRefund(id = 920040L, status = RefundStatus.COMPLETED, refundAmount = 1000L, processedAt = start) // 恰等于 start → 计入
      val w2 = seedRefund(id = 920041L, status = RefundStatus.COMPLETED, refundAmount = 2000L, processedAt = end) // 恰等于 end → 计入
      val w3 =
        seedRefund(
          id = 920042L,
          status = RefundStatus.COMPLETED,
          refundAmount = 3000L,
          processedAt = LocalDateTime.of(2025, 6, 1, 12, 0),
        )
      seedRefund(id = 920043L, status = RefundStatus.COMPLETED, processedAt = start.minusSeconds(1)) // 窗口前 → 排除
      seedRefund(id = 920044L, status = RefundStatus.PENDING, processedAt = start.plusHours(1)) // 非 COMPLETED → 排除
      seedRefund(id = 920045L, status = RefundStatus.COMPLETED, processedAt = start.plusHours(2), isDeleted = true) // 软删 → 排除

      val all = repo.findRefundedByDateRange(start, end, 0, 10)
      // processedAt 倒序:6/2 00:00 > 6/1 12:00 > 6/1 00:00
      all.content.map { it.id } shouldBe listOf(w2, w3, w1)
      all.content.map { it.refundAmount } shouldBe listOf(2000L, 3000L, 1000L)
      all.totalElements shouldBe 3L

      val page0 = repo.findRefundedByDateRange(start, end, 0, 2)
      page0.content.map { it.id } shouldBe listOf(w2, w3)
      page0.hasNext() shouldBe true
      val page1 = repo.findRefundedByDateRange(start, end, 1, 2)
      page1.content.map { it.id } shouldBe listOf(w1)
      page1.hasNext() shouldBe false
    }

    test("sumRefundedAmountByProcessedAtBetween:口径聚合 + 闭区间边界,非 COMPLETED/软删/窗口外不计") {
      val start = LocalDateTime.of(2025, 6, 1, 0, 0)
      val end = LocalDateTime.of(2025, 6, 2, 12, 0)
      seedRefund(id = 920050L, status = RefundStatus.COMPLETED, refundAmount = 1000L, processedAt = start) // 边界含
      seedRefund(id = 920051L, status = RefundStatus.COMPLETED, refundAmount = 2500L, processedAt = LocalDateTime.of(2025, 6, 1, 8, 0))
      seedRefund(id = 920052L, status = RefundStatus.COMPLETED, refundAmount = 500L, processedAt = end) // 边界含
      seedRefund(id = 920053L, status = RefundStatus.FAILED, refundAmount = 999L, processedAt = start.plusHours(1)) // 非 COMPLETED
      seedRefund(id = 920054L, status = RefundStatus.COMPLETED, refundAmount = 888L, processedAt = start.plusHours(2), isDeleted = true)
      seedRefund(id = 920055L, status = RefundStatus.COMPLETED, refundAmount = 1111L, processedAt = end.plusSeconds(1)) // 窗口外

      repo.sumRefundedAmountByProcessedAtBetween(start, end) shouldBe 4000L
    }

    test("sumRefundedAmountByProcessedAtBetween:无匹配窗口返回 0") {
      repo.sumRefundedAmountByProcessedAtBetween(
        LocalDateTime.of(2024, 1, 1, 0, 0),
        LocalDateTime.of(2024, 1, 2, 0, 0),
      ) shouldBe 0L
    }

    test("sumRefundedAmountGroupByDay:按退款成功时间逐日分组,口径同总和") {
      val start = LocalDateTime.of(2025, 6, 1, 0, 0)
      val end = LocalDateTime.of(2025, 6, 2, 23, 59, 59)
      seedRefund(id = 920060L, status = RefundStatus.COMPLETED, refundAmount = 1000L, processedAt = LocalDateTime.of(2025, 6, 1, 9, 0))
      seedRefund(id = 920061L, status = RefundStatus.COMPLETED, refundAmount = 2500L, processedAt = LocalDateTime.of(2025, 6, 1, 18, 0))
      seedRefund(id = 920062L, status = RefundStatus.COMPLETED, refundAmount = 500L, processedAt = LocalDateTime.of(2025, 6, 2, 10, 0))
      seedRefund(id = 920063L, status = RefundStatus.FAILED, refundAmount = 999L, processedAt = LocalDateTime.of(2025, 6, 1, 12, 0))

      val result = repo.sumRefundedAmountGroupByDay(start, end)

      result shouldBe
        mapOf(
          LocalDate.of(2025, 6, 1) to 3500L,
          LocalDate.of(2025, 6, 2) to 500L,
        )
    }
  })
